package org.mypulse.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.mypulse.model.Track;
import org.mypulse.util.LyricsFetcher;
import org.mypulse.util.ThemeManager;
import org.mypulse.view.MainView;

import java.io.File;
import java.util.Optional;

public class LyricsController {
    private final MainView mainView;

    public LyricsController(MainView mainView) {
        this.mainView = mainView;
    }

    public void showLyrics() {
        // Check if there is a currently playing track
        if (mainView.getCurrentlyPlayingTrack() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Track Playing");
            alert.setHeaderText(null);
            alert.setContentText("There is no track currently playing.");
            alert.showAndWait();
            return;
        }

        // Il brano in riproduzione è già disponibile direttamente: non serve ricercarlo
        // per titolo/artista/album (la ricerca era anche fragile, poteva restituire null
        // e mandare in NullPointerException il codice sotto)
        Track track = mainView.getCurrentlyPlayingTrack();
        String title = track.getTitle();
        String artist = track.getArtist();

        // Check if the title or artist is empty
        if (title == null || title.isEmpty() || artist == null || artist.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Information Missing");
            alert.setHeaderText(null);
            alert.setContentText("Please select a song with both artist and title.");
            alert.showAndWait();
            return;
        }

        // Create a new frame for displaying the lyrics
        Stage lyricsStage = new Stage();
        lyricsStage.setTitle("Lyrics - " + title);
        lyricsStage.setResizable(false);
        lyricsStage.setAlwaysOnTop(true);

        // Set the owner of the lyricsStage to the main application window
        lyricsStage.initModality(Modality.WINDOW_MODAL); // Make it modal only to the application


        // Intestazione con titolo/artista, per dare al testo un contesto (come su
        // Spotify) invece di lasciare solo il titolo della finestra a dirlo
        Label titleHeader = new Label(title);
        titleHeader.getStyleClass().add("lyrics-title");
        Label artistHeader = new Label(artist);
        artistHeader.getStyleClass().add("lyrics-artist");
        VBox header = new VBox(2, titleHeader, artistHeader);
        header.setAlignment(Pos.CENTER);

        // Create a TextArea to display the lyrics
        TextArea lyricsArea = new TextArea();
        lyricsArea.setWrapText(true);
        lyricsArea.setEditable(true);
        lyricsArea.setPrefWidth(560);
        lyricsArea.setPrefHeight(560);
        lyricsArea.getStyleClass().add("lyrics-text");
        if (track.getLyrics() != null) {
            lyricsArea.setText(track.getLyrics());
        }

        // Dimensione del testo regolabile con i pulsanti A-/A+ qui sotto, invece di
        // dover ingrandire l'intera finestra o affidarsi allo zoom del sistema
        int[] lyricsFontSize = {19};
        java.util.function.IntConsumer applyFontSize = size ->
                lyricsArea.setStyle("-fx-font-size: " + size + "px;");
        applyFontSize.accept(lyricsFontSize[0]);

        // Add a button for fetching the lyrics
        Button fetchLyricsButton = new Button("Fetch Lyrics");
        fetchLyricsButton.getStyleClass().add("lyrics-toolbar-button");

        Button zoomOutButton = new Button("A−");
        zoomOutButton.getStyleClass().add("lyrics-zoom-button");
        Button zoomInButton = new Button("A+");
        zoomInButton.getStyleClass().add("lyrics-zoom-button");

        zoomOutButton.setOnAction(event -> {
            lyricsFontSize[0] = Math.max(13, lyricsFontSize[0] - 2);
            applyFontSize.accept(lyricsFontSize[0]);
        });
        zoomInButton.setOnAction(event -> {
            lyricsFontSize[0] = Math.min(34, lyricsFontSize[0] + 2);
            applyFontSize.accept(lyricsFontSize[0]);
        });

        Button saveLyricsButton = new Button("Save Lyrics");
        saveLyricsButton.getStyleClass().add("lyrics-toolbar-button");

        // Barra sopra il testo: Fetch a sinistra, zoom al centro, Save a destra - stessa
        // logica "estremi/centro" già usata per la barra del media player
        Region toolbarLeftSpacer = new Region();
        Region toolbarRightSpacer = new Region();
        HBox.setHgrow(toolbarLeftSpacer, Priority.ALWAYS);
        HBox.setHgrow(toolbarRightSpacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8,
                fetchLyricsButton,
                toolbarLeftSpacer,
                zoomOutButton, zoomInButton,
                toolbarRightSpacer,
                saveLyricsButton
        );
        toolbar.setAlignment(Pos.CENTER);

        // Add action to the button
        fetchLyricsButton.setOnAction(event -> {
            // Run the Python script to fetch lyrics. Usa l'artista del brano (chi la
            // canta davvero), non l'artista dell'album: per compilation/soundtrack
            // l'artista album può essere "Various Artists" e costruirebbe un URL Genius
            // sbagliato
            String lyrics = LyricsFetcher.fetchLyrics(artist, title);
            if (lyrics != null) {
                lyricsArea.setText(lyrics);
            } else {
                lyricsArea.setText("Lyrics not found.");
            }
        });

        // Add an EventFilter for smooth scrolling
        lyricsArea.addEventFilter(ScrollEvent.SCROLL, event -> {
            double deltaY = event.getDeltaY();
            double newScrollTop = lyricsArea.getScrollTop() - deltaY / 3;
            lyricsArea.setScrollTop(newScrollTop);
            event.consume();
        });

        saveLyricsButton.setOnAction(event -> {
            // Controlla se i testi sono stati modificati
            String newLyrics = lyricsArea.getText();
            if (!newLyrics.equals(track.getLyrics())) {
                // Chiedi se aggiornare solo l'istanza o anche i metadati
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Aggiornamento Lyrics");
                alert.setHeaderText("Vuoi aggiornare solo la libreria o anche i metadati del file?");

                ButtonType updateOnlyInstanceButton = new ButtonType("Solo Libreria");
                ButtonType updateBothButton = new ButtonType("Libreria e metadati");
                ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(updateOnlyInstanceButton, updateBothButton, cancelButton);

                // Imposta il frame delle lyrics come proprietario del dialogo
                alert.initOwner(lyricsStage);
                alert.initModality(Modality.WINDOW_MODAL); // Modalità per apparire sopra al frame delle lyrics

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == updateOnlyInstanceButton) {
                        // Aggiorna solo l'istanza nella libreria
                        track.setLyrics(newLyrics);
                        System.out.println("Lyrics aggiornati nella libreria.");
                        mainView.autoSaveLibrary();
                    } else if (result.get() == updateBothButton) {
                        // Aggiorna sia l'istanza nella libreria che i metadati del file
                        track.setLyrics(newLyrics);
                        updateLyricsInFile(track); // Metodo per aggiornare i metadati effettivi
                        System.out.println("Lyrics aggiornati nella libreria e nel file.");
                        mainView.autoSaveLibrary();
                    }
                    // Se si clicca su "Annulla", non c'è nulla da salvare
                }
            }
        });





        // Layout for the frame
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(header, toolbar, lyricsArea);
        VBox.setVgrow(lyricsArea, Priority.ALWAYS);
        vbox.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(vbox, 640, 760);
        scene.setFill(null);  // Set the initial scene fill to transparent
        scene.getStylesheets().add(getClass().getResource("/smooth-scroll.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/dark-lyrics.css").toExternalForm());
        ThemeManager.applyToScene(scene); // Applica il tema (colori) attualmente scelto

        lyricsStage.setScene(scene);
        lyricsStage.show();
    }

    // Metodo per aggiornare i metadati del file
    private void updateLyricsInFile(Track track) {
        try {
            // Utilizza Jaudiotagger per aggiornare i metadati normali
            File audioFile = new File(track.getFilePath());
            AudioFile f = AudioFileIO.read(audioFile);
            Tag tag = f.getTag();

            if (tag != null) {
                tag.setField(FieldKey.LYRICS, track.getLyrics());
                f.commit(); // Salva i cambiamenti nel file
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Errore durante l'aggiornamento dei metadati del file.");
        }
    }

    // Il recupero dei lyrics (eseguendo get_lyrics.py) è ora in LyricsFetcher, condiviso
    // con TrackMetadataEditor (che prima non aveva affatto questa funzione nella sua tab
    // Lyrics)
}
