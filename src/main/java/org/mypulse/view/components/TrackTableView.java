package org.mypulse.view.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.mypulse.model.Playlist;
import org.mypulse.model.Track;
import org.mypulse.view.MainView;

import java.util.Comparator;
import java.util.List;


public class TrackTableView {
    private final MainView mainView;

    // Constructor that accepts an instance of MainView
    public TrackTableView(MainView mainView) {
        this.mainView = mainView;
    }

    // Method to populate tracks by album
    public void populateTracksByAlbum(String albumName) {
        mainView.getTableViewTracks().getItems().clear();
        List<Track> trackList = mainView.getMusicLibrary().getTracksByAlbum(albumName);

        // Sort the list by disc number and then by track number (both as Integers)
        trackList.sort(Comparator.comparing(Track::getDiscNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Track::getTrackNumber, Comparator.nullsLast(Integer::compareTo)));

        mainView.getTableViewTracks().getItems().addAll(trackList);

        // Set the columns for disc number and track number
        TableColumn<Track, Integer> discNumberColumn = (TableColumn<Track, Integer>) mainView.getTableViewTracks().getColumns().get(5); // Assuming "Disco" is at index 5
        discNumberColumn.setCellValueFactory(new PropertyValueFactory<>("discNumber"));
        discNumberColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<Track, Integer> trackNumberColumn = (TableColumn<Track, Integer>) mainView.getTableViewTracks().getColumns().get(0); // Assuming "Indice" is at index 0
        trackNumberColumn.setCellValueFactory(new PropertyValueFactory<>("trackNumber"));
        trackNumberColumn.setSortType(TableColumn.SortType.ASCENDING);

        // Set the sort order: first by disc number, then by track number
        mainView.getTableViewTracks().getSortOrder().add(discNumberColumn);
        mainView.getTableViewTracks().getSortOrder().add(trackNumberColumn);
        mainView.getTableViewTracks().sort();
        mainView.setCurrentTableMode("Album");
    }

    // Method to populate all tracks
    public void populateAllTracks() {
        mainView.getTableViewTrackAll().getItems().clear();
        List<Track> allTracks = mainView.getMusicLibrary().getAllTracks();

        // Sort the tracks by disc number and then by track number
        allTracks.sort(Comparator.comparing(Track::getDiscNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Track::getTrackNumber, Comparator.nullsLast(Integer::compareTo)));

        // Add sorted tracks to the table
        mainView.getTableViewTrackAll().getItems().addAll(allTracks);

        // Set the columns for disc number and track number
        TableColumn<Track, Integer> discNumberColumn = (TableColumn<Track, Integer>) mainView.getTableViewTrackAll().getColumns().get(5); // Assuming "Disco" is at index 5
        discNumberColumn.setCellValueFactory(new PropertyValueFactory<>("discNumber"));
        discNumberColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<Track, Integer> trackNumberColumn = (TableColumn<Track, Integer>) mainView.getTableViewTrackAll().getColumns().get(0); // Assuming "Indice" is at index 0
        trackNumberColumn.setCellValueFactory(new PropertyValueFactory<>("trackNumber"));
        trackNumberColumn.setSortType(TableColumn.SortType.ASCENDING);

        // Set the sort order: first by disc number, then by track number
        mainView.getTableViewTrackAll().getSortOrder().add(discNumberColumn);
        mainView.getTableViewTrackAll().getSortOrder().add(trackNumberColumn);
        mainView.getTableViewTrackAll().sort();
    }


    // Metodo per popolare la tabella estesa con tutti i brani
    public void populateExtendedTrackTable() {
        TableView<Track> extendedTableView = mainView.getTableViewTrackAll(); // Ottieni la tabella estesa dal MainView
        extendedTableView.getItems().clear(); // Pulisci la tabella prima di aggiungere nuovi elementi

        List<Track> allTracks = mainView.getMusicLibrary().getAllTracks();

        // Ordina i brani per titolo in ordine alfabetico
        allTracks.sort(Comparator.comparing(Track::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)));

        // Aggiungi i brani ordinati alla tabella estesa
        extendedTableView.getItems().addAll(allTracks);

        // Imposta le colonne per rispettare l'ordine della TrackTableFactoryExtended
        TableColumn<Track, Integer> trackNumberColumn = (TableColumn<Track, Integer>) extendedTableView.getColumns().get(0); // "Indice"
        trackNumberColumn.setCellValueFactory(new PropertyValueFactory<>("trackNumber"));

        TableColumn<Track, String> titleColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(1); // "Titolo"
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));

        TableColumn<Track, String> albumColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(2); // "Album"
        albumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAlbumName()));

        TableColumn<Track, String> artistColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(3); // "Artista"
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtist()));

        TableColumn<Track, String> artistAlbumColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(4); // "Artista Album"
        artistAlbumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtistAlbum()));

        TableColumn<Track, String> composerColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(5); // "Compositore"
        composerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getComposer()));

        TableColumn<Track, String> genreColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(6); // "Genere"
        genreColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGenre()));

        TableColumn<Track, String> yearColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(7); // "Anno"
        yearColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getReleaseYear() != null ? cellData.getValue().getReleaseYear().toString() : "N/A"));

        TableColumn<Track, String> durationColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(8); // "Durata"
        durationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                formatDurationInHMS(cellData.getValue().getDuration())));

        TableColumn<Track, Integer> discNumberColumn = (TableColumn<Track, Integer>) extendedTableView.getColumns().get(9); // "Disco"
        discNumberColumn.setCellValueFactory(new PropertyValueFactory<>("discNumber"));

        TableColumn<Track, Integer> playCountColumn = (TableColumn<Track, Integer>) extendedTableView.getColumns().get(10); // "Play Count"
        playCountColumn.setCellValueFactory(new PropertyValueFactory<>("playCount"));

        // Imposta l'ordine di default (ad esempio, per titolo)
        titleColumn.setSortType(TableColumn.SortType.ASCENDING);

        // Aggiungi la colonna per il sorting di default
        extendedTableView.getSortOrder().add(titleColumn);

        // Ordina la tabella in base all'ordine impostato
        extendedTableView.sort();

        // Aggiorna la visualizzazione della tabella
        extendedTableView.refresh();
    }


    // Metodo per formattare la durata in formato HH:MM:SS o MM:SS
    private String formatDurationInHMS(Integer durationInSeconds) {
        if (durationInSeconds == null) {
            return "N/A";
        }
        int hours = durationInSeconds / 3600;
        int minutes = (durationInSeconds % 3600) / 60;
        int seconds = durationInSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }


    // Metodo per popolare la tabella con tutti i brani della coda di riproduzione
    public void populateAllTracksInQueue() {
        TableView<Track> extendedTableView = mainView.getTableViewTrackAllInQueue(); // Ottieni la tabella per i brani in coda
        extendedTableView.getItems().clear(); // Pulisci la tabella prima di aggiungere nuovi elementi

        List<Track> queueTracks = mainView.getQueueTracks(); // Ottieni la lista dei brani in coda

        // Aggiungi i brani alla tabella mantenendo l'ordine originale
        extendedTableView.getItems().addAll(queueTracks);

        // Imposta le colonne per rispettare l'ordine della TrackTableFactoryExtended
        TableColumn<Track, Integer> trackNumberColumn = (TableColumn<Track, Integer>) extendedTableView.getColumns().get(0); // "Indice"
        trackNumberColumn.setCellValueFactory(new PropertyValueFactory<>("trackNumber"));

        TableColumn<Track, String> titleColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(1); // "Titolo"
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));

        TableColumn<Track, String> albumColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(2); // "Album"
        albumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAlbumName()));

        TableColumn<Track, String> artistColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(3); // "Artista"
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtist()));

        TableColumn<Track, String> artistAlbumColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(4); // "Artista Album"
        artistAlbumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtistAlbum()));

        TableColumn<Track, String> composerColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(5); // "Compositore"
        composerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getComposer()));

        TableColumn<Track, String> genreColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(6); // "Genere"
        genreColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGenre()));

        TableColumn<Track, String> yearColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(7); // "Anno"
        yearColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getReleaseYear() != null ? cellData.getValue().getReleaseYear().toString() : "N/A"));

        TableColumn<Track, String> durationColumn = (TableColumn<Track, String>) extendedTableView.getColumns().get(8); // "Durata"
        durationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                formatDurationInHMS(cellData.getValue().getDuration())));

        TableColumn<Track, Integer> discNumberColumn = (TableColumn<Track, Integer>) extendedTableView.getColumns().get(9); // "Disco"
        discNumberColumn.setCellValueFactory(new PropertyValueFactory<>("discNumber"));

        TableColumn<Track, Integer> playCountColumn = (TableColumn<Track, Integer>) extendedTableView.getColumns().get(10); // "Play Count"
        playCountColumn.setCellValueFactory(new PropertyValueFactory<>("playCount"));

        // Aggiorna la visualizzazione della tabella
        extendedTableView.refresh();

        System.out.println("Tabella riempita con i brani della coda di riproduzione. Numero di brani: " + queueTracks.size());
    }




    // Method to populate tracks by playlist
    public void populateTracksByPlaylist(String playlistName) {
        mainView.getTableViewTracks().getItems().clear();

        // Get the playlist by name
        Playlist playlist = mainView.getMusicLibrary().getPlaylistByName(playlistName);
        if (playlist != null) {
            List<Track> trackList = playlist.getTracks();

            // Add tracks to the table in their original order
            mainView.getTableViewTracks().getItems().addAll(trackList);

            // Set the columns for disc number and track number (if needed)
            TableColumn<Track, Integer> discNumberColumn = (TableColumn<Track, Integer>) mainView.getTableViewTracks().getColumns().get(5); // Assuming "Disco" is at index 5
            discNumberColumn.setCellValueFactory(new PropertyValueFactory<>("discNumber"));

            TableColumn<Track, Integer> trackNumberColumn = (TableColumn<Track, Integer>) mainView.getTableViewTracks().getColumns().get(0); // Assuming "Indice" is at index 0
            trackNumberColumn.setCellValueFactory(new PropertyValueFactory<>("trackNumber"));

            // Clear any existing sort order to maintain the original order
            mainView.getTableViewTracks().getSortOrder().clear();

            mainView.setCurrentTableMode("Playlist");
            // Refresh the table view
            mainView.getTableViewTracks().refresh();
        } else {
            System.out.println("Playlist not found: " + playlistName);
        }
    }










}
