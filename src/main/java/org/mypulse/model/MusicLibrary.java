package org.mypulse.model;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MusicLibrary implements Serializable {
    private static final long serialVersionUID = 716952489036810550L; // Versione fissa per la serializzazione

    private Map<String, List<Track>> tracksByArtist; // Mappa dei brani per artista
    private Map<String, List<Track>> tracksByAlbum;  // Mappa dei brani per album
    private Map<String, Album> albumsByName; // Mappa per gli album
    private Map<String, BufferedImage> albumCovers; // Mappa per le copertine degli album
    private List<Track> allTracks; // Lista che contiene tutti i brani
    private Map<String, Playlist> playlists; // Mappa delle playlist

    public MusicLibrary() {
        tracksByArtist = new HashMap<>();
        tracksByAlbum = new HashMap<>();
        albumsByName = new HashMap<>(); // Inizializza la mappa degli album
        albumCovers = new HashMap<>();
        allTracks = new ArrayList<>(); // Inizializza la lista di tutti i brani
        playlists = new HashMap<>(); // Inizializza la mappa delle playlist
    }

    // Aggiungi un album alla libreria
    public void addAlbum(Album album) {
        if (album != null && album.getName() != null) {
            albumsByName.put(album.getName(), album); // Aggiungi l'album alla mappa degli album
        }
    }

    // Recupera un album dal nome
    public Album getAlbumByName(String albumName) {
        return albumsByName.get(albumName); // Ritorna l'album se esiste, altrimenti null
    }

    // Aggiungi un brano
    public void addTrack(Track track) {
        if (track == null || track.getArtistAlbum() == null || track.getAlbumName() == null) {
            return; // Ignora se il brano o i campi essenziali sono null
        }

        String artist = track.getArtistAlbum();
        String album = track.getAlbumName();

        // Aggiungi il brano all'indice degli artisti
        tracksByArtist.computeIfAbsent(artist, k -> new ArrayList<>()).add(track);
        // Aggiungi il brano all'indice degli album
        tracksByAlbum.computeIfAbsent(album, k -> new ArrayList<>()).add(track);
        // Aggiungi il brano alla lista di tutti i brani
        allTracks.add(track);
    }

    // Ottieni i brani di un artista
    public List<Track> getTracksByArtist(String artist) {
        return tracksByArtist.getOrDefault(artist, new ArrayList<>());
    }

    // Ottieni i brani di un album
    public List<Track> getTracksByAlbum(String album) {
        return tracksByAlbum.getOrDefault(album, new ArrayList<>());
    }

    // Ottieni tutti gli artisti
    public List<String> getAllArtists() {
        return new ArrayList<>(tracksByArtist.keySet());
    }

    // Ottieni tutti gli album
    public List<String> getAllAlbums() {
        return new ArrayList<>(albumsByName.keySet());
    }

    // Metodo per stampare gli artisti con i loro album e brani
    public void printArtistsWithAlbumsAndTracks() {
        for (String artist : getAllArtists()) {
            System.out.println("Artist: " + artist);
            List<String> albums = getArtistAlbums(artist);
            for (String album : albums) {
                System.out.println("  Album: " + album);
                List<Track> tracks = getTracksByAlbum(album);
                for (Track track : tracks) {
                    System.out.printf("    Track: %s, Track Number: %d, Disc Number: %d, Duration: %d seconds%n",
                            track.getTitle(), track.getTrackNumber(), track.getDiscNumber(), track.getDuration());
                }
            }
        }
    }

    // Ottieni gli album di un artista
    public List<String> getArtistAlbums(String artist) {
        List<String> albums = new ArrayList<>();
        List<Track> tracks = getTracksByArtist(artist);
        for (Track track : tracks) {
            if (!albums.contains(track.getAlbumName())) {
                albums.add(track.getAlbumName());
            }
        }
        return albums;
    }

    public List<Album> getAlbums() {
        return new ArrayList<>(albumsByName.values());
    }

    public List<String> getAlbumNames() {
        List<String> albumNames = new ArrayList<>();
        for (Album album : albumsByName.values()) {
            albumNames.add(album.getName());  // Aggiungi il nome dell'album
        }
        return albumNames;  // Restituisci la lista di nomi degli album
    }

    // Metodo helper per ottenere album di un artista come oggetti Album
    public List<Album> getAlbumsByArtist(String artist) {
        List<Album> albumList = new ArrayList<>();
        List<Track> tracks = getTracksByArtist(artist);

        for (Track track : tracks) {
            Album album = getAlbumByName(track.getAlbumName());
            if (album != null && !albumList.contains(album)) {
                albumList.add(album);
            }
        }

        return albumList;
    }

    // Ottieni tutti i brani
    public List<Track> getAllTracks() {
        return new ArrayList<>(allTracks); // Restituisce una nuova lista con tutti i brani
    }

    // Metodo per copiare i dati da un'altra libreria musicale
    public void copyFrom(MusicLibrary otherLibrary) {
        this.tracksByArtist.clear();
        this.tracksByArtist.putAll(otherLibrary.tracksByArtist);

        this.tracksByAlbum.clear();
        this.tracksByAlbum.putAll(otherLibrary.tracksByAlbum);

        this.albumsByName.clear();
        this.albumsByName.putAll(otherLibrary.albumsByName);

        this.allTracks.clear();
        this.allTracks.addAll(otherLibrary.allTracks);

        this.playlists.clear();
        this.playlists.putAll(otherLibrary.playlists);
    }

    // Ottieni un brano in base al percorso del file
    public Track getTrackByFilePath(String absolutePath) {
        for (Track track : allTracks) {
            if (track.getFilePath().equals(absolutePath)) {
                return track; // Ritorna il brano se trovato
            }
        }
        return null; // Se nessun brano corrisponde, ritorna null
    }

    // Implementa i metodi di ricerca
    public List<String> searchArtists(String query) {
        return tracksByArtist.keySet().stream()
                .filter(artist -> artist.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Album> searchAlbums(String query) {
        return albumsByName.values().stream()
                .filter(album -> album.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Track> searchTracks(String query) {
        return allTracks.stream()
                .filter(track -> track.getTitle().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Metodo per ottenere un brano in base a titolo, artista e album
    public Track getTrackByDetails(String title, String artist, String album) {
        for (Track track : allTracks) {
            if (track.getTitle().equalsIgnoreCase(title) &&
                    track.getArtist().equalsIgnoreCase(artist) &&
                    track.getAlbumName().equalsIgnoreCase(album)) {
                return track; // Restituisce il brano corrispondente
            }
        }
        return null; // Restituisce null se non è stato trovato alcun brano con i dettagli forniti
    }

    public void clearLibrary() {
        tracksByArtist.clear();
        tracksByAlbum.clear();
        albumsByName.clear();
        albumCovers.clear();
        allTracks.clear();
        playlists.clear();
    }

    public void removeTrack(Track track) {
        if (track == null) return;

        String artist = track.getArtistAlbum();
        String album = track.getAlbumName();

        // Rimuovi il brano dalle mappe di artisti e album
        List<Track> artistTracks = tracksByArtist.get(artist);
        if (artistTracks != null) {
            artistTracks.remove(track);
            if (artistTracks.isEmpty()) {
                tracksByArtist.remove(artist);
            }
        }

        List<Track> albumTracks = tracksByAlbum.get(album);
        if (albumTracks != null) {
            albumTracks.remove(track);
            if (albumTracks.isEmpty()) {
                tracksByAlbum.remove(album);
                albumsByName.remove(album);
            }
        }

        // Rimuovi il brano dalla lista di tutti i brani
        allTracks.remove(track);
    }

    public void removeAlbum(Album oldAlbum) {
        if (oldAlbum == null) return;

        String albumName = oldAlbum.getName();

        // Rimuovi tutti i brani associati a questo album
        List<Track> albumTracks = tracksByAlbum.get(albumName);
        if (albumTracks != null) {
            for (Track track : new ArrayList<>(albumTracks)) {
                removeTrack(track);
            }
        }

        // Rimuovi l'album dalla mappa albumsByName
        albumsByName.remove(albumName);

        // Rimuovi la copertina dell'album se esiste
        albumCovers.remove(albumName);

        // Rimuovi l'album dalla mappa tracksByAlbum
        tracksByAlbum.remove(albumName);
    }

    // Aggiungi una playlist alla libreria
    public void addPlaylist(Playlist playlist) {
        if (playlist != null && playlist.getName() != null) {
            playlists.put(playlist.getName(), playlist);
        }
    }

    // Rimuovi una playlist dalla libreria
    public void removePlaylist(Playlist playlist) {
        if (playlist != null) {
            playlists.remove(playlist.getName());
        }
    }

    // Ottieni tutte le playlist
    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists.values());
    }

    // Ottieni una playlist per nome
    public Playlist getPlaylistByName(String playlistName) {
        return playlists.get(playlistName);
    }
    // Ottieni tutte le playlist come lista
    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists.values());
    }

}
