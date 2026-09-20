package org.mypulse.model;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
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
    private List<Track> queuedTracks; // coda di ascolto
    private Button nextButton;
    private Button prevButton;
    private int currentTrackIndex = -1; // Indice del brano attualmente in riproduzione
    private TableView<Track> currentTable;
    private List<Track> orderedSelectedTracks; // Lista ordinata di selezioni
    private AllViews allViews;
    private Utils utils;


    public MediaPlayerController(MusicLibrary musicLibrary, MainView mainView, AllViews allViews,
                                  Button searchButton, Button lyricsButton) {
        this.mainView = mainView; // Riferimento alla MainView
        queuedTracks = mainView.getQueueTracks();
        this.orderedSelectedTracks = new ArrayList<>();
        this.allViews = allViews; // Riferimento ad AllViews
        this.utils = new Utils();


        // Padding e spaziatura ridotti rispetto all'originale (10/10), ma non al minimo:
        // la riga sopra i controlli (titolo/artista/album) è un'etichetta cliccabile, le
        // serve un minimo di respiro per restare comoda da premere.
        this.setPadding(new Insets(6, 10, 6, 10));
        this.setSpacing(6);

        // Create the controls layout for the buttons
        HBox controlsLayout = new HBox(10);
        controlsLayout.setAlignment(Pos.CENTER); // Center the buttons horizontally

        // Create media control buttons: icone vettoriali (Polygon/Rectangle) invece di
        // testo/simboli Unicode - niente rischio di "tofu" se un carattere non è coperto
        // dal font attivo (lo stesso problema già affrontato per i lyrics), e un aspetto
        // più simile a un player moderno (Spotify/Apple Music) senza introdurre colori
        // o forme fuori dalla palette del tema.
        replayButton = new ToggleButton("R");
        replayButton.getStyleClass().addAll("replayButton-theme", "transport-button");
        replayButton.setTooltip(new Tooltip("Ripeti brano"));

        prevButton = new Button();
        prevButton.setGraphic(buildSkipIcon(false));
        prevButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        prevButton.getStyleClass().add("transport-button");
        prevButton.setTooltip(new Tooltip("Brano precedente"));

        // Play resta il pulsante primario della barra: un po' più grande e riempito con
        // l'accento del tema, per segnalare l'azione principale
        playButton = new Button();
        playButton.setGraphic(buildTriangleIcon(true));
        playButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        playButton.getStyleClass().add("transport-button-primary");
        Tooltip playTooltip = new Tooltip("Play");
        playButton.setTooltip(playTooltip);

        pauseButton = new Button();
        pauseButton.setGraphic(buildPauseIcon());
        pauseButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        pauseButton.getStyleClass().add("transport-button");
        pauseButton.setTooltip(new Tooltip("Pausa"));

        stopButton = new Button();
        stopButton.setGraphic(buildStopIcon());
        stopButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        stopButton.getStyleClass().add("transport-button");
        stopButton.setTooltip(new Tooltip("Stop"));

        nextButton = new Button();
        nextButton.setGraphic(buildSkipIcon(true));
        nextButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        nextButton.getStyleClass().add("transport-button");
        nextButton.setTooltip(new Tooltip("Brano successivo"));

// Create fixed-width spacers
        Region spacer1 = new Region();
        spacer1.setMinWidth(5);  // Adjust width for space between 'R' and '<<'

        Region spacer2 = new Region();
        spacer2.setMinWidth(5);  // Adjust width for space between 'X' and '>>'

        // Cerca e Lyrics ora sulla stessa riga dei controlli di riproduzione (prima erano
        // su una riga a parte, sopra la barra di avanzamento/volume): due spaziatori
        // elastici li spingono ai due estremi mentre i pulsanti di trasporto restano
        // centrati, per tenere la barra complessiva più snella (una riga in meno).
        Region leftEdgeSpacer = new Region();
        Region rightEdgeSpacer = new Region();
        HBox.setHgrow(leftEdgeSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightEdgeSpacer, Priority.ALWAYS);

// Add buttons and spacers to controls layout
        controlsLayout.getChildren().addAll(
                searchButton,
                leftEdgeSpacer,
                replayButton, spacer1, prevButton, playButton, pauseButton, nextButton, spacer2, stopButton,
                rightEdgeSpacer,
                lyricsButton
        );
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
        progressAndVolumeLayout.setPadding(new Insets(2, 0, 0, 0));
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

        // I listener degli slider vanno registrati una sola volta: prima venivano
        // aggiunti ad ogni cambio brano, accumulandosi e moltiplicando i seek
        addProgressSliderListener();
    }

    // Icone vettoriali per i pulsanti di trasporto, costruite con forme JavaFX invece di
    // caratteri Unicode (▶/⏸/⏭ ecc.): non tutti quei glifi sono coperti dal font attivo
    // (stesso rischio di "tofu" già risolto per i lyrics), mentre una Polygon/Rectangle
    // disegna sempre lo stesso identico pulsante su qualunque sistema. Il colore non è
    // fisso nel codice: la classe CSS (icon-secondary/icon-on-accent) lo lega ai token
    // del tema attivo, così le icone cambiano insieme al resto quando si cambia tema.
    private Node buildTriangleIcon(boolean onAccent) {
        Polygon triangle = new Polygon(0, 0, 0, 14, 12, 7);
        triangle.getStyleClass().add(onAccent ? "icon-on-accent" : "icon-secondary");
        return triangle;
    }

    private Node buildPauseIcon() {
        Rectangle bar1 = new Rectangle(4, 14);
        Rectangle bar2 = new Rectangle(4, 14);
        bar1.getStyleClass().add("icon-secondary");
        bar2.getStyleClass().add("icon-secondary");
        HBox bars = new HBox(4, bar1, bar2);
        bars.setAlignment(Pos.CENTER);
        return bars;
    }

    private Node buildStopIcon() {
        Rectangle square = new Rectangle(12, 12);
        square.setArcWidth(2);
        square.setArcHeight(2);
        square.getStyleClass().add("icon-secondary");
        return square;
    }

    // Il "doppio triangolo" classico di avanti/indietro veloce: due Polygon
    // sovrapposte in un Group invece che affiancate in una HBox, per farle toccare
    // leggermente come nell'icona standard invece di apparire troppo distanziate
    private Node buildSkipIcon(boolean pointingRight) {
        Polygon tri1 = new Polygon();
        Polygon tri2 = new Polygon();
        if (pointingRight) {
            tri1.getPoints().addAll(0.0, 0.0, 0.0, 14.0, 9.0, 7.0);
            tri2.getPoints().addAll(7.0, 0.0, 7.0, 14.0, 16.0, 7.0);
        } else {
            tri1.getPoints().addAll(9.0, 0.0, 9.0, 14.0, 0.0, 7.0);
            tri2.getPoints().addAll(16.0, 0.0, 16.0, 14.0, 7.0, 7.0);
        }
        tri1.getStyleClass().add("icon-secondary");
        tri2.getStyleClass().add("icon-secondary");
        return new Group(tri1, tri2);
    }

    private void setRowFactoryForTableView(TableView<Track> tableView) {
        // Il listener di selezione va registrato una sola volta per tabella: la rowFactory
        // sotto viene invocata da JavaFX per ogni riga renderizzata (scroll, resize, refresh),
        // quindi chiamarlo da dentro duplicava il listener ad ogni ridisegno della tabella
        addSelectionListener(tableView);

        tableView.setRowFactory(tv -> {
            TableRow<Track> row = new TableRow<Track>() {
                @Override
                protected void updateItem(Track item, boolean empty) {
                    super.updateItem(item, empty);
                    // Classe CSS invece di uno stile inline fisso (era un rosso uguale in
                    // ogni tema, rgba(255,0,0,0.41)): uno stile inline vince sempre sulla
                    // selezione, quindi selezionare il brano in riproduzione nascondeva del
                    // tutto l'evidenziazione della selezione (e, dopo la correzione del
                    // contrasto del testo selezionato, ci lasciava anche testo scuro poco
                    // leggibile su quel rosso scuro). Con una classe, ":selected" nel CSS può
                    // avere la precedenza quando la riga è sia in riproduzione che selezionata.
                    getStyleClass().remove("now-playing-row");
                    if (item != null && !empty && item.equals(mainView.getCurrentlyPlayingTrack())) {
                        getStyleClass().add("now-playing-row");
                    }
                }
            };

            // Listener per il doppio click che avvia la riproduzione del brano selezionato
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Track selectedTrack = row.getItem();
                    playSelectedTrack(selectedTrack, "Play"); // Riproduci il brano selezionato
                    fillQueueWithTableTracks(tableView); // Riempie la coda con i brani presenti nella tabella
                }
            });

            // Menu contestuale: usa l'evento dedicato invece di controllare il tasto del
            // mouse dentro setOnMouseClicked, così il click destro seleziona prima la riga
            // (a meno che non faccia già parte di una selezione multipla esistente) e le
            // azioni del menu operano sulla selezione corretta invece che su quella precedente
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty() && tableView != mainView.getTableViewTrackAllInQueue()) {
                    if (!tableView.getSelectionModel().getSelectedItems().contains(row.getItem())) {
                        tableView.getSelectionModel().clearSelection();
                        tableView.getSelectionModel().select(row.getIndex());
                    }

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
                            Utils.logSeparator();
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
                                    // Argomenti passati separatamente (niente concatenazione in una
                                    // stringa di shell): un nome file con `"`, `$( )` o `;` non può
                                    // più iniettare comandi arbitrari
                                    String os = System.getProperty("os.name").toLowerCase();
                                    if (os.contains("win")) {
                                        // Windows
                                        new ProcessBuilder("explorer", "/select,\"" + trackFile.getAbsolutePath() + "\"").start();
                                    } else if (os.contains("mac")) {
                                        // macOS
                                        new ProcessBuilder("open", "-R", trackFile.getAbsolutePath()).start();
                                    } else if (os.contains("nix") || os.contains("nux")) {
                                        // Linux: Use 'xdg-open' to open the directory (file selection is not supported in most Linux file managers)
                                        new ProcessBuilder("xdg-open", trackFile.getParent()).start();
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

                                mainView.autoSaveLibrary();
                            }
                        } else {
                            System.out.println("No tracks selected for deletion.");
                        }
                    });

                    playNextItem.setOnAction(e -> {
                        if (!orderedSelectedTracks.isEmpty()) {
                            Utils.logSeparator();
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
                // Accetta il drop solo se la riga trascinata proviene dalla stessa tabella:
                // un indice preso da un'altra tabella non ha senso su questa lista
                if (event.getGestureSource() instanceof TableRow
                        && ((TableRow<?>) event.getGestureSource()).getTableView() == tableView
                        && event.getGestureSource() != row
                        && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            row.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                boolean success = false;

                if (dragboard.hasString() && event.getGestureSource() instanceof TableRow
                        && ((TableRow<?>) event.getGestureSource()).getTableView() == tableView) {
                    int draggedIndex = Integer.parseInt(dragboard.getString());
                    int dropIndex = row.isEmpty() ? tableView.getItems().size() : row.getIndex();

                    Track draggedTrack = tableView.getItems().remove(draggedIndex);
                    // La rimozione ha già spostato in basso di una posizione tutti gli
                    // indici successivi a quello trascinato: se il bersaglio era dopo,
                    // il suo indice calcolato prima della remove va corretto di conseguenza
                    if (draggedIndex < dropIndex) {
                        dropIndex--;
                    }

                    tableView.getItems().add(dropIndex, draggedTrack);
                    success = true;

                    // Se è la tabella della coda, l'ordine di riproduzione va tenuto
                    // sincronizzato con quello visuale: prima il drag&drop riordinava solo
                    // la tabella, senza alcun effetto sull'ordine di riproduzione reale
                    if (tableView == mainView.getTableViewTrackAllInQueue()) {
                        Track currentlyPlaying = mainView.getCurrentlyPlayingTrack();
                        queuedTracks.clear();
                        queuedTracks.addAll(tableView.getItems());
                        currentTrackIndex = (currentlyPlaying != null) ? queuedTracks.indexOf(currentlyPlaying) : -1;
                    }

                    // Aggiorna la tabella
                    tableView.getSelectionModel().clearSelection();
                    tableView.getSelectionModel().select(dropIndex);
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
            mainView.autoSaveLibrary();
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
        // Non si stampa più qui l'elenco completo della coda: il log di cosa parte
        // (playSelectedTrack, action "Play") è già stato stampato dal doppio click
        // che ha chiamato questo metodo, ed è quello che conta in console

        // Mantieni l'indice del brano corrente o imposta l'indice corretto se necessario
        if (mainView.getCurrentlyPlayingTrack() != null && queuedTracks.contains(mainView.getCurrentlyPlayingTrack())) {
            currentTrackIndex = queuedTracks.indexOf(mainView.getCurrentlyPlayingTrack());  // Mantieni l'indice del brano corrente se è presente nella coda
        } else {
            currentTrackIndex = 0;  // Se non c'è un brano in riproduzione, avvia la coda dal primo brano
        }
    }

    // Metodo per riprodurre un brano specifico
    // "action" descrive perché parte questo brano (Play, Avanti, Indietro, Replay, ...)
    // e viene stampato in console al posto del vecchio dump completo della coda
    private void playSelectedTrack(Track selectedTrack, String action) {
        if (selectedTrack != null) {
            String trackPath = selectedTrack.getFilePath(); // Ottieni il percorso del file del brano
            File trackFile = new File(trackPath);

            if (trackFile.exists()) {
                // Se c'è un media player già in esecuzione, fermalo e liberarne le risorse native
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
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
                        playSelectedTrack(queuedTracks.get(currentTrackIndex), "Replay");
                    } else {
                        // Otherwise, play the next track in the queue
                        playNextTrack();
                    }
                });

                // Gestione errori: file corrotto/non decodificabile non deve bloccare la coda in silenzio
                mediaPlayer.setOnError(() -> {
                    System.out.println("Errore durante la riproduzione di " + trackPath + ": "
                            + mediaPlayer.getError());
                    // Salta sempre al successivo: con playNextTrack() e replay attivo si
                    // ritenterebbe all'infinito lo stesso file rotto
                    advanceToNextTrack();
                });

                // Inizia la riproduzione
                mediaPlayer.play();

                // Il pulsante è icon-only (stessa icona per "avvia" e "riprendi"): la
                // distinzione resta disponibile al passaggio del mouse sul tooltip
                playButton.getTooltip().setText("Play");

                // Imposta il brano corrente in riproduzione
                mainView.setCurrentlyPlayingTrack(selectedTrack);

                Utils.logSeparator();
                System.out.println("[" + action + "] " + selectedTrack.getTitle() + " — " + selectedTrack.getArtist());

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


    // Chiamato alla fine naturale di un brano (fine coda esclusa): qui il repeat-one va rispettato
    private void playNextTrack() {
        if (replayButton.isSelected()) {
            playSelectedTrack(queuedTracks.get(currentTrackIndex), "Replay");
        } else {
            advanceToNextTrack();
        }
    }

    // Avanza sempre al brano successivo in coda, indipendentemente dal repeat-one:
    // usato sia dal salto manuale (">>") sia per saltare un brano che non è riproducibile
    private void advanceToNextTrack() {
        if (currentTrackIndex < queuedTracks.size() - 1) {
            currentTrackIndex++;
            playSelectedTrack(queuedTracks.get(currentTrackIndex), "Avanti");
        } else {
            Utils.logSeparator();
            System.out.println("Fine della coda, nessun brano successivo da riprodurre.");
            // Optionally disable the ">>" button if you want to prevent navigation
            // nextButton.setDisable(true);
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


    // Descrive il brano attualmente in riproduzione per i log di Pausa/Resume/Indietro,
    // dove non viene creato un nuovo MediaPlayer e quindi playSelectedTrack() non passa
    private String describeCurrentTrack() {
        Track current = mainView.getCurrentlyPlayingTrack();
        return current != null ? current.getTitle() + " — " + current.getArtist() : "";
    }

    // Metodo per riprodurre o riprendere il brano
    private void playOrResumeTrack() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            // Riprendi la riproduzione
            mediaPlayer.play();
            playButton.getTooltip().setText("Play");

            Utils.logSeparator();
            System.out.println("[Resume] " + describeCurrentTrack());
        } else {
            // Riproduci il primo brano della coda se non c'è nulla in riproduzione
            if (!queuedTracks.isEmpty()) {
                currentTrackIndex = (currentTrackIndex == -1) ? 0 : currentTrackIndex;  // Imposta il primo brano se non è stato ancora selezionato
                playSelectedTrack(queuedTracks.get(currentTrackIndex), "Play");  // Riproduci il brano
            } else {
                Utils.logSeparator();
                System.out.println("La coda è vuota. Aggiungi brani alla coda.");
            }
        }
    }

    // Metodo per mettere in pausa il brano
    private void pauseTrack() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playButton.getTooltip().setText("Resume");

            Utils.logSeparator();
            System.out.println("[Pausa] " + describeCurrentTrack());
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


    // Pulsante ">>": un salto manuale deve sempre avanzare, il repeat-one riguarda
    // solo cosa succede alla fine naturale di un brano, non la navigazione esplicita
    private void nextTrack() {
        advanceToNextTrack();
    }


    // Metodo per riprodurre il brano precedente quando si preme il pulsante "Indietro"
    private void previousTrack() {
        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 2) {
            // Se la riproduzione è oltre i 2 secondi, ricomincia il brano corrente
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();

            Utils.logSeparator();
            System.out.println("[Indietro] Riavvio: " + describeCurrentTrack());
        } else if (currentTrackIndex > 0) {
            // Se siamo all'inizio del brano, torna al brano precedente
            currentTrackIndex--;  // Torna al brano precedente
            playSelectedTrack(queuedTracks.get(currentTrackIndex), "Indietro");  // Riproduci il brano precedente
        } else {
            Utils.logSeparator();
            System.out.println("Inizio della coda, nessun brano precedente da riprodurre.");
        }
    }


    public Track getCurrentlyPlayingTrack() {
        return mainView.getCurrentlyPlayingTrack();
    }

    // Da chiamare alla chiusura dell'applicazione (Application.stop() in MainView) per
    // liberare le risorse native del MediaPlayer: prima non veniva mai rilasciato in
    // nessun caso alla chiusura, solo quando si premeva esplicitamente "Stop"
    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

}
