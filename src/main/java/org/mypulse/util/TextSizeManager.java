package org.mypulse.util;

import javafx.scene.Scene;
import javafx.stage.Window;

import java.util.List;
import java.util.prefs.Preferences;

// Gestisce la densità/dimensione del testo di tutta l'app (Vista > Dimensione testo),
// con lo stesso meccanismo e la stessa struttura di ThemeManager: ogni finestra chiama
// applyToScene(scene) dopo aver impostato tema e fogli di stile, e il cambio si applica
// dal vivo a tutte le finestre già aperte scorrendo Window.getWindows().
//
// A differenza del tema (un foglio CSS separato), qui non serve un file per taglia: la
// dimensione di base (14px) vive in theme-base.css/.root ed è ereditata da tutti i nodi
// che non la sovrascrivono più specificamente (titoli, intestazioni, ecc. restano alla
// loro dimensione fissa). Basta impostare -fx-font-size inline sul nodo radice della
// Scene per cambiare quella base ovunque non sia già più specifica.
public class TextSizeManager {

    public enum TextSize {
        COMPATTA("Compatta", 12),
        NORMALE("Normale", 14),
        GRANDE("Grande", 16);

        private final String displayName;
        private final int pixelSize;

        TextSize(String displayName, int pixelSize) {
            this.displayName = displayName;
            this.pixelSize = pixelSize;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getPixelSize() {
            return pixelSize;
        }
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(TextSizeManager.class);
    private static final String PREF_KEY = "textSize";

    private static TextSize current = loadSavedTextSize();

    private TextSizeManager() {
    }

    public static TextSize getCurrent() {
        return current;
    }

    private static TextSize loadSavedTextSize() {
        String saved = PREFS.get(PREF_KEY, TextSize.NORMALE.name());
        try {
            return TextSize.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return TextSize.NORMALE;
        }
    }

    // Cambia la dimensione del testo e la applica subito a tutte le finestre aperte,
    // ricordandola per il prossimo avvio (come ThemeManager.applyTheme)
    public static void applyTextSize(TextSize size) {
        current = size;
        PREFS.put(PREF_KEY, size.name());

        for (Window window : List.copyOf(Window.getWindows())) {
            Scene scene = window.getScene();
            if (scene != null) {
                applyToScene(scene);
            }
        }
    }

    // Imposta la dimensione corrente sul nodo radice della Scene indicata. Ogni finestra
    // la chiama una volta, dopo ThemeManager.applyToScene, così anche le finestre aperte
    // dopo un cambio di dimensione partono già con quella giusta.
    public static void applyToScene(Scene scene) {
        if (scene.getRoot() != null) {
            scene.getRoot().setStyle("-fx-font-size: " + current.getPixelSize() + "px;");
        }
    }
}
