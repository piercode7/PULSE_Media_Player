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
import org.mypulse.util.ThemeManager;
import org.mypulse.util.Utils;
import org.mypulse.view.components.*;

import java.io.*;
import java.util.*;
import java.util.logging.LogManager;
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
    private MediaPlayerController mediaPlayerControl;
    private AppMenu appMenu;
    private Stage primaryStage;
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
        this.primaryStage = primaryStage; // Serve come "owner" per le finestre ausiliarie (es. ricerca)

        // Inizializza la libreria e lo scanner musicale
        musicLibrary = new MusicLibrary();
        Utils utils = new Utils();
        queuedTracks = new ArrayList<>();
        musicScanner = new MusicScanner(musicLibrary);
        lyricsController = new LyricsController(this);
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
        appMenu = new AppMenu(scanAction, musicLibrary, this, allViews);
        MenuBar menuBar = appMenu.createMenuBar(primaryStage);
        // Niente più colore fisso qui: la regola .menu-bar del tema attivo (dark-theme.css)
        // si applica da sola. Prima questo .setStyle() aveva priorità su qualunque tema
        // scelto dal selettore, quindi la barra del menu restava sempre dello stesso grigio.


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
        // Classe dedicata per dare alla navigazione principale un peso diverso (più in
        // grassetto) dalle liste di contenuto (artisti, brani, ...) che condividono la
        // stessa .list-cell generica - più gerarchia visiva, meno "piatto"
        listViewMenu.getStyleClass().add("nav-list");
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


// Pulsanti Cerca e Lyrics: costruiti qui perché servono al MediaPlayerController (li
// mette sulla stessa riga dei controlli di riproduzione, agli estremi sinistro/destro).
// Le azioni restano collegate più sotto, dove già venivano assegnate.
        searchButton = new Button("Cerca");
        searchButton.setMinWidth(60);

        lyricsButton = new Button("Lyrics");
        lyricsButton.setMinWidth(60);

// Etichette con titolo/artista/album del brano in riproduzione. Prima stavano nella
// stessa fascia dei due pulsanti sopra; ora quella fascia non esiste più (Cerca e Lyrics
// sono passati al media player), quindi restano da sole in una riga sottile, inserita
// dentro il media player subito sopra la barra di avanzamento/volume.
        HBox nowPlayingLabels = new HBox(10);
        nowPlayingLabels.setAlignment(Pos.CENTER);
        nowPlayingLabels.setPadding(new Insets(0, 0, 2, 0));  // Un po' di spazio verso la barra volume

        titleLabel = new Label("");
        artistLabel = new Label("");
        albumLabel = new Label("");

        titleLabel.getStyleClass().add("clickable-label");

// Colori del tema invece di bianco/grigio fissi, così cambiano insieme al resto quando si
// sceglie un tema diverso dal selettore.
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -text-primary;");
        artistLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        albumLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        nowPlayingLabels.getChildren().addAll(titleLabel, artistLabel, albumLabel);


        // Crea il MediaPlayerController, passandogli Cerca e Lyrics da collocare sulla
        // riga dei controlli di riproduzione
        // Media player at the bottom
        mediaPlayerControl = new MediaPlayerController(musicLibrary, this, allViews, searchButton, lyricsButton);
        mediaPlayerControl.setId("bottom"); // Assegna un ID al MediaPlayerController

        // Inserisce la riga con titolo/artista/album sopra i pulsanti di trasporto
        // (indice 0): prima stava fra i pulsanti e la barra progresso/volume, ma titolo e
        // pulsanti funzionano meglio con l'etichetta cliccabile in cima
        mediaPlayerControl.getChildren().add(0, nowPlayingLabels);

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
                        autoSaveLibrary();
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
                        autoSaveLibrary();
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
            if (newValue != null) {
                // Separa in console il log di questo cambio vista (Artisti/Album/Brani/
                // Coda/Playlist) da quello dell'azione precedente
                Utils.logSeparator();
            }

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

        // Seleziona "Artisti" di default all'avvio: senza questo listViewMenu partiva
        // senza alcuna selezione (nessun elemento è preselezionato in un ListView per
        // conto suo) e l'app si apriva su una finestra vuota - solo il menu a sinistra,
        // nessuna lista popolata - finché l'utente non cliccava manualmente una voce
        listViewMenu.getSelectionModel().select("Artisti");

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
                        autoSaveLibrary();
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
        ThemeManager.applyToScene(scene); // Applica il tema (colori) attualmente scelto

        primaryStage.setScene(scene);
        primaryStage.setTitle("Pulse");

        // Chiudere con la X passava prima direttamente per la chiusura della finestra,
        // senza mai chiedere se salvare la libreria (a differenza di File > Esci): ora
        // usa lo stesso dialogo, e consuma l'evento perché è confirmAndExit stesso a
        // decidere se e quando chiudere davvero (via Platform.exit())
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            appMenu.confirmAndExit(primaryStage);
        });

        primaryStage.show();
    }

    @Override
    public void stop() {
        // Punto unico di rilascio delle risorse native del MediaPlayer, chiamato dal
        // runtime JavaFX per qualunque via si esca dall'applicazione (X, menu Esci, ecc.):
        // prima il MediaPlayer non veniva mai rilasciato alla chiusura dell'app
        if (mediaPlayerControl != null) {
            mediaPlayerControl.releaseMediaPlayer();
        }
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

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // Punto unico da cui editor e dialoghi (metadati, copertine, playlist, ...) chiedono
    // il salvataggio automatico della libreria dopo una modifica, senza dover conoscere
    // AppMenu direttamente
    public void autoSaveLibrary() {
        if (appMenu != null) {
            appMenu.autoSaveLibrary();
        }
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

        // Chiamato anche da TrackMetadataEditor dopo un salvataggio (che già fa il suo
        // autoSaveLibrary): qui in più copre la voce di menu "Aggiorna dettagli album",
        // che prima non salvava mai il nuovo genere calcolato per gli album
        autoSaveLibrary();
    }



    // Getters and setters for the mode
    public String getCurrentTableMode() {
        return currentTableMode;
    }

    public void setCurrentTableMode(String mode) {
        this.currentTableMode = mode;
    }


    // Metodo per aprire o riportare in primo piano il frame di ricerca. Un'unica istanza
    // per tutta la sessione: prima ne veniva creata una nuova ad ogni click sul pulsante
    // "Cerca" senza mai chiudere le precedenti, che restavano aperte (nascoste dietro le
    // altre) accumulandosi per tutta la durata dell'app. musicLibrary è lo stesso oggetto
    // per l'intera sessione (viene aggiornato in place da un caricamento/scansione, mai
    // sostituito), quindi una singola SearchFrame vede sempre i dati aggiornati.
    private void openSearchFrame(MusicLibrary musicLibrary, MainView mainView) {
        if (searchFrame == null) {
            searchFrame = new SearchFrame(musicLibrary, mainView);
        }
        // toFront()/requestFocus() del campo di testo sono già gestiti dentro
        // showSearchFrame()
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
        configureLogging();
        launch(args);
    }

    // Carica src/main/resources/logging.properties (finora presente ma mai letto da
    // nessuna parte): senza questo, i log interni INFO/WARNING di jaudiotagger
    // (uno per ogni frame ID3 letto) sommergono il riepilogo di scansione in console
    private static void configureLogging() {
        try (InputStream configStream = MainView.class.getResourceAsStream("/logging.properties")) {
            if (configStream != null) {
                LogManager.getLogManager().readConfiguration(configStream);
            }
        } catch (IOException e) {
            System.out.println("Impossibile caricare logging.properties: " + e.getMessage());
        }
    }


}
