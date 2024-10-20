package org.mypulse.view.components;

import org.mypulse.model.Album;
import org.mypulse.model.Track;
import org.mypulse.view.MainView;

public class AllViews {
    private final MainView mainView;

    // Constructor that takes an instance of MainView
    public AllViews(MainView mainView) {
        this.mainView = mainView;
    }

    public void refreshAllViews() {
        // Use getters from MainView to access lists and tables
        mainView.getListViewMenu().refresh();
        mainView.getListViewArtist().refresh();
        mainView.getListViewAlbum().refresh();
        mainView.getListViewAlbumAll().refresh();

        mainView.getTableViewTracks().refresh();
        mainView.getTableViewTrackAll().refresh();
        mainView.getTableViewTrackAllInQueue().refresh();
    }

    // Method to update the song information
    public void updateNowPlayingInfo(String title, String artist, String album) {
        mainView.getTitleLabel().setText(title != null ? title : "Titolo sconosciuto");
        mainView.getArtistLabel().setText(artist != null ? artist : "Artista sconosciuto");
        mainView.getAlbumLabel().setText(album != null ? album : "Album sconosciuto");
    }

    // Method to reset the song information
    public void resetNowPlayingInfo() {
        mainView.getTitleLabel().setText("");
        mainView.getArtistLabel().setText("");
        mainView.getAlbumLabel().setText("");
    }

    // Method to reset the view
    public void postInit() {
        // Clear all current views
        mainView.getListViewArtist().getItems().clear();
        mainView.getListViewAlbum().getItems().clear();
        mainView.getListViewAlbumAll().getItems().clear();
        mainView.getTableViewTracks().getItems().clear();
        mainView.getTableViewTrackAll().getItems().clear();
        mainView.getTableViewTrackAllInQueue().getItems().clear();

        // Clear any existing album cover image
        mainView.getAlbumCoverView().setImage(mainView.getDefaultImage());

        // Select "Artisti" in the menu to reset the view
        mainView.getListViewMenu().getSelectionModel().select("Artisti");

        // Repopulate the artist list
        mainView.getArtistListView().populateArtists();
    }

    // New method to set selections post-editing
    public void setSelectionsPostEditing(String artistName, String albumName, String trackName) {
        // Select "Artisti" in the main menu
        mainView.getListViewMenu().getSelectionModel().select("Artisti");

        // Populate the list of artists
        mainView.getArtistListView().populateArtists();

        // Select and scroll to the artist
        for (String artist : mainView.getListViewArtist().getItems()) {
            if (artist.equals(artistName)) {
                mainView.getListViewArtist().getSelectionModel().select(artist);
                mainView.getListViewArtist().scrollTo(artist);
                break;
            }
        }

        // Populate albums for the selected artist
        mainView.getAlbumListView().populateAlbumsByArtist(artistName);

        // Select and scroll to the album
        for (Album album : mainView.getListViewAlbum().getItems()) {
            if (album.getName().equals(albumName)) {
                mainView.getListViewAlbum().getSelectionModel().select(album);
                mainView.getListViewAlbum().scrollTo(album);
                break;
            }
        }

        // Populate tracks for the selected album
        mainView.getTrackTableView().populateTracksByAlbum(albumName);

        // Select and scroll to the track in the table
        for (Track track : mainView.getTableViewTracks().getItems()) {
            if (track.getTitle().equals(trackName)) {
                mainView.getTableViewTracks().getSelectionModel().select(track);
                mainView.getTableViewTracks().scrollTo(track);
                break;
            }
        }
    }
}
