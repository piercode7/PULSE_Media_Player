package org.mypulse.model;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.mypulse.util.SerializationUtils;
import org.mypulse.view.MainView;
import org.mypulse.view.components.AllViews;
import org.mypulse.view.components.TrackTableView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
        Menu apiMenu = new Menu("API");
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

        // Aggiungere l'azione di scansione
        scanItem.setOnAction(event ->{ scanAction.run();allViews.postInit();}
);

        // Aggiungere l'azione di uscita
// Aggiungere l'azione di uscita
        exitItem.setOnAction(event -> {
            Alert confirmExit = new Alert(Alert.AlertType.CONFIRMATION);
            confirmExit.setTitle("Conferma uscita");
            confirmExit.setHeaderText("Vuoi uscire dall'applicazione?");
            confirmExit.setContentText("Se confermi, la libreria musicale verrà salvata prima di chiudere.");

            ButtonType saveAndExitButton = new ButtonType("Salva e Esci");
            ButtonType exitWithoutSavingButton = new ButtonType("Esci senza salvare");
            ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

            confirmExit.getButtonTypes().setAll(saveAndExitButton, exitWithoutSavingButton, cancelButton);

            Optional<ButtonType> result = confirmExit.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveAndExitButton) {
                    // Richiama l'azione di salvataggio
                    saveLibraryWithoutConfirmation(window);
                    Platform.exit(); // Chiude l'applicazione dopo il salvataggio
                } else if (result.get() == exitWithoutSavingButton) {
                    Platform.exit(); // Chiude l'applicazione senza salvare
                }
                // Se si clicca su "Annulla", non si fa nulla
            }
        });
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

            }
        });

        editAlbumDetails.setOnAction(event -> {
            mainView.refreshAlbumDetail();

        });

        apiSpoti.setOnAction(event -> SpotifyAPIDialog.requestAndSaveAPICredentials());



        // Aggiungere gli item al menu "File"
        fileMenu.getItems().addAll(openItem, saveItem, saveAsItem, loadItem, loadIChoseItem, scanItem, deleteItem, exitItem);

        editMenu.getItems().addAll(editAlbumDetails);

        apiMenu.getItems().addAll(apiSpoti);

        // Aggiungere i menu al menuBar
        menuBar.getMenus().addAll(fileMenu, editMenu, apiMenu, viewMenu);

        return menuBar;




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

    // Metodo per salvare la libreria con nome predefinito senza conferma di sovrascrittura
    private void saveLibraryWithoutConfirmation(Window window) {
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
