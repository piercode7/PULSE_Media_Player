package org.mypulse.model;

import java.io.Serializable;

public class Track implements Serializable {
    private String title;
    private Integer trackNumber;
    private Integer discNumber;
    private String filePath;
    private Integer duration;
    private String genre;
    private String artistAlbum;
    private String artist;
    private String albumName;
    private Integer releaseYear;
    private byte[] coverImage;
    private Album album;
    private String lyrics;
    private String composer;
    private Integer playCount;
    private Integer bitrate;
    private String format;



    public Track(String title, Integer trackNumber, Integer discNumber, String filePath, Integer duration,
                 String genre, String artistAlbum, String artist, String albumName, Integer releaseYear,
                 byte[] coverImage, Album album, String lyrics, String composer) {
        this.title = title;
        this.trackNumber = trackNumber;
        this.discNumber = discNumber;
        this.filePath = filePath;
        this.duration = duration;
        this.genre = genre;
        this.artistAlbum = artistAlbum;
        this.artist = artist;
        this.albumName = albumName;
        this.releaseYear = releaseYear;
        this.coverImage = coverImage;
        this.album = album;
        this.lyrics = lyrics;
        this.composer = composer;
        this.playCount = 0;



    }

    // Getter e Setter per tutti i campi
    // title
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // track
    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    // disc
    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    // path
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    // duration
    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    // genre
    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }


    // album artist
    public String getArtistAlbum() {
        return artistAlbum;
    }

    public void setArtistAlbum(String artistAlbum) {
        this.artistAlbum = artistAlbum;
    }


    // artist
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }


    // date
    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }


    // cover
    public byte[] getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(byte[] coverImage) {
        this.coverImage = coverImage;
    }


    // album
    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    // album
    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }


    // composer
    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }
    // lyrics
    // Gestione null safety per alcuni getter (esempio per `lyrics` e `composer`)
    public String getLyrics() {
        return (lyrics != null) ? lyrics : "";
    }
    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    // playCount
    public Integer getPlayCount() {
        return playCount;
    }

    // Metodo per incrementare il contatore di riproduzione
    public void incrementPlayCount() {
        if (playCount != null) {
            playCount++;
        } else {
            playCount = 1;
        }
    }

    public void resetPlayCount() {
        this.playCount = 0;
    }

    public Integer getBitrate() {
        return this.bitrate;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }
}
