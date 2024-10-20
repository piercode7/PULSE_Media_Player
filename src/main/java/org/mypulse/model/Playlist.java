package org.mypulse.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private String name; // Name of the playlist
    private List<Track> tracks; // List of tracks in the playlist
    private String description; // Optional description of the playlist

    // Constructor
    public Playlist(String name) {
        this.name = name;
        this.tracks = new ArrayList<>();
        this.description = "";
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Add a track to the playlist
    public void addTrack(Track track) {
        tracks.add(track);
    }

    // Remove a track from the playlist
    public boolean removeTrack(Track track) {
        return tracks.remove(track);
    }

    // Get the number of tracks in the playlist
    public int getTrackCount() {
        return tracks.size();
    }

    // Clear all tracks from the playlist
    public void clearPlaylist() {
        tracks.clear();
    }

    // Get track by index
    public Track getTrack(int index) {
        if (index >= 0 && index < tracks.size()) {
            return tracks.get(index);
        }
        return null; // Return null if index is out of bounds
    }

    // Check if the playlist contains a specific track
    public boolean containsTrack(Track track) {
        return tracks.contains(track);
    }

    // Get a copy of the playlist (to avoid modification of internal list)
    public List<Track> getTrackListCopy() {
        return new ArrayList<>(tracks);
    }

    @Override
    public String toString() {
        return name + " (" + tracks.size() + " tracks)";
    }
}
