package org.mypulse.util;

import javafx.scene.control.Alert;
import javafx.util.Duration;
import org.mypulse.model.MusicLibrary;

public class Utils {

    // Metodo per formattare la durata in ore, minuti e secondi
    private String formatDurationInHMS(Integer durationInSeconds) {
        if (durationInSeconds == null) {
            return "N/A";
        }
        int hours = durationInSeconds / 3600;
        int minutes = (durationInSeconds % 3600) / 60;
        int seconds = durationInSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    // Metodo per verificare se la libreria musicale è valida
    public boolean isValidMusicLibrary(MusicLibrary library) {
        return library != null && library.getAllTracks() != null && library.getAllAlbums() != null;
    }

    // Metodo per mostrare un avviso
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Metodo per creare un avviso (spostato qui dalla classe originale)
    public static Alert createAlert(Alert.AlertType type, String title, String headerText, String contentText) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        return alert;
    }

    // Metodo per formattare la durata in formato mm:ss
    public String formatDuration(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
