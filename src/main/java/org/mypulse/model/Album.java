package org.mypulse.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Album implements Serializable {
    private String name;
    private String artist;
    private String coverImagePath; // Percorso dell'immagine salvata
    private byte[] coverImageData;  // Metadato della copertina
    private List<Track> tracks;     // Lista dei brani dell'album
    private String genre;           // Genere dell'album
    private Integer releaseDate;    // Data di rilascio dell'album

    public Album(String name, String artist, byte[] coverImageData, String coverImagePath) {
        this.name = name;
        this.artist = artist;
        this.coverImageData = coverImageData; // Salva i metadati della copertina
        this.coverImagePath = coverImagePath; // Percorso della copertina salvata localmente
        this.tracks = new ArrayList<>();
    }

    // Aggiungi un brano all'album
    public void addTrack(Track track) {
        this.tracks.add(track);

        // Se è il primo brano, impostiamo il genere e la data di rilascio
        if (tracks.size() == 1) {
            this.genre = track.getGenre();
            this.releaseDate = track.getReleaseYear();
        }
    }

    // Getter e Setter
    public String getCoverImagePath() {
        return coverImagePath;
    }

    public byte[] getCoverImageData() {
        return coverImageData;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public String getName() {
        return name;
    }

    public String getArtistAlbum() {
        return artist;
    }

    public String getGenre() {
        return genre != null ? genre : "Genere sconosciuto";  // Ritorna il genere, o un valore predefinito
    }

    public Integer getReleaseDate() {
        return releaseDate != null ? releaseDate : null;  // Ritorna la data di rilascio se presente
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setReleaseDate(Integer releaseDate) {
        this.releaseDate = releaseDate;
    }
    public void removeTrack(Track track) {
        tracks.remove(track); // Assuming `tracks` is a list of tracks in the album
    }


    public void setCoverImage(byte[] newCoverImage) {
        this.coverImageData = newCoverImage;
    }

    public byte[] getCoverImage() {
        return coverImageData;
    }

    public String getArtistName() {
        return artist.toString();
    }

    public void setName(String name) {
        this.name = name;
    }



    public void setArtistAlbum(String newArtistAlbum) {
        this.artist = newArtistAlbum;
    }
}
