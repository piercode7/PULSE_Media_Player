package org.mypulse.model;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mypulse.util.Utils;
import org.mypulse.view.MainView;
import org.mypulse.view.components.AllViews;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.input.DragEvent;


public class MediaPlayerController extends VBox {

    private MediaPlayer mediaPlayer; // Lettore multimediale
    private Slider progressSlider;   // Slider per il progresso
    private Label trackTimeLabel;    // Etichetta per il tempo del brano
    private Slider volumeSlider;     // Slider per il volume
    private MainView mainView;       // Riferimento a
    private ToggleButton replayButton;// lla vista principale
    private Button playButton;       // Pulsante Play
    private Button pauseButton;      // Pulsante Pause
    private Button stopButton;       // Pulsante Stop
    private SearchFrame searchFrame; // Variabile per memorizzare il frame di ricerca
    private List<Track> queuedTracks; // coda di ascolto
    private Button nextButton;
    private Button prevButton;
    private int currentTrackIndex = -1; // Indice del brano attualmente in riproduzione
    private TableView<Track> currentTable;
    private List<Track> orderedSelectedTracks; // Lista ordinata di selezioni
    private AllViews allViews;
    private Utils utils;


    public MediaPlayerController(MusicLibrary musicLibrary, MainView mainView, AllViews allViews) {
        this.mainView = mainView; // Riferimento alla MainView
        queuedTracks = mainView.getQueueTracks();
        this.orderedSelectedTracks = new ArrayList<>();
        this.allViews = allViews; // Riferimento ad AllViews
        this.utils = new Utils();


        // Set padding and spacing for the media player
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // Create the controls layout for the buttons
        HBox controlsLayout = new HBox(10);
        controlsLayout.setAlignment(Pos.CENTER); // Center the buttons horizontally

        // Create media control buttons
        replayButton = new ToggleButton("R");
        replayButton.setStyle("-fx-font-size: 14px;");
        replayButton.getStyleClass().add("replayButton-theme");

        prevButton = new Button("<<");
        playButton = new Button("Play");
        pauseButton = new Button("Pause");
        stopButton = new Button("X");
        nextButton = new Button(">>");

// Create fixed-width spacers
        Region spacer1 = new Region();
        spacer1.setMinWidth(5);  // Adjust width for space between 'R' and '<<'

        Region spacer2 = new Region();
        spacer2.setMinWidth(5);  // Adjust width for space between 'X' and '>>'

// Add buttons and spacers to controls layout
        controlsLayout.getChildren().addAll(replayButton, spacer1, prevButton, playButton, pauseButton, nextButton, spacer2, stopButton);

// Center align the layout
        controlsLayout.setAlignment(Pos.CENTER);
// -----------------------------------------------------------------------


        // Create the volume slider
        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setPrefWidth(150);

        // Create the track time label
        trackTimeLabel = new Label("00:00 / 00:00");

        // Create the progress slider
        progressSlider = new Slider();
        progressSlider.setPrefWidth(400);


        // Aggiungi l'azione per il bottone di ricerca

        // Create a layout for search, progress, and volume
        HBox progressAndVolumeLayout = new HBox(20);  // Increased spacing to 20 for better separation
        progressAndVolumeLayout.setPadding(new Insets(10, 0, 0, 0));
        progressAndVolumeLayout.setAlignment(Pos.CENTER);  // Center-align the layout

        // Make the progress slider expand and stay centered
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressSlider.setMaxWidth(Double.MAX_VALUE);  // Allow the progress slider to expand

        // Push the volume slider to the right and the search button to the left
        HBox.setHgrow(volumeSlider, Priority.NEVER);

        // Add the search button, sliders, and label to the layout
        progressAndVolumeLayout.getChildren().addAll(progressSlider, trackTimeLabel, volumeSlider);

        // Add the controls and the progress bar to the main layout
        this.getChildren().addAll(controlsLayout, progressAndVolumeLayout);
// ------------------------------------------------------------------------------


        // Aggiungi azioni ai pulsanti
        playButton.setOnAction(event -> playOrResumeTrack());
        pauseButton.setOnAction(event -> pauseTrack());
        stopButton.setOnAction(event -> stopTrack());
        // Aggiungi l'azione per il pulsante "Avanti"
        nextButton.setOnAction(event -> nextTrack());
// Aggiungi l'azione per il pulsante "Indietro"
        prevButton.setOnAction(event -> previousTrack());

        // Add an action listener to handle the replay toggle
        replayButton.setOnAction(event -> {
            if (replayButton.isSelected()) {
                System.out.println("Replay is ON");
                // Add your replay logic here
            } else {
                System.out.println("Replay is OFF");
                // Add logic to disable replay here
            }
        });


// Chiamata del metodo per entrambe le tabelle
        setRowFactoryForTableView(mainView.getTableViewTracks());  // Per la tabella album/artista
        setRowFactoryForTableView(mainView.getTableViewTrackAll()); // Per la tabella "tutti i brani"
        setRowFactoryForTableView(mainView.getTableViewTrackAllInQueue()); // Per la tabella "brani in coda"


    }

    private void setRowFactoryForTableView(TableView<Track> tableView) {
        tableView.setRowFactory(tv -> {
            TableRow<Track> row = new TableRow<Track>() {
                @Override
                protected void updateItem(Track item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle(""); // Rimuovi lo stile quando non c'è alcun brano nella riga
                    } else if (item.equals(mainView.getCurrentlyPlayingTrack())) {
                        setStyle("-fx-background-color: rgba(255,0,0,0.41);"); // Evidenzia la riga del brano in riproduzione
                    } else {
                        setStyle(""); // Resetta lo stile per le altre righe
                    }
                }
            };

            // Aggiungi il listener per gestire la selezione multipla
            addSelectionListener(tableView);


            // Listener per il doppio click che avvia la riproduzione del brano selezionato
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Track selectedTrack = row.getItem();
                    playSelectedTrack(selectedTrack); // Riproduci il brano selezionato
                    fillQueueWithTableTracks(tableView); // Riempie la coda con i brani presenti nella tabella
                }


                // Mostra il menu contestuale quando si usa il clic destro
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty() && tableView != mainView.getTableViewTrackAllInQueue()) {
                    // Crea il menu contestuale
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem addToQueueItem = new MenuItem("Aggiungi alla coda");
                    MenuItem playNextItem = new MenuItem("Riproduci come successivo");
                    MenuItem editTrackData = new MenuItem("Mostra informazioni brano");
                    MenuItem showInFinder = new MenuItem("Mostra percorso file");
                    MenuItem addToPlaylist = new MenuItem("Aggiungi ad una playlist");
                    MenuItem deleteTrack = new MenuItem("Elimina brano");


                    addToQueueItem.setOnAction(e -> {
                        if (!orderedSelectedTracks.isEmpty()) {
                            // Add selected tracks in the order they were selected
                            for (Track track : orderedSelectedTracks) {
                                // Add each selected track to the end of the queue, preserving duplicates
                                queuedTracks.add(track);
                                System.out.println("Aggiunto alla coda: " + track.getTitle());
                            }

                            System.out.println("Coda aggiornata con " + queuedTracks.size() + " brani.");

                            // Update the queue table
                            TableView<Track> queueTable = mainView.getTableViewTrackAllInQueue();
                            queueTable.getItems().clear();  // Clear current table
                            queueTable.getItems().addAll(queuedTracks);  // Add updated queue
                            queueTable.refresh();  // Refresh the table
                        }
                    });

                    showInFinder.setOnAction(e -> {
                        Track selectedTrack = tableView.getSelectionModel().getSelectedItem(); // Get the selected track
                        if (selectedTrack != null) {
                            File trackFile = new File(selectedTrack.getFilePath());

                            if (trackFile.exists()) {
                                try {
                                    String os = System.getProperty("os.name").toLowerCase();
                                    if (os.contains("win")) {
                                        // Windows
                                        String command = "explorer /select,\"" + trackFile.getAbsolutePath() + "\"";
                                        Runtime.getRuntime().exec(command);
                                    } else if (os.contains("mac")) {
                                        // macOS
                                        String command = "open -R \"" + trackFile.getAbsolutePath() + "\"";
                                        Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
                                    } else if (os.contains("nix") || os.contains("nux")) {
                                        // Linux: Use 'xdg-open' to open the directory (file selection is not supported in most Linux file managers)
                                        String command = "xdg-open \"" + trackFile.getParent() + "\"";
                                        Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
                                    } else {
                                        // Unsupported OS
                                        System.out.println("Unsupported operating system.");
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                System.out.println("File does not exist: " + trackFile.getAbsolutePath());
                            }
                        } else {
                            System.out.println("No track selected.");
                        }
                    });


                    deleteTrack.setOnAction(e -> {
                        ObservableList<Track> selectedTracks = tableView.getSelectionModel().getSelectedItems(); // Get all selected tracks

                        if (!selectedTracks.isEmpty()) {
                            // Confirm the deletion for all selected tracks
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Delete Tracks");
                            alert.setHeaderText("Are you sure you want to delete the selected tracks?");
                            alert.setContentText("Number of tracks selected: " + selectedTracks.size());

                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                List<Track> tracksToRemove = new ArrayList<>(selectedTracks); // Make a copy to avoid modification during iteration

                                // Ask if the user wants to delete the files for the tracks
                                Alert deleteFileAlert = new Alert(Alert.AlertType.CONFIRMATION);
                                deleteFileAlert.setTitle("Delete Files");
                                deleteFileAlert.setHeaderText("Do you also want to delete the track files?");
                                deleteFileAlert.setContentText("This action cannot be undone.");
                                Optional<ButtonType> fileResult = deleteFileAlert.showAndWait();
                                boolean deleteFiles = fileResult.isPresent() && fileResult.get() == ButtonType.OK;

                                for (Track track : tracksToRemove) {
                                    // Remove the track from the library
                                    mainView.getMusicLibrary().removeTrack(track);
                                    System.out.println("Track removed from library: " + track.getTitle());

                                    // Delete the file if the user chose to
                                    if (deleteFiles) {
                                        File trackFile = new File(track.getFilePath());
                                        if (trackFile.exists() && trackFile.delete()) {
                                            System.out.println("File deleted: " + trackFile.getAbsolutePath());
                                        } else {
                                            System.out.println("Failed to delete the file or file does not exist.");
                                        }
                                    }
                                }

                                // Refresh the table views to reflect changes
                                mainView.getTableViewTracks().getItems().removeAll(tracksToRemove);
                                mainView.getTableViewTrackAll().getItems().removeAll(tracksToRemove);
                                mainView.getTableViewTrackAllInQueue().getItems().removeAll(tracksToRemove);

                                mainView.getTableViewTracks().refresh();
                                mainView.getTableViewTrackAll().refresh();
                                mainView.getTableViewTrackAllInQueue().refresh();
                            }
                        } else {
                            System.out.println("No tracks selected for deletion.");
                        }
                    });

                    playNextItem.setOnAction(e -> {
                        if (!orderedSelectedTracks.isEmpty()) {
                            int insertPosition = currentTrackIndex + 1;  // Insert immediately after the currently playing track

                            // Add selected tracks in the order they were selected
                            for (Track track : orderedSelectedTracks) {
                                // Insert each selected track right after the currently playing track, preserving duplicates
                                queuedTracks.add(insertPosition, track);
                                insertPosition++;  // Increment the position to maintain order
                                System.out.println("Aggiunto come successivo: " + track.getTitle());
                            }

                            System.out.println("Coda aggiornata con " + queuedTracks.size() + " brani.");

                            // Update the queue table
                            TableView<Track> queueTable = mainView.getTableViewTrackAllInQueue();  // Assuming this is the queue table
                            queueTable.getItems().clear();  // Clear current table
                            queueTable.getItems().addAll(queuedTracks);  // Add updated queue
                            queueTable.refresh();  // Refresh the table
                        }
                    });

                    editTrackData.setOnAction(e -> {
                        // Get the selected tracks from the table
                        List<Track> selectedTracks = tableView.getSelectionModel().getSelectedItems();

                        if (!selectedTracks.isEmpty()) {
                            // Open the metadata editor for the selected tracks
                            TrackMetadataEditor editor = new TrackMetadataEditor(selectedTracks, this.mainView, allViews);
                            editor.show();
                        } else {
                            System.out.println("Nessun brano selezionato.");
                        }
                    });


                    addToPlaylist.setOnAction(e -> {
                        Track selectedTrack = tableView.getSelectionModel().getSelectedItem();
                        if (selectedTrack != null) {
                            // Mostra un popup per selezionare una playlist esistente o crearne una nuova
                            showPlaylistSelectionPopup(selectedTrack);
                        } else {
                            System.out.println("Nessun brano selezionato.");
                        }
                    });


                    // Aggiungi le voci al menu
                    contextMenu.getItems().addAll(addToQueueItem, playNextItem, editTrackData, showInFinder, addToPlaylist, deleteTrack);

                    // Mostra il menu contestuale alla posizione del cursore
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });

            // Aggiungi la logica di trascinamento (drag-and-drop)
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(Integer.toString(row.getIndex())); // Salva l'indice della riga per l'operazione di trascinamento
                    dragboard.setContent(content);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            row.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                boolean success = false;

                if (dragboard.hasString()) {
                    int draggedIndex = Integer.parseInt(dragboard.getString());
                    Track draggedTrack = tableView.getItems().remove(draggedIndex);

                    int dropIndex;

                    if (row.isEmpty()) {
                        dropIndex = tableView.getItems().size();
                    } else {
                        dropIndex = row.getIndex();
                    }

                    tableView.getItems().add(dropIndex, draggedTrack);
                    success = true;

                    // Aggiorna la tabella
                    tableView.getSelectionModel().clearSelection();
                    tableView.refresh();
                }
                event.setDropCompleted(success);
                event.consume();
            });

            // Listener che consuma l'evento di drag
            row.setOnDragDone(DragEvent::consume);

            return row;
        });


    }


    private void showPlaylistSelectionPopup(Track selectedTrack) {
        // Crea un dialogo di scelta
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("Aggiungi alla Playlist");
        dialog.setHeaderText("Seleziona una playlist o creane una nuova:");
        dialog.setContentText("Playlist:");

        // Ottieni tutte le playlist dalla libreria musicale
        List<String> playlistNames = new ArrayList<>(mainView.getMusicLibrary().getAllPlaylists().stream().map(Playlist::getName).toList());
        playlistNames.add("Crea nuova playlist...");
        dialog.getItems().addAll(playlistNames);

        // Mostra il dialogo e attendi la selezione dell'utente
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(playlistName -> {
            if ("Crea nuova playlist...".equals(playlistName)) {
                // Richiedi il nome della nuova playlist
                TextInputDialog newPlaylistDialog = new TextInputDialog();
                newPlaylistDialog.setTitle("Nuova Playlist");
                newPlaylistDialog.setHeaderText("Crea una nuova playlist");
                newPlaylistDialog.setContentText("Nome playlist:");

                Optional<String> newPlaylistName = newPlaylistDialog.showAndWait();
                newPlaylistName.ifPresent(name -> {
                    // Crea la nuova playlist e aggiungi il brano
                    Playlist newPlaylist = new Playlist(name);
                    newPlaylist.addTrack(selectedTrack);
                    mainView.getMusicLibrary().addPlaylist(newPlaylist);
                    System.out.println("Nuova playlist creata: " + name + " con brano aggiunto.");
                });
            } else {
                // Aggiungi il brano alla playlist esistente
                Playlist selectedPlaylist = mainView.getMusicLibrary().getPlaylistByName(playlistName);
                if (selectedPlaylist != null) {
                    selectedPlaylist.addTrack(selectedTrack);
                    System.out.println("Brano aggiunto alla playlist: " + playlistName);
                }
            }
        });
    }


    private void addSelectionListener(TableView<Track> tableView) {
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);  // Permetti selezioni multiple
        ObservableList<Track> selectedTracks = tableView.getSelectionModel().getSelectedItems();

        // Listener per gestire la selezione multipla e mantenere l'ordine di selezione
        selectedTracks.addListener((Observable obs) -> {
            for (Track track : selectedTracks) {
                if (!orderedSelectedTracks.contains(track)) {
                    // Aggiungi il brano selezionato all'ordine delle selezioni solo se non è già presente
                    orderedSelectedTracks.add(track);
                }
            }

            // Rimuovi i brani che non sono più selezionati
            orderedSelectedTracks.removeIf(track -> !selectedTracks.contains(track));

            // Stampa l'ordine delle selezioni nel terminale
            System.out.println("Ordine di selezione aggiornato:");
            for (Track track : orderedSelectedTracks) {
                System.out.println(track.getTitle());
            }

            // Se non ci sono selezioni, segnala che tutte le righe sono state deselezionate
            if (orderedSelectedTracks.isEmpty()) {
                System.out.println("Tutte le righe deselezionate.");
            }
        });
    }


    // Metodo per riempire la coda con i brani della tabella corrente e aggiornare l'indice
    private void fillQueueWithTableTracks(TableView<Track> tableView) {
        List<Track> currentTracks = new ArrayList<>(tableView.getItems()); // Recupera tutti i brani dalla tabella corrente

        // Ripristina e crea la nuova coda
        queuedTracks.clear();  // Svuota la coda esistente
        queuedTracks.addAll(currentTracks);  // Aggiungi i brani della tabella corrente alla coda

        System.out.println("Coda aggiornata con " + queuedTracks.size() + " brani.");
        System.out.println("Brani nella coda:");
        for (Track track : queuedTracks) {
            System.out.println(track.getTitle());
        }

        // Mantieni l'indice del brano corrente o imposta l'indice corretto se necessario
        if (mainView.getCurrentlyPlayingTrack() != null && queuedTracks.contains(mainView.getCurrentlyPlayingTrack())) {
            currentTrackIndex = queuedTracks.indexOf(mainView.getCurrentlyPlayingTrack());  // Mantieni l'indice del brano corrente se è presente nella coda
        } else {
            currentTrackIndex = 0;  // Se non c'è un brano in riproduzione, avvia la coda dal primo brano
        }
    }

    // Metodo per riprodurre un brano specifico
    private void playSelectedTrack(Track selectedTrack) {
        if (selectedTrack != null) {
            String trackPath = selectedTrack.getFilePath(); // Ottieni il percorso del file del brano
            File trackFile = new File(trackPath);

            if (trackFile.exists()) {
                // Se c'è un media player già in esecuzione, fermalo
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }

                // Crea il Media e il MediaPlayer per il brano selezionato
                Media media = new Media(trackFile.toURI().toString());
                mediaPlayer = new MediaPlayer(media);

                // Imposta il volume
                mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (mediaPlayer != null) {
                        updateProgress(newTime, mediaPlayer.getTotalDuration());
                    }
                });

                // Listener per la fine del brano
                mediaPlayer.setOnEndOfMedia(() -> {
                    if (replayButton.isSelected()) {
                        // If replay is on, replay the current track
                        playSelectedTrack(queuedTracks.get(currentTrackIndex));
                    } else {
                        // Otherwise, play the next track in the queue
                        playNextTrack();
                    }
                });

                // Inizia la riproduzione
                mediaPlayer.play();

                // Cambia il testo del pulsante Play a "Play"
                playButton.setText("Play");

                // Aggiungi il listener per lo slider di progresso
                addProgressSliderListener();

                // Imposta il brano corrente in riproduzione
                mainView.setCurrentlyPlayingTrack(selectedTrack);

                // Aggiorna le informazioni del brano tramite allViews
                String title = selectedTrack.getTitle();
                String artist = selectedTrack.getArtist(); // Aggiungi un metodo getArtist() al tuo modello Track se non c'è già
                String album = selectedTrack.getAlbumName();  // Aggiungi un metodo getAlbum() al tuo modello Track
                allViews.updateNowPlayingInfo(title, artist, album);  // Usa il metodo di istanza allViews

                // Aggiorna entrambe le tabelle per riflettere il brano in riproduzione
                mainView.getTableViewTracks().refresh();
                mainView.getTableViewTrackAll().refresh();
                mainView.getTableViewTrackAllInQueue().refresh();
            } else {
                System.out.println("Il file non esiste: " + trackPath);
            }
        }
    }


    private void playNextTrack() {
        if (replayButton.isSelected()) {
            // If replay is on, restart the current track
            playSelectedTrack(queuedTracks.get(currentTrackIndex));
        } else {
            // Otherwise, move to the next track as usual
            if (currentTrackIndex < queuedTracks.size() - 1) {
                currentTrackIndex++;  // Move to the next track
                playSelectedTrack(queuedTracks.get(currentTrackIndex));  // Play the next track
            } else {
                System.out.println("End of the queue, no next track to play.");
                // Optionally disable the ">>" button if you want to prevent navigation
                // nextButton.setDisable(true);
            }
        }
    }


    // Metodo per aggiungere i listener allo slider di progresso
    private void addProgressSliderListener() {
        // Listener che aggiorna il progresso quando si sposta lo slider manualmente
        progressSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
            if (!isChanging && mediaPlayer != null) {
                // Calcola la nuova posizione in base allo slider
                double progress = progressSlider.getValue() / 100.0;
                Duration newTime = mediaPlayer.getTotalDuration().multiply(progress);
                mediaPlayer.seek(newTime);
            }
        });

        // Listener che gestisce il seek quando l'utente rilascia lo slider
        progressSlider.setOnMouseReleased(event -> {
            if (mediaPlayer != null) {
                double progress = progressSlider.getValue() / 100.0;
                Duration newTime = mediaPlayer.getTotalDuration().multiply(progress);
                mediaPlayer.seek(newTime);
            }
        });

        // Listener che sposta lo slider alla nuova posizione quando l'utente clicca su di esso
        progressSlider.setOnMouseClicked(event -> {
            if (mediaPlayer != null) {
                double progress = event.getX() / progressSlider.getWidth();
                Duration newTime = mediaPlayer.getTotalDuration().multiply(progress);
                mediaPlayer.seek(newTime);
                progressSlider.setValue(progress * 100);
            }
        });

        // Imposta il comportamento dello slider del volume
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newValue.doubleValue() / 100.0);  // Imposta il volume tra 0.0 e 1.0
            }
        });

    }


    public List<Track> getQueuedTracks() {
        return queuedTracks;
    }

    // Metodo per aggiornare lo slider di progresso e il tempo
    private void updateProgress(Duration currentTime, Duration totalTime) {
        if (mediaPlayer != null && totalTime != null && !totalTime.isUnknown()) {
            double progress = currentTime.toMillis() / totalTime.toMillis();
            progressSlider.setValue(progress * 100);

            String currentTimeFormatted = utils.formatDuration(currentTime);
            String totalTimeFormatted = utils.formatDuration(totalTime);
            trackTimeLabel.setText(currentTimeFormatted + " / " + totalTimeFormatted);
        } else {
            // Se il mediaPlayer è nullo, resetta lo slider e l'etichetta del tempo
            progressSlider.setValue(0);
            trackTimeLabel.setText("00:00 / 00:00");
        }
    }


    // Metodo per riprodurre o riprendere il brano
    private void playOrResumeTrack() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            // Riprendi la riproduzione
            mediaPlayer.play();
            playButton.setText("Play");  // Cambia il testo del pulsante
        } else {
            // Riproduci il primo brano della coda se non c'è nulla in riproduzione
            if (!queuedTracks.isEmpty()) {
                currentTrackIndex = (currentTrackIndex == -1) ? 0 : currentTrackIndex;  // Imposta il primo brano se non è stato ancora selezionato
                playSelectedTrack(queuedTracks.get(currentTrackIndex));  // Riproduci il brano
            } else {
                System.out.println("La coda è vuota. Aggiungi brani alla coda.");
            }
        }
    }

    // Metodo per mettere in pausa il brano
    private void pauseTrack() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playButton.setText("Resume");  // Cambia il testo del pulsante
        }
    }

    private void stopTrack() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();  // Ferma il brano corrente
            mediaPlayer.dispose();  // Libera le risorse del MediaPlayer
            mediaPlayer = null;  // Reset del MediaPlayer
        }

        // Reset dello slider di progresso e dell'etichetta del tempo del brano
        progressSlider.setValue(0);
        trackTimeLabel.setText("00:00 / 00:00");

        // Svuota la coda di riproduzione
        queuedTracks.clear();

        // Deseleziona tutte le righe nelle tabelle
        if (mainView.getTableViewTracks() != null) {
            mainView.getTableViewTracks().getSelectionModel().clearSelection();
        }

        if (mainView.getTableViewTrackAll() != null) {
            mainView.getTableViewTrackAll().getSelectionModel().clearSelection();
        }

        if (mainView.getTableViewTrackAllInQueue() != null) {
            mainView.getTableViewTrackAllInQueue().getSelectionModel().clearSelection();
            mainView.getTableViewTrackAllInQueue().getItems().clear();  // Svuota la tabella della coda
        }

        // Resetta l'indice del brano corrente
        currentTrackIndex = -1;
        mainView.setCurrentlyPlayingTrack(null);

        // Reset delle informazioni del brano in AllViews
        allViews.resetNowPlayingInfo();  // Usa il metodo di istanza allViews

        System.out.println("Ripristinato lo stato iniziale: coda svuotata, riproduzione fermata.");
    }


    private void nextTrack() {
        // Check if replayButton is selected
        if (replayButton.isSelected()) {
            // If replay is on, restart the current track
            playSelectedTrack(queuedTracks.get(currentTrackIndex));
        } else {
            // Otherwise, move to the next track as usual
            if (currentTrackIndex < queuedTracks.size() - 1) {
                currentTrackIndex++;
                playSelectedTrack(queuedTracks.get(currentTrackIndex));
            } else {
                System.out.println("End of queue, no next track to play.");
            }
        }
    }


    // Metodo per riprodurre il brano precedente quando si preme il pulsante "Indietro"
    private void previousTrack() {
        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 2) {
            // Se la riproduzione è oltre i 2 secondi, ricomincia il brano corrente
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
        } else if (currentTrackIndex > 0) {
            // Se siamo all'inizio del brano, torna al brano precedente
            currentTrackIndex--;  // Torna al brano precedente
            playSelectedTrack(queuedTracks.get(currentTrackIndex));  // Riproduci il brano precedente
        } else {
            System.out.println("Inizio della coda, nessun brano precedente da riprodurre.");
        }
    }


    public Track getCurrentlyPlayingTrack() {
        return mainView.getCurrentlyPlayingTrack();
    }


}
