package org.mypulse.model;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

import org.mypulse.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MusicScanner {

    private MusicLibrary musicLibrary;
    private final String mediaFolderPath = "Pulse/media/";

    // Contatori della scansione in corso, azzerati ad ogni scanDirectory() e usati
    // per il riepilogo finale invece di stampare una riga per ogni singolo file
    private int filesFound;
    private int tracksAdded;
    private int tracksSkippedDuplicate;
    private int tracksSkippedNoTag;
    private int tracksSkippedError;
    private int albumsCreated;

    public MusicScanner(MusicLibrary musicLibrary) {
        this.musicLibrary = musicLibrary;
    }

    public void scanDirectory(String directoryPath) {
        // Non si cancella più la cartella media ad ogni scansione: lo si faceva
        // incondizionatamente, quindi ri-scansionare (o scansionare una cartella diversa)
        // cancellava anche le copertine degli album già presenti in libreria da una
        // sessione precedente, che restavano orfane pur avendo ancora i loro mp3 intatti.
        // saveCoverImage() scrive comunque su un percorso deterministico per (artista,
        // album), quindi le copertine vengono sovrascritte correttamente se ritrovate.
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            Utils.logSeparator();
            System.out.println("[Scansione] Percorso non valido: " + directoryPath);
            return;
        }

        Utils.logSeparator();
        System.out.println("=== Scansione avviata: " + directoryPath + " ===");
        long startTime = System.currentTimeMillis();
        filesFound = 0;
        tracksAdded = 0;
        tracksSkippedDuplicate = 0;
        tracksSkippedNoTag = 0;
        tracksSkippedError = 0;
        albumsCreated = 0;

        processDirectory(directory);

        printScanSummary(directoryPath, System.currentTimeMillis() - startTime);
    }

    // Riepilogo finale con i numeri della scansione, al posto del vecchio printAllAlbums()
    // che in realtà non stampava mai nulla di utile (scorreva una mappa mai popolata)
    private void printScanSummary(String directoryPath, long elapsedMs) {
        System.out.println("=== Scansione completata: " + directoryPath + " ===");
        System.out.println("File .mp3 trovati:        " + filesFound);
        System.out.println("Brani aggiunti:            " + tracksAdded);
        System.out.println("Brani già presenti:        " + tracksSkippedDuplicate + "  (stesso percorso già in libreria)");
        System.out.println("Brani senza tag ID3:       " + tracksSkippedNoTag + "  (saltati)");
        System.out.println("Brani con errore:          " + tracksSkippedError + "  (saltati, vedi sopra)");
        System.out.println("Nuovi album creati:        " + albumsCreated);
        System.out.printf("Durata scansione:          %.1f s%n", elapsedMs / 1000.0);
        System.out.println("=".repeat(40));
    }

    private void processDirectory(File directory) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(file); // Chiamata ricorsiva
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                    filesFound++;
                    extractAndAddTrack(file);
                }
            }
        }
    }
    private void extractAndAddTrack(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            AudioHeader audioHeader = audioFile.getAudioHeader(); // Retrieve the audio header

            if (tag != null) {
                String artistAlbum = tag.getFirst(FieldKey.ALBUM_ARTIST);
                String artist = tag.getFirst(FieldKey.ARTIST);
                String albumName = tag.getFirst(FieldKey.ALBUM);
                String title = tag.getFirst(FieldKey.TITLE);
                String trackNumber = tag.getFirst(FieldKey.TRACK);
                String discNumber = tag.getFirst(FieldKey.DISC_NO);
                String year = tag.getFirst(FieldKey.YEAR);
                String genre = tag.getFirst(FieldKey.GENRE);
                String lyrics = tag.getFirst(FieldKey.LYRICS);
                String composer = tag.getFirst(FieldKey.COMPOSER);

                // Extract format and bitrate
                String format = audioHeader.getFormat(); // Get the audio format (e.g., "MPEG-1 Layer 3")
                Integer bitrate = Math.toIntExact(audioHeader.getBitRateAsNumber()); // Get the bitrate as an integer

                // Checks for null or empty values and assigns default values
                if ((artistAlbum == null || artistAlbum.trim().isEmpty()) && (artist != null && !artist.trim().isEmpty())) {
                    artistAlbum = artist; // Use artist if album artist is not present
                }
                if (artistAlbum == null || artistAlbum.trim().isEmpty()) {
                    artistAlbum = "Artista Sconosciuto"; // Default value for empty artists
                }

                if (albumName == null || albumName.trim().isEmpty()) {
                    albumName = "Album Sconosciuto"; // Default value for empty albums
                }

                if (title == null || title.trim().isEmpty()) {
                    title = "Titolo Sconosciuto"; // Default value for empty tracks
                }

                // parseLeadingInt gestisce anche i formati "3/12" (traccia/totale) molto
                // comuni nei tag TRACK/DISC: un parseInt diretto ci sarebbe andato in
                // NumberFormatException, scartando l'intero file dal catch più esterno
                Integer trackNumberInt = parseLeadingInt(trackNumber);
                Integer discNumberInt = parseLeadingInt(discNumber);
                if (discNumberInt == null) {
                    discNumberInt = 1;
                }

                int durationSeconds = (int) audioHeader.getTrackLength();

                Integer releaseYear = null;
                if (year != null && !year.isEmpty()) {
                    releaseYear = parseLeadingInt(year.split("-")[0]);
                }

                String genreValue = (genre != null && !genre.isEmpty()) ? genre : null;

                // Obtain cover image (if available)
                byte[] coverImage = null;
                String coverImagePath = null;
                if (tag.getFirstArtwork() != null) {
                    Artwork artwork = tag.getFirstArtwork();
                    coverImage = artwork.getBinaryData();

                    // Save the cover image
                    coverImagePath = saveCoverImage(artistAlbum, albumName, coverImage);
                }

                // Check if the album already exists
                Album album = musicLibrary.getAlbumByName(albumName);
                if (album == null) {
                    // Create a new album. Non stampiamo invece nulla quando l'album esiste
                    // già: è il caso normale (gli altri brani dello stesso album), non un
                    // segnale utile, e prima veniva stampato una volta per ogni brano
                    album = new Album(albumName, artistAlbum, coverImage, coverImagePath);
                    musicLibrary.addAlbum(album); // Add the album to the library
                    albumsCreated++;
                    System.out.println("[Album] Nuovo album: \"" + albumName + "\" — " + artistAlbum);
                }

                // Check if the track already exists
                Track existingTrack = musicLibrary.getTrackByFilePath(file.getAbsolutePath());
                if (existingTrack != null) {
                    // Contato ma non stampato riga per riga: su una libreria già scansionata
                    // sarebbe la stragrande maggioranza dei file, il totale basta nel riepilogo
                    tracksSkippedDuplicate++;
                    return; // Exit if the track already exists
                }

                // Create the Track object
                Track track = new Track(
                        title,
                        trackNumberInt,
                        discNumberInt,
                        file.getAbsolutePath(),
                        durationSeconds,
                        genreValue,
                        artistAlbum,
                        artist,
                        albumName,
                        releaseYear,
                        coverImage,
                        album,
                        lyrics,
                        composer
                );

                // Set the extracted format and bitrate to the track (assume you add these fields in the Track class)
                track.setFormat(format);
                track.setBitrate(bitrate);

                album.addTrack(track); // Add the track to the album
                musicLibrary.addTrack(track); // Add the track to the music library
                tracksAdded++;

                // Update album genre based on tracks
                updateAlbumGenre(album);
            } else {
                tracksSkippedNoTag++;
                System.out.println("[Attenzione] Nessun tag ID3 trovato, brano saltato: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            tracksSkippedError++;
            // Una riga di riepilogo leggibile subito sopra lo stack trace completo: gli
            // errori sono l'evento raro (a differenza dei duplicati), quindi qui il
            // dettaglio serve davvero per capire cosa non ha funzionato su quel file
            System.out.println("[Errore] " + file.getAbsolutePath() + " — " + e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            e.printStackTrace();
        }
    }

    // Estrae il numero intero iniziale da valori come "3", "3/12" o " 3 ": molti tag
    // TRACK/DISC/YEAR usano la forma "numero/totale" o hanno spazi/rumore attorno.
    // Ritorna null se non c'è alcuna cifra da estrarre, invece di lanciare un'eccezione.
    private Integer parseLeadingInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String digits = value.trim().split("[^0-9]")[0];
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Metodo per aggiornare il genere dell'album
    private void updateAlbumGenre(Album album) {
        Set<String> genres = new HashSet<>();
        for (Track track : album.getTracks()) {
            if (track.getGenre() != null) {
                genres.add(track.getGenre());
            }
        }

        if (genres.size() == 1) {
            album.setGenre(genres.iterator().next()); // Set the genre if all tracks have the same genre
        } else if (genres.size() > 1) {
            album.setGenre("Misto"); // Set "Misto" if there are multiple genres
        } else {
            album.setGenre(null); // No genre information available
        }
    }

    // Metodo per salvare l'immagine di copertina
    private String saveCoverImage(String artist, String album, byte[] imageData) throws IOException {
        String imageFolderPath = "Pulse/media/images/" + sanitizeFileName(artist) + "/" + sanitizeFileName(album);
        File imageFolder = new File(imageFolderPath);

        // Crea la cartella se non esiste
        if (!imageFolder.exists()) {
            imageFolder.mkdirs();
        }

        // Salva l'immagine nella cartella come "cover.jpg"
        String imagePath = imageFolderPath + "/cover.jpg";
        try (FileOutputStream fos = new FileOutputStream(imagePath)) {
            fos.write(imageData);
        }

        return imagePath; // Restituisce il percorso dell'immagine salvata
    }

    // Metodo per rimuovere caratteri non validi dai nomi dei file
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_"); // Sostituisci caratteri non validi con "_"
    }
}