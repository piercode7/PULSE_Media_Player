package org.mypulse.model;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MusicScanner {

    private MusicLibrary musicLibrary;
    private Map<String, Album> albumsMap; // Mappa per gli album unici
    private final String mediaFolderPath = "Pulse/media/";

    public MusicScanner(MusicLibrary musicLibrary) {
        this.musicLibrary = musicLibrary;
        this.albumsMap = new HashMap<>();
    }
    public void scanDirectory(String directoryPath) {
        // Cancella il contenuto della cartella media prima di iniziare una nuova scansione
        clearMediaDirectory(new File(mediaFolderPath));

        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            processDirectory(directory);

            // Stampa tutti gli album creati
            printAllAlbums();
        } else {
            System.out.println("Il percorso specificato non è una directory valida.");
        }
    }

    // Metodo per stampare le informazioni sugli album
    private void printAllAlbums() {
        System.out.println("=== Album Trovati ===");
        for (Album album : albumsMap.values()) {
            System.out.println("Album: " + album.getName());
            System.out.println("Artista Album: " + album.getArtistAlbum());
            System.out.println("Genere: " + album.getGenre());

            // Stampa i brani associati a questo album
            System.out.println("Brani:");
            for (Track track : album.getTracks()) {
                System.out.println("  - Traccia " + track.getTrackNumber() + ": " + track.getTitle());
            }
            System.out.println("Path - " + album.getCoverImagePath());

            System.out.println("------------------------------");
        }
    }

    private void processDirectory(File directory) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(file); // Chiamata ricorsiva
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
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

                Integer trackNumberInt = (trackNumber != null && !trackNumber.isEmpty()) ? Integer.parseInt(trackNumber) : null;
                Integer discNumberInt = (discNumber != null && !discNumber.isEmpty()) ? Integer.parseInt(discNumber) : 1;

                int durationSeconds = (int) audioHeader.getTrackLength();

                Integer releaseYear = null;
                if (year != null && !year.isEmpty()) {
                    String[] parts = year.split("-");
                    releaseYear = Integer.parseInt(parts[0]);
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
                    // Create a new album
                    album = new Album(albumName, artistAlbum, coverImage, coverImagePath);
                    musicLibrary.addAlbum(album); // Add the album to the library
                } else {
                    System.out.println("Album già presente: " + albumName);
                }

                // Check if the track already exists
                Track existingTrack = musicLibrary.getTrackByFilePath(file.getAbsolutePath());
                if (existingTrack != null) {
                    System.out.println("Brano già presente: " + title + " - " + file.getAbsolutePath());
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

                // Update album genre based on tracks
                updateAlbumGenre(album);
            } else {
                System.out.println("Nessun tag trovato per il file: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Errore durante l'elaborazione del file: " + file.getAbsolutePath());
            e.printStackTrace();
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

    // Metodo per cancellare il contenuto della cartella media
    private void clearMediaDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    clearMediaDirectory(file); // Cancella ricorsivamente le sottocartelle
                }
                file.delete(); // Cancella i file
            }
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