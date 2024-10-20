package org.mypulse.model;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mypulse.view.MainView;

public class PlaylistEditor {
    private final Playlist playlist;
    private final Stage stage;
    private final TextField playlistNameField;
    private final TableView<Track> trackTableView;
    private final Button saveButton; // Pulsante Salva
    private final Button deleteButton; // Pulsante Elimina Brano

    public PlaylistEditor(Playlist playlist, MainView mainView) {
        this.playlist = playlist;
        this.stage = new Stage();

        // Imposta il titolo della finestra
        stage.setTitle("Modifica Playlist - " + playlist.getName());

        // Campo di testo per il nome della playlist
        playlistNameField = new TextField(playlist.getName());
        playlistNameField.setPromptText("Nome della Playlist");

        // Creazione della TableView per i brani
        trackTableView = new TableView<>();

        // Permetti la selezione multipla
        trackTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Creazione delle colonne per la tabella
        TableColumn<Track, String> titleColumn = new TableColumn<>("Titolo");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Track, String> artistColumn = new TableColumn<>("Artista");
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));

        TableColumn<Track, String> albumColumn = new TableColumn<>("Album");
        albumColumn.setCellValueFactory(new PropertyValueFactory<>("albumName"));

        TableColumn<Track, String> durationColumn = new TableColumn<>("Durata");
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));

        // Aggiunta delle colonne alla TableView
        trackTableView.getColumns().addAll(titleColumn, artistColumn, albumColumn, durationColumn);

        // Popolamento della tabella con i brani della playlist
        trackTableView.getItems().addAll(playlist.getTracks());

        // Aggiunta del supporto per il drag and drop
        addDragAndDropSupport();

        // Creazione del pulsante Salva
        saveButton = new Button("Salva");
        saveButton.setOnAction(event -> savePlaylist(mainView)); // Listener per il salvataggio

        // Creazione del pulsante Elimina Brano
        deleteButton = new Button("Elimina Brano");
        deleteButton.setOnAction(event -> deleteSelectedTracks()); // Listener per eliminare i brani selezionati

        // Layout principale
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Layout per il campo di testo della playlist
        HBox nameLayout = new HBox(10);
        nameLayout.getChildren().addAll(new Label("Nome Playlist:"), playlistNameField);
        HBox.setHgrow(playlistNameField, Priority.ALWAYS);

        // Layout per i pulsanti
        HBox buttonLayout = new HBox(10);
        buttonLayout.getChildren().addAll(saveButton, deleteButton);

        // Aggiunta degli elementi al layout principale
        mainLayout.getChildren().addAll(nameLayout, trackTableView, buttonLayout);

        // Impostazione del layout nel BorderPane e creazione della scena
        BorderPane root = new BorderPane(mainLayout);
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        stage.setScene(scene);
    }

    // Metodo per aggiungere il supporto per il drag and drop alla tabella
    private void addDragAndDropSupport() {
        trackTableView.setRowFactory(tv -> {
            TableRow<Track> row = new TableRow<>();

            // Gestione dell'inizio del drag
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();

                    // Crea il contenuto del drag
                    Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(index.toString());
                    dragboard.setContent(content);

                    event.consume();
                }
            });

            // Gestione del drag over (quando l'elemento trascinato è sopra una riga)
            row.setOnDragOver(event -> {
                if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            // Gestione del rilascio del drag
            row.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                if (dragboard.hasString()) {
                    int draggedIndex = Integer.parseInt(dragboard.getString());
                    Track draggedTrack = trackTableView.getItems().remove(draggedIndex);

                    int dropIndex;
                    if (row.isEmpty()) {
                        dropIndex = trackTableView.getItems().size();
                    } else {
                        dropIndex = row.getIndex();
                    }

                    trackTableView.getItems().add(dropIndex, draggedTrack);
                    event.setDropCompleted(true);

                    // Seleziona solo il brano nella nuova posizione
                    trackTableView.getSelectionModel().clearSelection();
                    trackTableView.getSelectionModel().select(dropIndex);

                } else {
                    event.setDropCompleted(false);
                }
                event.consume();
            });

            return row;
        });
    }


    // Metodo per eliminare i brani selezionati
    private void deleteSelectedTracks() {
        // Ottieni i brani selezionati
        var selectedTracks = trackTableView.getSelectionModel().getSelectedItems();

        // Rimuovi i brani dalla playlist
        playlist.getTracks().removeAll(selectedTracks);

        // Rimuovi i brani dalla tabella
        trackTableView.getItems().removeAll(selectedTracks);

        // Aggiorna la tabella
        trackTableView.refresh();
    }

    // Metodo per salvare la playlist
    private void savePlaylist(MainView mainView) {
        // Aggiorna il nome della playlist
        String newName = playlistNameField.getText().trim();
        if (!newName.isEmpty()) {
            playlist.setName(newName);
        }

        // Aggiorna l'ordine dei brani nella playlist
        playlist.getTracks().clear();
        playlist.getTracks().addAll(trackTableView.getItems());

        // Chiamata al MainView per aggiornare la tabella delle playlist
        mainView.updatePlaylistTableView();

        // Mostra un messaggio di conferma
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Salvataggio");
        alert.setHeaderText(null);
        alert.setContentText("La playlist è stata salvata con successo.");
        alert.showAndWait();

        // Chiudi l'editor
        stage.close();
    }

    // Metodo per visualizzare l'editor della playlist
    public void show() {
        stage.show();
    }
}
