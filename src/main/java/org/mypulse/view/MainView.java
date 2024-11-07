package org.mypulse.view;

import javafx.application.Application;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.mypulse.controller.LyricsController;
import org.mypulse.controller.MusicController;
import org.mypulse.model.*;
import org.mypulse.util.SerializationUtils;
import org.mypulse.util.Utils;
import org.mypulse.view.components.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class MainView extends Application {

    private MusicLibrary musicLibrary;  // Libreria musicale
    private MusicScanner musicScanner;  // Scanner musicale
    public ListView<String> listViewMenu;
    public ListView<String> listViewArtist;
    public ListView<Album> listViewAlbum;
    public ListView<Album> listViewAlbumAll;
    private ListView<Playlist> listViewPlaylist; // Component for displaying playlists
    private TableView<Track> tableViewTrackAll; // Seconda tabella per tutti i brani
    public TableView<Track> tableViewTracks;  // Tabella per i brani
    public TableView<Track> tableViewTrackAllInQueue;
    private ImageView albumCoverView;  // Campo per mostrare la copertina dell'album
    private Image defaultImage;  // Campo per l'immagine di default
    private VBox imageAndTableContainer;  // Contenitore per immagine e tabella
    private FilteredList<Track> filteredTracks;  // Lista filtrata dei brani
    private List<Track> queuedTracks; // coda di ascolto
    private Track currentlyPlayingTrack; // Brano attualmente in riproduzione
    private AllViews allViews;
    private ArtistListView artistListView;
    private AlbumListView albumListView;
    private MusicController musicController;
    private TrackTableView trackTableView;
    private ImageCoverView imageCoverView;
    private LyricsController lyricsController;
    private Button searchButton;
    private SearchFrame searchFrame;
    private String currentTableMode;
    // Constants to represent the modes
    private static final String MODE_ALBUM = "Album";
    private static final String MODE_PLAYLIST = "Playlist";

    private Label titleLabel;
    private Label artistLabel;
    private Label albumLabel;


    // Nome predefinito per il file serializzato
    private static final String DEFAULT_SAVE_FILE = "music_library.ser";

    // Directory predefinita per il file
    private static final String SAVE_DIRECTORY = System.getProperty("user.dir"); // Directory corrente del programma


    private Button lyricsButton;


    @Override
    public void start(Stage primaryStage) {
        // Inizializza la libreria e lo scanner musicale
        musicLibrary = new MusicLibrary();
        Utils utils = new Utils();
        queuedTracks = new ArrayList<>();
        musicScanner = new MusicScanner(musicLibrary);
        lyricsController = new LyricsController(this);
        searchFrame = new SearchFrame(musicLibrary, this);
        currentTableMode = "";


// Controlla se il file predefinito esiste
        File defaultFile = new File(SAVE_DIRECTORY, DEFAULT_SAVE_FILE);
        if (defaultFile.exists()) {
            // Prova a caricare la libreria musicale serializzata
            try {
                Object deserializedObject = SerializationUtils.deserialize(defaultFile.getAbsolutePath());

                // Verifica se l'oggetto deserializzato è effettivamente un'istanza di MusicLibrary
                if (deserializedObject instanceof MusicLibrary) {
                    MusicLibrary loadedLibrary = (MusicLibrary) deserializedObject;

                    // Verifica l'integrità dei dati caricati (es. non null, strutture di dati valide)
                    if (utils.isValidMusicLibrary(loadedLibrary)) {
                        musicLibrary.copyFrom(loadedLibrary); // Copia i dati dalla libreria caricata
                        System.out.println("Libreria musicale caricata con successo dal file predefinito: " + defaultFile.getAbsolutePath());
                    } else {
                        System.out.println("Il file caricato è corrotto o incompleto, caricamento ignorato.");
                        utils.showAlert("Errore", "Il file della libreria musicale è corrotto o incompleto. Avvio con una libreria vuota.");
                    }
                } else {
                    System.out.println("Il file deserializzato non è una libreria musicale valida.");
                    utils.showAlert("Errore", "Il file caricato non contiene una libreria musicale valida.");
                }
            } catch (Exception e) {
                System.out.println("Errore durante il caricamento della libreria musicale: " + e.getMessage());
                utils.showAlert("Errore", "Si è verificato un problema durante il caricamento della libreria musicale. Avvio con una libreria vuota.");
            }
        } else {
            System.out.println("Nessun file predefinito trovato, avvio con una libreria vuota.");
        }

        allViews = new AllViews(this);  // Istanzia AllViews
        artistListView = new ArtistListView(this, this.musicLibrary);
        albumListView = new AlbumListView(this);
        musicController = new MusicController(musicScanner, artistListView);
        imageCoverView = new ImageCoverView(this);
        trackTableView = new TrackTableView(this);
        lyricsController = new LyricsController(this);

// Creare la funzione di scansione
        Runnable scanAction = () -> musicController.scanMusicFolder();

// Creare il menu e passare l'azione di scansione, la libreria musicale, MainView, e AllViews
        AppMenu appMenu = new AppMenu(scanAction, musicLibrary, this, allViews);
        MenuBar menuBar = appMenu.createMenuBar(primaryStage);
        menuBar.setStyle("-fx-background-color: #333333; -fx-text-fill: white;");


        try {
            String defaultImagePath = getClass().getResource("/undefinedAlbum.jpg").toExternalForm();
            defaultImage = new Image(defaultImagePath, 300, 300, true, false);  // Imposta la larghezza e altezza dell'immagine
            System.out.println("Immagine di default caricata correttamente: " + defaultImagePath);
        } catch (Exception e) {
            System.out.println("Errore nel caricamento dell'immagine di default: " + e.getMessage());
        }

        // Layout grid
        GridPane gridPane = new GridPane();
        // Aggiungi queste righe per impostare larghezze proporzionali per le colonne
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        ColumnConstraints col3 = new ColumnConstraints();
        ColumnConstraints col4 = new ColumnConstraints();
        col1.setPercentWidth(10); // Regola la percentuale in base alle tue necessità
        col2.setPercentWidth(15);
        col3.setPercentWidth(25);
        col4.setPercentWidth(50);

        gridPane.getColumnConstraints().addAll(col1, col2, col3, col4);

        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10));

        // Primo ListView (menu principale)
        listViewMenu = new ListView<>();
        listViewMenu.getItems().addAll("Artisti", "Album", "Brani", "Playlist", "Coda");
        listViewMenu.setPrefWidth(150);
        GridPane.setVgrow(listViewMenu, Priority.ALWAYS);
        gridPane.add(listViewMenu, 0, 0);

        // Secondo ListView (artisti)
        listViewArtist = new ListView<>();
        listViewArtist.setPrefWidth(250);
        GridPane.setVgrow(listViewArtist, Priority.ALWAYS);

        // Terzo ListView (album)
        listViewAlbum = new ListView<>();
        listViewAlbum.setPrefWidth(390);
        GridPane.setVgrow(listViewAlbum, Priority.ALWAYS);

        // Quarto ListView (album grande)
        listViewAlbumAll = new ListView<>();
        listViewAlbumAll.setPrefWidth(640);  // Larghezza doppia per occupare due colonne
        GridPane.setVgrow(listViewAlbumAll, Priority.ALWAYS);

        listViewPlaylist = new ListView<>();
        listViewPlaylist.setPrefWidth(640);
        GridPane.setVgrow(listViewPlaylist, Priority.ALWAYS);


// Aggiungere la tabella dal TrackTableFactory
        tableViewTracks = TrackTableFactory.createTrackTable();
        gridPane.add(tableViewTracks, 1, 0, 3, 1); // Aggiungi la tabella in modo che occupi 3 colonne


        tableViewTrackAll = TrackTableFactoryExtended.createExtendedTrackTableAll();
        tableViewTrackAll.setVisible(true); // Rendi la tabella visibile

        tableViewTrackAllInQueue = TrackTableFactoryExtended.createExtendedTrackTableAll();
        tableViewTrackAllInQueue.setVisible(true);


// Crea l'ImageView per la copertina dell'album
        albumCoverView = new ImageView();
        albumCoverView.setFitWidth(300);  // Imposta una larghezza fissa
        albumCoverView.setFitHeight(300);  // Imposta un'altezza fissa, uguale alla larghezza per ottenere un quadrato
        albumCoverView.setPreserveRatio(false);  // Disattiva il mantenimento del rapporto d'aspetto
        albumCoverView.setSmooth(true);  // Rendering più pulito
        albumCoverView.setCache(true);  // Migliora la performance del rendering

        imageAndTableContainer = new VBox(10);  // Spaziatura di 10 tra immagine e tabella
        imageAndTableContainer.setAlignment(Pos.CENTER);  // Centra l'immagine
        imageAndTableContainer.getChildren().add(albumCoverView);

        // Aggiungi la VBox al layout nella colonna desiderata (es. colonna 3)
        gridPane.add(imageAndTableContainer, 3, 0);
        GridPane.setVgrow(imageAndTableContainer, Priority.ALWAYS);  // Assicura che la VBox cresca verticalmente
        GridPane.setHalignment(imageAndTableContainer, HPos.CENTER);  // Centra la VBox orizzontalmente


        // Creazione del BorderPane come root principale
        BorderPane rootPane = new BorderPane();

        // Imposta il GridPane al centro del BorderPane
        rootPane.setCenter(gridPane);
        BorderPane.setAlignment(gridPane, Pos.CENTER);

        // Aggiungere il menuBar in alto
        rootPane.setTop(menuBar);


// Creazione dell'HBox per le informazioni del brano
        HBox infoBar = new HBox(10);  // Spaziatura di 10 tra gli elementi
        infoBar.setAlignment(Pos.CENTER);  // Allinea tutto al centro
        infoBar.setPadding(new Insets(5, 0, 5, 0));  // Aggiungi padding superiore (10) e inferiore (20)

        infoBar.setStyle("-fx-background-color: #444444; -fx-text-fill: white;");  // Stile per sfondo e testo
        searchButton = new Button("Cerca");
        searchButton.setMinWidth(60); // Set a minimum width for the button if necessary

        lyricsButton = new Button("Lyrics");
        lyricsButton.setMinWidth(60); // Imposta una larghezza minima per il pulsante se necessario


// Etichette per titolo, artista e album
        titleLabel = new Label("");
        artistLabel = new Label("");
        albumLabel = new Label("");

        titleLabel.getStyleClass().add("clickable-label");


// Crea un pulsante "lyrics" e impostalo fisso all'estrema sinistra

// Crea due spaziatori (uno prima e uno dopo le etichette)
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();

// Imposta l'HBox di espandersi per centrare le etichette
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

// Imposta lo stile per le etichette (le info secondarie come artista e album saranno meno prominenti)
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
        artistLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dad9d9;");
        albumLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dad9d9;");

// Imposta le etichette per espandersi e rimanere centrate
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        artistLabel.setMaxWidth(Double.MAX_VALUE);
        albumLabel.setMaxWidth(Double.MAX_VALUE);

// Imposta l'allineamento del testo al centro per ogni etichetta
        titleLabel.setAlignment(Pos.CENTER);
        artistLabel.setAlignment(Pos.CENTER);
        albumLabel.setAlignment(Pos.CENTER);

// Aggiungi il pulsante, gli spaziatori e le etichette all'HBox
        infoBar.getChildren().addAll(searchButton, leftSpacer, titleLabel, artistLabel, albumLabel, rightSpacer, lyricsButton);

// Aggiungi l'infoBar subito sotto il MenuBar
        VBox topContainer = new VBox(menuBar, infoBar);  // Aggiungi l'infoBar e il menuBar nello stesso contenitore verticale
        rootPane.setTop(topContainer);  // Imposta questo contenitore come la parte superiore del BorderPane


        // Crea il MediaPlayerController
// Crea il MediaPlayerController
        // Media player at the bottom
        MediaPlayerController mediaPlayerControl = new MediaPlayerController(musicLibrary, this, allViews);
        mediaPlayerControl.setId("bottom"); // Assegna un ID al MediaPlayerController

        // Aggiungi il media player in basso al BorderPane
        rootPane.setBottom(mediaPlayerControl);

        // Imposta il media player a occuparsi tutto lo spazio orizzontale disponibile
        BorderPane.setAlignment(mediaPlayerControl, Pos.CENTER);
        BorderPane.setMargin(mediaPlayerControl, new Insets(10));


        // Aggiungi questa parte all'interno del metodo start() o dove crei albumCoverView

// Create the context menu for the album cover
        ContextMenu coverContextMenu = new ContextMenu();

        MenuItem changeCoverMenuItem = new MenuItem("Modifica copertina album");
        MenuItem deleteAlbumMenuItem = new MenuItem("Elimina album");
        MenuItem editAlbumMetadata = new MenuItem("Mostra informazioni album");

        // Menu items for playlist
        MenuItem editPlaylistMenuItem = new MenuItem("Modifica playlist");
        MenuItem deletePlaylistMenuItem = new MenuItem("Elimina playlist");


        changeCoverMenuItem.setOnAction(event -> {
            // Check if the table has items
            if (!getTableViewTracks().getItems().isEmpty()) {
                // Get the first track in the table
                Track firstTrack = getTableViewTracks().getItems().get(0);

                // Retrieve the album name from the first track
                String albumName = firstTrack.getAlbumName();

                // Get the album from the music library using the album name
                Album selectedAlbum = getMusicLibrary().getAlbumByName(albumName);

                if (selectedAlbum != null) {
                    System.out.println("Modifica copertina album per: " + selectedAlbum.getName());
                    // Call the method to edit the album cover
                    imageCoverView.editAlbumCover(selectedAlbum);
                } else {
                    System.out.println("Album non trovato.");
                }
            } else {
                System.out.println("La tabella dei brani è vuota.");
            }
        });

// Add action to the "Elimina album" menu item
        deleteAlbumMenuItem.setOnAction(event -> {
            Album selectedAlbum;

            // Check if the table has items
            if (!getTableViewTracks().getItems().isEmpty()) {
                // Get the first track in the table
                Track firstTrack = getTableViewTracks().getItems().get(0);

                // Retrieve the album name from the first track
                String albumName = firstTrack.getAlbumName();

                // Get the album from the music library using the album name
                selectedAlbum = getMusicLibrary().getAlbumByName(albumName);
            } else {
                selectedAlbum = null;
            }

            if (selectedAlbum != null) {
                // Prompt the user for confirmation
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Conferma eliminazione");
                alert.setHeaderText("Sei sicuro di voler eliminare l'album \"" + selectedAlbum.getName() + "\"?");
                alert.setContentText("Questa azione è irreversibile. Vuoi anche eliminare i file effettivi?");

                ButtonType deleteFilesButton = new ButtonType("Elimina anche i file effettivi");
                ButtonType deleteOnlyButton = new ButtonType("Elimina l'album dalla libreria");
                ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(deleteFilesButton, deleteOnlyButton, cancelButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == deleteFilesButton || response == deleteOnlyButton) {
                        // Remove the album from the music library
                        getMusicLibrary().removeAlbum(selectedAlbum);

                        // Remove the album from the list view
                        listViewAlbum.getItems().remove(selectedAlbum);

                        if (response == deleteFilesButton) {
                            // Delete the files associated with the album
                            for (Track track : selectedAlbum.getTracks()) {
                                File trackFile = new File(track.getFilePath());
                                if (trackFile.exists()) {
                                    boolean deleted = trackFile.delete();
                                    if (deleted) {
                                        System.out.println("File eliminato: " + trackFile.getAbsolutePath());
                                    } else {
                                        System.out.println("Impossibile eliminare il file: " + trackFile.getAbsolutePath());
                                    }
                                }
                            }
                        }

                        // Refresh the UI components
                        allViews.refreshAllViews();
                        listViewMenu.getSelectionModel().select("Artisti");
                        listViewMenu.getSelectionModel().select("Album");
                        getTableViewTracks().getItems().clear();
                        getAlbumCoverView().setImage(getDefaultImage()); // Reset to default image

                        System.out.println("Album \"" + selectedAlbum.getName() + "\" eliminato.");
                    }
                });
            } else {
                System.out.println("Nessun album selezionato.");
            }
        });


        editAlbumMetadata.setOnAction(event -> {
            // Recupera tutti i brani presenti nella tabella
            List<Track> selectedTracks = getTableViewTracks().getItems();

            if (!selectedTracks.isEmpty()) {
                // Crea un'istanza di TrackMetadataEditor con tutti i brani presenti nella tabella
                TrackMetadataEditor editor = new TrackMetadataEditor(selectedTracks, this, allViews);
                editor.show();
            } else {
                System.out.println("Nessun brano presente nella tabella.");
            }
        });




// Action for "Modifica playlist"
        editPlaylistMenuItem.setOnAction(event -> {
            // Recupera la playlist selezionata dalla lista delle playlist
            Playlist selectedPlaylist = listViewPlaylist.getSelectionModel().getSelectedItem();

            if (selectedPlaylist != null) {
                // Crea un'istanza dell'editor della playlist e mostra la finestra
                PlaylistEditor editor = new PlaylistEditor(selectedPlaylist, this); // Passa 'this' come riferimento a MainView
                editor.show(); // Mostra l'editor della playlist
            } else {
                System.out.println("Nessuna playlist selezionata.");
            }
        });

// Action for "Elimina playlist"
        deletePlaylistMenuItem.setOnAction(event -> {
            Playlist selectedPlaylist = listViewPlaylist.getSelectionModel().getSelectedItem();
            if (selectedPlaylist != null) {
                // Prompt the user for confirmation
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Conferma eliminazione");
                alert.setHeaderText("Sei sicuro di voler eliminare la playlist \"" + selectedPlaylist.getName() + "\"?");
                alert.setContentText("Questa azione è irreversibile.");

                ButtonType deleteButton = new ButtonType("Elimina");
                ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(deleteButton, cancelButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == deleteButton) {
                        // Rimuove la playlist dalla libreria musicale
                        getMusicLibrary().removePlaylist(selectedPlaylist);

                        // Rimuove la playlist dalla vista delle playlist
                        listViewPlaylist.getItems().remove(selectedPlaylist);

                        // Rinfresca la vista delle playlist
                        listViewPlaylist.refresh();

                        System.out.println("Playlist \"" + selectedPlaylist.getName() + "\" eliminata.");
                    }
                });
            } else {
                System.out.println("Nessuna playlist selezionata.");
            }
        });


// Add the context menu event listener
        albumCoverView.setOnContextMenuRequested(event -> {
            // Clear previous items
            coverContextMenu.getItems().clear();

            // Add items based on the current table mode
            if (MODE_ALBUM.equals(currentTableMode)) {
                coverContextMenu.getItems().addAll(changeCoverMenuItem, editAlbumMetadata, deleteAlbumMenuItem);
            } else if (MODE_PLAYLIST.equals(currentTableMode)) {
                coverContextMenu.getItems().addAll(editPlaylistMenuItem, deletePlaylistMenuItem);
            }
            // Show the context menu
            coverContextMenu.show(albumCoverView, event.getScreenX(), event.getScreenY());
        });


        listViewMenu.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Rimuovi tutte le viste attuali prima di aggiungere quella nuova
            gridPane.getChildren().removeAll(listViewArtist, listViewAlbum, listViewAlbumAll, tableViewTrackAll, albumCoverView, tableViewTrackAllInQueue, listViewPlaylist); // aggiungi listViewPlaylist se non è incluso

            if ("Artisti".equals(newValue)) {
                listViewArtist.getItems().clear();
                listViewAlbum.getItems().clear();
                if (!gridPane.getChildren().contains(listViewArtist)) {
                    gridPane.add(listViewArtist, 1, 0);
                }
                if (!gridPane.getChildren().contains(listViewAlbum)) {
                    gridPane.add(listViewAlbum, 2, 0);
                }
                artistListView.populateArtists(); // Popola la lista degli artisti

            } else if ("Album".equals(newValue)) {
                listViewArtist.getItems().clear();
                listViewAlbum.getItems().clear();
                if (!gridPane.getChildren().contains(listViewAlbumAll)) {
                    gridPane.add(listViewAlbumAll, 1, 0, 2, 1); // Aggiungi la vista album
                }
                albumListView.populateAlbums(); // Popola la lista degli album

            } else if ("Brani".equals(newValue)) {
                if (!gridPane.getChildren().contains(tableViewTrackAll)) {
                    gridPane.add(tableViewTrackAll, 1, 0, 3, 1);
                }
                trackTableView.populateExtendedTrackTable(); // Usa il nuovo metodo per popolare la tabella estesa
            } else if ("Coda".equals(newValue)) {
                if (!gridPane.getChildren().contains(tableViewTrackAllInQueue)) {
                    gridPane.add(tableViewTrackAllInQueue, 1, 0, 3, 1); // Aggiungi la tabella della coda di riproduzione
                }
                trackTableView.populateAllTracksInQueue(); // Popola la tabella con i brani in coda

            } else if ("Playlist".equals(newValue)) {
                if (!gridPane.getChildren().contains(listViewPlaylist)) {
                    gridPane.add(listViewPlaylist, 1, 0); // Aggiungi la vista della playlist
                }
                populatePlaylists();
            }
        });


        listViewArtist.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                albumListView.populateAlbumsByArtist(newValue);
            }
        });

        listViewAlbum.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String albumName = newValue.getName();
                trackTableView.populateTracksByAlbum(albumName);

                // Check if there are tracks in the album and set the cover image to the first track's cover
                if (!tableViewTracks.getItems().isEmpty()) {
                    Track firstTrack = tableViewTracks.getItems().get(0);
                    byte[] coverImage = firstTrack.getCoverImage();
                    if (coverImage != null) {
                        albumCoverView.setImage(new Image(new ByteArrayInputStream(coverImage), 300, 300, true, false));
                    } else {
                        albumCoverView.setImage(defaultImage); // Use default image if no cover is available
                    }
                } else {
                    // If the album has no tracks, fall back to the album's cover image
                    String imagePath = newValue.getCoverImagePath();
                    if (imagePath != null && !imagePath.isEmpty()) {
                        Image albumCover = new Image("file:" + imagePath, 300, 300, true, false);
                        albumCoverView.setImage(albumCover);
                    } else {
                        albumCoverView.setImage(defaultImage); // Use default image if no cover is available
                    }
                }

                // Show the table and add it to the container only if it is not already present
                if (!imageAndTableContainer.getChildren().contains(tableViewTracks)) {
                    imageAndTableContainer.getChildren().add(tableViewTracks);
                }
                tableViewTracks.setVisible(true); // Show the table
            }
        });

        listViewPlaylist.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String playlistName = newValue.getName();
                trackTableView.populateTracksByPlaylist(playlistName);
                if (!tableViewTracks.getItems().isEmpty()) {

                    albumCoverView.setImage(defaultImage); // Use default image if no cover is available

                }
                // Mostra la tabella e aggiungila al contenitore solo se non è già presente
                if (!imageAndTableContainer.getChildren().contains(tableViewTracks)) {
                    imageAndTableContainer.getChildren().add(tableViewTracks);
                }
                tableViewTracks.setVisible(true);  // Mostra la tabella


            }

        });


        listViewAlbumAll.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String albumName = newValue.getName();
                trackTableView.populateTracksByAlbum(albumName);

                // Mostra la copertina dell'album selezionato sopra la tabella
                String imagePath = newValue.getCoverImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    Image albumCover = new Image("file:" + imagePath, 300, 300, true, false);
                    albumCoverView.setImage(albumCover);
                } else {
                    albumCoverView.setImage(defaultImage);
                }

                // Mostra la tabella e aggiungila al contenitore solo se non è già presente
                if (!imageAndTableContainer.getChildren().contains(tableViewTracks)) {
                    imageAndTableContainer.getChildren().add(tableViewTracks);
                }
                tableViewTracks.setVisible(true);  // Mostra la tabella
            }
        });


        lyricsButton.setOnAction(event -> lyricsController.showLyrics());
        searchButton.setOnAction(event -> openSearchFrame(musicLibrary, this));  // Passa la libreria musicale

        titleLabel.setOnMouseClicked(event -> {
        });
        //handleArtistClick();  // <--- Questo collega l'azione al click su un elemento della lista artisti
        //handleAlbumClick();
        handleTrackClick();


        // Create the context menu for the playlist
        ContextMenu playlistContextMenu = new ContextMenu();
        MenuItem deletePlaylistMenuItem2 = new MenuItem("Elimina Playlist");

// Set the action for deleting the playlist
        deletePlaylistMenuItem2.setOnAction(event -> {
            Playlist selectedPlaylist = listViewPlaylist.getSelectionModel().getSelectedItem();
            if (selectedPlaylist != null) {
                // Prompt the user for confirmation
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Conferma eliminazione");
                alert.setHeaderText("Sei sicuro di voler eliminare la playlist \"" + selectedPlaylist.getName() + "\"?");
                alert.setContentText("Questa azione è irreversibile.");

                ButtonType deleteButton = new ButtonType("Elimina");
                ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(deleteButton, cancelButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == deleteButton) {
                        // Remove the playlist from the music library
                        getMusicLibrary().removePlaylist(selectedPlaylist);

                        // Remove the playlist from the list view
                        listViewPlaylist.getItems().remove(selectedPlaylist);

                        // Refresh the ListView
                        listViewPlaylist.refresh();

                        System.out.println("Playlist \"" + selectedPlaylist.getName() + "\" eliminata.");
                    }
                });
            } else {
                System.out.println("Nessuna playlist selezionata.");
            }
        });

// Add the menu item to the context menu
        playlistContextMenu.getItems().add(deletePlaylistMenuItem2);

// Add the context menu to the listViewPlaylist
        listViewPlaylist.setOnContextMenuRequested(event -> {
            // Show the context menu at the mouse's position
            playlistContextMenu.show(listViewPlaylist, event.getScreenX(), event.getScreenY());
        });


        // Creazione della scena
        Scene scene = new Scene(rootPane, 1400, 800);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Pulse");
        primaryStage.show();
    }

    // Method to populate the playlist ListView
    private void populatePlaylists() {
        // Clear the current items in the playlist view
        listViewPlaylist.getItems().clear();

        // Retrieve playlists from the music library
        List<Playlist> playlists = musicLibrary.getPlaylists();

        // Add playlists to the ListView
        listViewPlaylist.getItems().addAll(playlists);

        // Refresh the ListView to ensure changes are visible
        listViewPlaylist.refresh();
    }


    public AllViews getAllViews() {
        return allViews;
    }

    public ListView<String> getListViewArtist() {
        return listViewArtist;
    }

    public ListView<String> getListViewMenu() {
        return listViewMenu;
    }

    public ListView<Album> getListViewAlbumAll() {
        return listViewAlbumAll;
    }

    public ListView<Album> getListViewAlbum() {
        return listViewAlbum;
    }

    public TableView<Track> getListViewTrack() {
        return tableViewTracks;
    }

    public MusicLibrary getMusicLibrary() {
        return musicLibrary;
    }

    public TableView<Track> getTableViewTracks() {
        return tableViewTracks;
    }

    public TableView<Track> getTableViewTrackAll() {
        return tableViewTrackAll;
    }

    public TableView<Track> getTableViewTrackAllInQueue() {
        return tableViewTrackAllInQueue;
    }

    public List<Track> getQueueTracks() {
        return queuedTracks;
    }

    public Track getCurrentlyPlayingTrack() {
        return currentlyPlayingTrack;
    }

    public void setCurrentlyPlayingTrack(Track track) {
        currentlyPlayingTrack = track;
    }

    public Label getTitleLabel() {
        return this.titleLabel;
    }

    public Label getArtistLabel() {
        return this.artistLabel;
    }

    public Label getAlbumLabel() {
        return this.albumLabel;
    }

    public ImageView getAlbumCoverView() {
        return albumCoverView;
    }

    public Image getDefaultImage() {
        return defaultImage;
    }

    public ArtistListView getArtistListView() {
        return artistListView;
    }

    public AlbumListView getAlbumListView() {
        return albumListView;
    }

    public TrackTableView getTrackTableView() {
        return trackTableView;
    }


    // Metodo per gestire il click su un brano
    private void handleTrackClick() {
        titleLabel.setOnMouseClicked(event -> {
            Track selectedTrack = musicLibrary.getTrackByDetails(titleLabel.getText(), artistLabel.getText(), albumLabel.getText());
            if (selectedTrack != null) {
                // Recupera l'artista e l'album associati al brano selezionato
                String selectedArtist = selectedTrack.getArtistAlbum();
                String selectedAlbum = selectedTrack.getAlbumName();
                System.out.println("Artista, Album e Brano recuperati: " + selectedArtist + " - " + selectedAlbum + " - " + selectedTrack.getTitle());

                // Seleziona la voce "Artisti" nel menu principale
                listViewMenu.getSelectionModel().select("Artisti");

                // Popola la lista degli artisti
                artistListView.populateArtists();

                // Seleziona e scrolla fino all'artista associato al brano
                listViewArtist.getSelectionModel().select(selectedArtist);
                listViewArtist.scrollTo(selectedArtist);

                // Ora popola la lista degli album solo per quell'artista
                albumListView.populateAlbumsByArtist(selectedArtist);

                // Seleziona e scrolla fino all'album associato al brano
                Album album = getMusicLibrary().getAlbumByName(selectedAlbum); // Supponendo che ci sia un metodo per recuperare l'album dalla libreria
                if (album != null) {
                    listViewAlbum.getSelectionModel().select(album);
                    listViewAlbum.scrollTo(album);

                    // Ora che l'album è selezionato, popola automaticamente la lista dei brani
                    trackTableView.populateTracksByAlbum(album.getName());

                    // Seleziona e scrolla fino al brano selezionato nella tabella
                    tableViewTracks.getSelectionModel().select(selectedTrack);
                    tableViewTracks.scrollTo(selectedTrack);
                }

            }
        });
    }


    public void refreshAlbumDetail() {
        // Recupera tutti gli album dalla libreria musicale
        List<Album> albums = getMusicLibrary().getAlbums();

        System.out.println("Inizio aggiornamento dei dettagli degli album...");

        for (Album album : albums) {
            // Stampa il nome dell'album in fase di aggiornamento
            System.out.println("Aggiornamento album: " + album.getName());

            // Estrai tutti i generi delle tracce dell'album
            List<String> trackGenres = album.getTracks().stream()
                    .map(Track::getGenre)
                    .distinct()
                    .collect(Collectors.toList());

            // Stampa i generi delle tracce
            System.out.println("Generi trovati per l'album: " + trackGenres);

            if (trackGenres.size() == 1) {
                // Se tutte le tracce hanno lo stesso genere, imposta il genere dell'album
                album.setGenre(trackGenres.get(0));
                System.out.println("Genere unificato per l'album: " + trackGenres.get(0));
            } else {
                // Se ci sono più generi, imposta il genere dell'album come "Misto"
                album.setGenre("Misto");
                System.out.println("Album " + album.getName() + " impostato come genere misto.");
            }
        }

        // Refresh the views to reflect the changes in album details
        getAllViews().refreshAllViews();

        System.out.println("Aggiornamento dettagli degli album completato.");
    }



    // Getters and setters for the mode
    public String getCurrentTableMode() {
        return currentTableMode;
    }

    public void setCurrentTableMode(String mode) {
        this.currentTableMode = mode;
    }


    // Metodo per aprire o riportare in primo piano il frame di ricerca
    private void openSearchFrame(MusicLibrary musicLibrary, MainView mainView) {
// Always create a new instance of SearchFrame with the latest MusicLibrary
        searchFrame = new SearchFrame(musicLibrary, mainView);
        searchFrame.showSearchFrame();


    }

    public void updatePlaylistTableView() {
        // Ottieni la playlist selezionata
        Playlist selectedPlaylist = listViewPlaylist.getSelectionModel().getSelectedItem();

        if (selectedPlaylist != null) {
            // Pulisce la tabella corrente
            tableViewTracks.getItems().clear();

            // Aggiunge le tracce aggiornate dalla playlist
            tableViewTracks.getItems().addAll(selectedPlaylist.getTracks());

            // Aggiorna la tabella per mostrare le modifiche
            tableViewTracks.refresh();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }


}
