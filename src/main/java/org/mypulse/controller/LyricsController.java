package org.mypulse.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.mypulse.model.Track;
import org.mypulse.view.MainView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

        // Retrieve the song details from the labels
        String title = mainView.getCurrentlyPlayingTrack().getTitle();
        System.out.println(title);
        String artist = mainView.getCurrentlyPlayingTrack().getArtist();
        System.out.println(artist);
        String artistAlbum = mainView.getCurrentlyPlayingTrack().getArtistAlbum();
        System.out.println(artistAlbum);
        String album = mainView.getAlbumLabel().getText();
        System.out.println(album);
        Track track = mainView.getMusicLibrary().getTrackByDetails(title, artist, album);

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


        // Create a TextArea to display the lyrics
        TextArea lyricsArea = new TextArea();
        lyricsArea.setWrapText(true);
        lyricsArea.setEditable(true);
        lyricsArea.setPrefWidth(500);
        lyricsArea.setPrefHeight(600);
        if (track.getLyrics() != null) {
            lyricsArea.setText(track.getLyrics());
        }

        // Add a button for fetching the lyrics
        Button fetchLyricsButton = new Button("Fetch Lyrics");
        fetchLyricsButton.setAlignment(Pos.CENTER);
        fetchLyricsButton.setPadding(new Insets(10));

        Button saveLyricsButton = new Button("Save Lyrics");
        saveLyricsButton.setAlignment(Pos.CENTER_RIGHT);
        saveLyricsButton.setPadding(new Insets(10));

        // Add action to the button
        fetchLyricsButton.setOnAction(event -> {
            // Run the Python script to fetch lyrics
            String lyrics = fetchLyricsFromPythonScript(artistAlbum, title);
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
                    } else if (result.get() == updateBothButton) {
                        // Aggiorna sia l'istanza nella libreria che i metadati del file
                        track.setLyrics(newLyrics);
                        updateLyricsInFile(track); // Metodo per aggiornare i metadati effettivi
                        System.out.println("Lyrics aggiornati nella libreria e nel file.");
                    }
                }
            }
        });





        // Layout for the frame
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(15));
        vbox.getChildren().addAll(fetchLyricsButton, lyricsArea, saveLyricsButton);
        vbox.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(vbox, 600, 700);
        scene.setFill(null);  // Set the initial scene fill to transparent
        scene.getStylesheets().add(getClass().getResource("/smooth-scroll.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/dark-lyrics.css").toExternalForm());

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

    // Method to run the Python script and fetch lyrics
    private String fetchLyricsFromPythonScript(String artist, String songTitle) {
        try {
            // Build the command to run the Python script
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "get_lyrics.py", artist, songTitle);
            processBuilder.directory(new File("src/main/resources")); // Adjust the path to where your script is located

            // Start the process
            Process process = processBuilder.start();

            // Read the output of the Python script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString().trim(); // Return the lyrics
            } else {
                System.err.println("Error: Python script exited with code " + exitCode);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
