package org.mypulse.model;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.mypulse.view.MainView;
import org.mypulse.view.components.AlbumListView;
import org.mypulse.view.components.ArtistListView;
import org.mypulse.view.components.TrackTableView;

import java.util.List;

public class SearchFrame extends Stage {

    private MusicLibrary musicLibrary;  // Riferimento alla libreria musicale
    private TextField searchField;
    private ListView<String> artistListView;
    private ListView<Album> albumListView;
    private ListView<Track> trackListView;
    private MainView mainView;
    private ArtistListView artistListViewMain;
    private AlbumListView albumListViewMain;
    private TrackTableView trackTableViewMain;

    public SearchFrame(MusicLibrary musicLibrary, MainView mainView) {
        // Inizializza la libreria musicale
        this.musicLibrary = musicLibrary;
        this.mainView = mainView;
        this.artistListViewMain = new ArtistListView(mainView, musicLibrary);
        this.albumListViewMain = new AlbumListView(mainView);
        this.trackTableViewMain = new TrackTableView(mainView);

        // Imposta il titolo del frame
        this.setTitle("Ricerca musica");

        // Crea il campo di ricerca
        searchField = new TextField();
        searchField.setPromptText("Cerca artisti, album o brani...");
        searchField.setMaxWidth(Double.MAX_VALUE); // Imposta la larghezza massima dinamica
        searchField.requestFocus();

        // Imposta la barra di ricerca in cima
        HBox searchBox = new HBox(searchField);
        searchBox.setPadding(new Insets(10));
        searchBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(searchField, Priority.ALWAYS); // Permette alla barra di ricerca di espandersi

        // Crea le ListView per visualizzare i risultati della ricerca
        artistListView = new ListView<>();
        albumListView = new ListView<>();
        trackListView = new ListView<>();

        // Imposta i placeholder per le ListView
        artistListView.setPlaceholder(new Label("Nessun artista trovato"));
        albumListView.setPlaceholder(new Label("Nessun album trovato"));
        trackListView.setPlaceholder(new Label("Nessun brano trovato"));

        // Etichette per le tre colonne
        Label artistLabel = new Label("Artisti");
        Label albumLabel = new Label("Album");
        Label trackLabel = new Label("Brani");

        // Crea una GridPane per organizzare le colonne
        GridPane resultsGrid = new GridPane();
        resultsGrid.setHgap(10);  // Spaziatura orizzontale
        resultsGrid.setVgap(10);  // Spaziatura verticale
        resultsGrid.setPadding(new Insets(20));

        // Definisci le tre colonne della griglia con larghezza uguale
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(33); // Ogni colonna occuperà un terzo della larghezza
        column1.setHgrow(Priority.ALWAYS); // Permette alla colonna di espandersi
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(33);
        column2.setHgrow(Priority.ALWAYS);
        ColumnConstraints column3 = new ColumnConstraints();
        column3.setPercentWidth(33);
        column3.setHgrow(Priority.ALWAYS);
        resultsGrid.getColumnConstraints().addAll(column1, column2, column3);

        // Aggiungi le etichette e le ListView alla griglia
        resultsGrid.add(artistLabel, 0, 0);
        resultsGrid.add(albumLabel, 1, 0);
        resultsGrid.add(trackLabel, 2, 0);

        resultsGrid.add(artistListView, 0, 1);
        resultsGrid.add(albumListView, 1, 1);
        resultsGrid.add(trackListView, 2, 1);

        // Imposta la crescita delle ListView in modo che si espandano con la finestra
        GridPane.setHgrow(artistListView, Priority.ALWAYS);
        GridPane.setHgrow(albumListView, Priority.ALWAYS);
        GridPane.setHgrow(trackListView, Priority.ALWAYS);
        GridPane.setVgrow(artistListView, Priority.ALWAYS);
        GridPane.setVgrow(albumListView, Priority.ALWAYS);
        GridPane.setVgrow(trackListView, Priority.ALWAYS);

        // Crea il layout principale
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(searchBox, resultsGrid);

        // Crea la scena e imposta il layout
        Scene scene = new Scene(layout, 1000, 500);
        scene.setFill(null);  // Set the initial scene fill to transparent
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        layout.applyCss();


        // Imposta la scena nello stage
        this.setScene(scene);
        setResizable(false);

        handleArtistClick(mainView);  // <--- Questo collega l'azione al click su un elemento della lista artisti
        handleAlbumClick(mainView);
        handleTrackClick(mainView);
        // Aggiungi listener per il campo di ricerca
        searchField.textProperty().addListener((observable, oldValue, newValue) -> searchMusic(newValue));

        // Personalizza le celle di albumListView e trackListView
        albumListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Album> call(ListView<Album> param) {
                return new AlbumListCell();
            }
        });

        trackListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Track> call(ListView<Track> param) {
                return new TrackListCell();
            }
        });



    }

    // Metodo per cercare musica e aggiornare le tre colonne
    private void searchMusic(String query) {
        if (query.isEmpty()) {
            artistListView.getItems().clear();
            albumListView.getItems().clear();
            trackListView.getItems().clear();
        } else {
            // Usa la libreria musicale per ottenere i risultati della ricerca
            List<String> artistResults = musicLibrary.searchArtists(query);
            List<Album> albumResults = musicLibrary.searchAlbums(query);
            List<Track> trackResults = musicLibrary.searchTracks(query);

            // Aggiorna le ListView con i risultati della ricerca
            artistListView.getItems().setAll(artistResults);
            albumListView.getItems().setAll(albumResults);
            trackListView.getItems().setAll(trackResults);
        }
    }

    // Classe personalizzata per visualizzare gli album con il nome dell'artista
    private static class AlbumListCell extends ListCell<Album> {
        @Override
        protected void updateItem(Album album, boolean empty) {
            super.updateItem(album, empty);
            if (empty || album == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox vbox = new VBox(5);
                vbox.setPadding(new Insets(5));

                Label albumName = new Label(album.getName());
                albumName.setStyle("-fx-font-weight: bold;");

                Label artistName = new Label(album.getArtistAlbum());
                artistName.setStyle("-fx-text-fill: gray;");

                vbox.getChildren().addAll(albumName, artistName);
                setGraphic(vbox);
            }
        }
    }

    // Classe personalizzata per visualizzare i brani con il nome dell'album e dell'artista
    private static class TrackListCell extends ListCell<Track> {
        @Override
        protected void updateItem(Track track, boolean empty) {
            super.updateItem(track, empty);
            if (empty || track == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox vbox = new VBox(5);
                vbox.setPadding(new Insets(5));

                Label trackTitle = new Label(track.getTitle());
                trackTitle.setStyle("-fx-font-weight: bold;");

                Label albumName = new Label("Album: " + track.getAlbumName());
                albumName.setStyle("-fx-text-fill: gray;");

                Label artistName = new Label("Artista: " + track.getArtistAlbum());
                artistName.setStyle("-fx-text-fill: gray;");

                vbox.getChildren().addAll(trackTitle, albumName, artistName);
                setGraphic(vbox);
            }
        }
    }

    // Metodo per gestire il click su un artista
    private void handleArtistClick(MainView mainView) {
        artistListView.setOnMouseClicked(event -> {
            String selectedArtist = artistListView.getSelectionModel().getSelectedItem();
            if (selectedArtist != null) {
                // Aggiungi qui l'azione da eseguire quando si clicca su un artista
                System.out.println("Artista recuperato: " + selectedArtist);

                // Seleziona la voce "Artisti" nel menu principale
                mainView.listViewMenu.getSelectionModel().select("Artisti");

                // Assicura che la visualizzazione cambi per mostrare la lista degli artisti e degli album
                artistListViewMain.populateArtists(); // Questo caricherà tutti gli artisti nella listViewArtist

                // Seleziona l'artista recuperato nella listViewArtist
                mainView.listViewArtist.getSelectionModel().select(selectedArtist);

                // Scrolla fino all'artista selezionato per assicurarsi che sia visibile
                mainView.listViewArtist.scrollTo(selectedArtist);

                // Ora che l'artista è selezionato, popola automaticamente la lista degli album
                albumListViewMain.populateAlbumsByArtist(selectedArtist);
                this.toBack();
                searchField.requestFocus();

            }
        });





    }


    // Metodo per gestire il click su un album
    private void handleAlbumClick(MainView mainView) {
        albumListView.setOnMouseClicked(event -> {
            Album selectedAlbum = albumListView.getSelectionModel().getSelectedItem();
            if (selectedAlbum != null) {
                // Recupera l'artista associato all'album selezionato
                String selectedArtist = selectedAlbum.getArtistAlbum();
                System.out.println("Artista e Album recuperati: " + selectedArtist + " - " + selectedAlbum.getName());

                // Seleziona la voce "Artisti" nel menu principale
                mainView.listViewMenu.getSelectionModel().select("Artisti");

                // Popola la lista degli artisti
                artistListViewMain.populateArtists();

                // Seleziona e scrolla fino all'artista associato all'album
                mainView.listViewArtist.getSelectionModel().select(selectedArtist);
                mainView.listViewArtist.scrollTo(selectedArtist);

                // Ora popola la lista degli album solo per quell'artista
                albumListViewMain.populateAlbumsByArtist(selectedArtist);

                // Seleziona l'album recuperato nella listViewAlbum
                mainView.listViewAlbum.getSelectionModel().select(selectedAlbum);
                mainView.listViewAlbum.scrollTo(selectedAlbum);

                // Ora che l'album è selezionato, popola automaticamente la lista dei brani
                trackTableViewMain.populateTracksByAlbum(selectedAlbum.getName());

                // Metti il SearchFrame in secondo piano
                this.toBack();
                searchField.requestFocus();
            }
        });
    }



    // Metodo per gestire il click su un brano
    private void handleTrackClick(MainView mainView) {
        trackListView.setOnMouseClicked(event -> {
            Track selectedTrack = trackListView.getSelectionModel().getSelectedItem();
            if (selectedTrack != null) {
                // Recupera l'artista e l'album associati al brano selezionato
                String selectedArtist = selectedTrack.getArtistAlbum();
                String selectedAlbum = selectedTrack.getAlbumName();
                System.out.println("Artista, Album e Brano recuperati: " + selectedArtist + " - " + selectedAlbum + " - " + selectedTrack.getTitle());

                // Seleziona la voce "Artisti" nel menu principale
                mainView.listViewMenu.getSelectionModel().select("Artisti");

                // Popola la lista degli artisti
                artistListViewMain.populateArtists();

                // Seleziona e scrolla fino all'artista associato al brano
                mainView.listViewArtist.getSelectionModel().select(selectedArtist);
                mainView.listViewArtist.scrollTo(selectedArtist);

                // Ora popola la lista degli album solo per quell'artista
                albumListViewMain.populateAlbumsByArtist(selectedArtist);

                // Seleziona e scrolla fino all'album associato al brano
                Album album = mainView.getMusicLibrary().getAlbumByName(selectedAlbum); // Supponendo che ci sia un metodo per recuperare l'album dalla libreria
                if (album != null) {
                    mainView.listViewAlbum.getSelectionModel().select(album);
                    mainView.listViewAlbum.scrollTo(album);

                    // Ora che l'album è selezionato, popola automaticamente la lista dei brani
                    trackTableViewMain.populateTracksByAlbum(album.getName());

                    // Seleziona e scrolla fino al brano selezionato nella tabella
                    mainView.tableViewTracks.getSelectionModel().select(selectedTrack);
                    mainView.tableViewTracks.scrollTo(selectedTrack);
                }

                // Porta il SearchFrame in secondo piano
                this.toBack();
                searchField.requestFocus();
            }
        });
    }




    // Metodo per mostrare il frame
    public void showSearchFrame() {
        this.show();
    }
}
