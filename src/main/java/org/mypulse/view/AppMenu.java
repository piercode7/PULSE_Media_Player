package org.mypulse.view;

import org.mypulse.model.*;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.mypulse.util.AppSettings;
import org.mypulse.util.SerializationUtils;
import org.mypulse.util.TextSizeManager;
import org.mypulse.util.TextSizeManager.TextSize;
import org.mypulse.view.MainView;
import org.mypulse.view.components.AllViews;
import org.mypulse.view.components.TrackTableView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AppMenu {

    private final Runnable scanAction;
    private final MusicLibrary musicLibrary; // Riferimento alla libreria musicale
    private MainView mainView;
    private TrackTableView trackTableViewMain;
    private AllViews allViews;
    // Nome predefinito per il file serializzato
    private static final String DEFAULT_SAVE_FILE = "music_library.ser";

    // Directory predefinita per salvare il file
    private static final String SAVE_DIRECTORY = System.getProperty("user.dir"); // Directory corrente del programma

    // Coda a thread singolo per il salvataggio automatico: le scritture su disco vengono
    // eseguite in background (senza bloccare la UI) ma una alla volta, in ordine, così
    // due autoSaveLibrary() ravvicinati non scrivono in modo concorrente sullo stesso file
    private final ExecutorService autoSaveExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pulse-autosave");
        thread.setDaemon(true); // Non deve impedire la chiusura della JVM
        return thread;
    });

    // Costruttore che accetta un'azione di scansione e la libreria musicale
// Costruttore che accetta un'azione di scansione, la libreria musicale, MainView e AllViews
    public AppMenu(Runnable scanAction, MusicLibrary musicLibrary, MainView mainView, AllViews allViews) {
        this.scanAction = scanAction;
        this.musicLibrary = musicLibrary;
        this.mainView = mainView;
        this.allViews = allViews;
    }


    public MenuBar createMenuBar(Window window) {
        // Creare il MenuBar
        MenuBar menuBar = new MenuBar();

        // Creare i menu
        Menu fileMenu = new Menu("File");
        Menu editMenu = new Menu("Modifica");
        // Prima "API", con la sola voce Spotify: rinominato in "Impostazioni" per
        // ospitare anche preferenze non legate a un'API specifica (vedi syncMetadataItem)
        Menu settingsMenu = new Menu("Impostazioni");
        Menu viewMenu = new Menu("Vista");

        // Creare i menu item
        MenuItem openItem = new MenuItem("Apri");
        MenuItem saveItem = new MenuItem("Salva");
        MenuItem saveAsItem = new MenuItem("Salva come");
        MenuItem loadItem = new MenuItem("Carica");
        MenuItem loadIChoseItem = new MenuItem("Carica da...");
        MenuItem scanItem = new MenuItem("Scansiona cartella");
        MenuItem deleteItem = new MenuItem("Inizializza libreria");
        MenuItem exitItem = new MenuItem("Esci");


        MenuItem editAlbumDetails = new MenuItem("Aggiorna dettagli album");

        MenuItem apiSpoti = new MenuItem("Inserisci API di Spotify");

        // Prima veniva chiesto ogni volta (con un dialogo "Libreria"/"Libreria e
        // metadati") se salvare le modifiche anche nei tag reali del file audio, sia
        // dall'editor metadati sia dalla finestra Lyrics. Ora è una scelta persistente
        // fatta una volta sola qui, invece che ripetuta ad ogni salvataggio.
        CheckMenuItem syncMetadataItem = new CheckMenuItem("Aggiorna anche i metadati nel file audio");
        syncMetadataItem.setSelected(AppSettings.isSyncMetadataToFile());
        syncMetadataItem.setOnAction(event ->
                AppSettings.setSyncMetadataToFile(syncMetadataItem.isSelected()));

        // Voce del menu "Vista", che finora non ne aveva mai avuta nessuna
        MenuItem themeItem = new MenuItem("Tema...");

        // Sottomenu per la dimensione del testo (Compatta/Normale/Grande): tre opzioni
        // mutuamente esclusive, non serve un frame dedicato come per il tema
        Menu textSizeMenu = new Menu("Dimensione testo");
        ToggleGroup textSizeGroup = new ToggleGroup();
        for (TextSize size : TextSize.values()) {
            RadioMenuItem item = new RadioMenuItem(size.getDisplayName());
            item.setToggleGroup(textSizeGroup);
            item.setSelected(size == TextSizeManager.getCurrent());
            item.setOnAction(event -> TextSizeManager.applyTextSize(size));
            textSizeMenu.getItems().add(item);
        }

        // Aggiungere l'azione di scansione
        scanItem.setOnAction(event -> {
            scanAction.run();
            allViews.postInit();
            autoSaveLibrary(); // I brani appena trovati non erano ancora mai stati salvati
        });

        // Aggiungere l'azione di uscita (stessa azione usata anche alla chiusura con la X,
        // vedi saveAndExit più sotto)
        exitItem.setOnAction(event -> saveAndExit(window));

        // Aggiungere l'azione di salvataggio con nome predefinito
        saveItem.setOnAction(event -> saveLibraryWithDefaultName(window));

        // Aggiungere l'azione di salvataggio con scelta directory
        saveAsItem.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salva Libreria Musicale");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Serialized File", "*.ser"));
            File file = fileChooser.showSaveDialog(window);
            if (file != null) {
                try {
                    SerializationUtils.serialize(musicLibrary, file.getAbsolutePath());
                    System.out.println("Libreria musicale salvata con successo su " + file.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Errore durante la serializzazione: " + e.getMessage());
                }
            }
        });

        // Aggiungere l'azione di caricamento con nome predefinito
        loadItem.setOnAction(event -> loadLibraryWithDefaultName(window));

        // Aggiungere l'azione di caricamento da un file scelto
        loadIChoseItem.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Carica Libreria Musicale");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Serialized File", "*.ser"));
            File file = fileChooser.showOpenDialog(window);
            if (file != null) {
                try {
                    MusicLibrary loadedLibrary = (MusicLibrary) SerializationUtils.deserialize(file.getAbsolutePath());
                    musicLibrary.copyFrom(loadedLibrary); // Copia i dati nella libreria corrente
                    System.out.println("Libreria musicale caricata con successo da " + file.getAbsolutePath());
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Errore durante il caricamento: " + e.getMessage());
                }
            }
        });

        // Aggiungere l'azione di inizializzazione della libreria
        deleteItem.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Conferma Inizializzazione");
            alert.setHeaderText("Stai per cancellare la libreria musicale");
            alert.setContentText("Vuoi davvero cancellare tutti i dati dalla libreria musicale? Questa operazione non può essere annullata.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                musicLibrary.clearLibrary(); // Chiama il metodo per cancellare i dati
                System.out.println("La libreria musicale è stata inizializzata.");
                allViews.postInit();
                autoSaveLibrary(); // La libreria svuotata va persistita, non solo in memoria
            }
        });

        editAlbumDetails.setOnAction(event -> {
            mainView.refreshAlbumDetail();

            // Il genere aggiornato si vede nella lista album (sottotitolo di ogni cella):
            // senza ripopolarla restava quello vecchio finché non si riselezionava
            // manualmente "Artisti" o "Album" nel menu principale
            mainView.getAlbumListView().populateAlbums();
            String selectedArtist = mainView.listViewArtist.getSelectionModel().getSelectedItem();
            if (selectedArtist != null && !selectedArtist.startsWith("Artisti trovati:")) {
                mainView.getAlbumListView().populateAlbumsByArtist(selectedArtist);
            }
        });

        apiSpoti.setOnAction(event -> SpotifyAPIDialog.requestAndSaveAPICredentials());

        themeItem.setOnAction(event -> ThemePickerFrame.show());

        // Aggiungere gli item al menu "File"
        fileMenu.getItems().addAll(openItem, saveItem, saveAsItem, loadItem, loadIChoseItem, scanItem, deleteItem, exitItem);

        editMenu.getItems().addAll(editAlbumDetails);

        settingsMenu.getItems().addAll(apiSpoti, new SeparatorMenuItem(), syncMetadataItem);

        viewMenu.getItems().addAll(themeItem, textSizeMenu);

        // Aggiungere i menu al menuBar
        menuBar.getMenus().addAll(fileMenu, editMenu, settingsMenu, viewMenu);

        return menuBar;




    }







    // Usato sia dalla voce di menu "Esci" sia dalla chiusura della finestra con la X.
    // Prima chiedeva conferma ogni volta (Salva e Esci / Esci senza salvare / Annulla):
    // con il salvataggio automatico ormai attivo dopo ogni modifica significativa, quel
    // dialogo era solo un passaggio in più - ora salva ed esce direttamente.
    public void saveAndExit(Window window) {
        saveLibraryWithoutConfirmation();
        Platform.exit();
    }

    // Metodo per salvare la libreria con nome predefinito
    private void saveLibraryWithDefaultName(Window window) {
        File defaultFile = new File(SAVE_DIRECTORY, DEFAULT_SAVE_FILE);

        // Se il file esiste già, chiedi conferma di sovrascrittura
        if (defaultFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Conferma Sovrascrittura");
            alert.setHeaderText("Il file esiste già");
            alert.setContentText("Vuoi sovrascrivere il file esistente?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Se l'utente conferma, salva e sovrascrivi il file
                serializeLibrary(defaultFile);
            }
        } else {
            // Se il file non esiste, procedi con il salvataggio
            serializeLibrary(defaultFile);
        }
    }

    // Salva la libreria sul file predefinito in background, senza dialoghi né bloccare
    // la UI. Va chiamato dopo ogni azione che modifica davvero i dati (editor metadati,
    // copertine, playlist, scansione, eliminazioni, ...): con questo, il salvataggio
    // manuale ("Salva" o il dialogo di conferma all'uscita) non serve più per non perdere
    // le modifiche, resta solo come opzione esplicita (es. "Salva come" per esportare).
    public void autoSaveLibrary() {
        File defaultFile = new File(SAVE_DIRECTORY, DEFAULT_SAVE_FILE);
        autoSaveExecutor.submit(() -> {
            try {
                SerializationUtils.serialize(musicLibrary, defaultFile.getAbsolutePath());
                System.out.println("[Salvataggio automatico] Libreria aggiornata su " + defaultFile.getAbsolutePath());
            } catch (IOException e) {
                System.out.println("[Salvataggio automatico] Errore durante il salvataggio: " + e.getMessage());
            }
        });
    }

    // Metodo per salvare la libreria con nome predefinito senza conferma di sovrascrittura
    private void saveLibraryWithoutConfirmation() {
        File defaultFile = new File(SAVE_DIRECTORY, DEFAULT_SAVE_FILE);

        // Salva direttamente il file, sovrascrivendo se già esiste
        serializeLibrary(defaultFile);
    }



    // Metodo per serializzare la libreria
    private void serializeLibrary(File file) {
        try {
            SerializationUtils.serialize(musicLibrary, file.getAbsolutePath());
            System.out.println("Libreria musicale salvata con successo su " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Errore durante la serializzazione: " + e.getMessage());
        }
    }

    // Metodo per caricare la libreria con nome predefinito
    private void loadLibraryWithDefaultName(Window window) {
        File defaultFile = new File(SAVE_DIRECTORY, DEFAULT_SAVE_FILE);

        // Se il file non esiste, mostra un errore
        if (!defaultFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("File non trovato");
            alert.setContentText("Il file predefinito non esiste.");
            alert.showAndWait();
            return;
        }

        try {
            MusicLibrary loadedLibrary = (MusicLibrary) SerializationUtils.deserialize(defaultFile.getAbsolutePath());
            musicLibrary.copyFrom(loadedLibrary); // Copia i dati nella libreria corrente
            System.out.println("Libreria musicale caricata con successo da " + defaultFile.getAbsolutePath());
            allViews.postInit();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Errore durante il caricamento: " + e.getMessage());
        }
    }


}
