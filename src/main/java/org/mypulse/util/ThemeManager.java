package org.mypulse.util;

import javafx.scene.Scene;
import javafx.stage.Window;

import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Gestisce il tema (colori) dell'intera applicazione. Ogni finestra carica il proprio
// foglio di stile strutturale (dark-theme.css, dark-editor.css, dark-lyrics.css - la
// disposizione/i selettori, invariati per tutti i temi) e in più chiama
// applyToScene(scene) per aggiungere il foglio del tema attualmente scelto (i colori). Il
// cambio tema (applyTheme) aggiorna dal vivo tutte le finestre già aperte scorrendo
// Window.getWindows() - l'elenco che JavaFX stesso mantiene aggiornato - senza che questa
// classe debba tenere traccia delle finestre per conto proprio.
public class ThemeManager {

    public enum Theme {
        SCURO("Scuro", "/theme-dark.css", "#0d0d0d", "#1a1a1a", "#b3b3b3", "#ededed"),
        INDACO("Indaco", "/theme-indigo.css", "#14161c", "#262a38", "#6c5ce7", "#eceef2"),
        AMBRA("Ambra", "/theme-amber.css", "#181512", "#2f2820", "#e08a3c", "#f2ece2"),
        CHIARO("Chiaro", "/theme-light.css", "#f5f6f8", "#e7e9ee", "#6c5ce7", "#1c1e24");

        private final String displayName;
        private final String cssPath;
        // Colori usati solo per disegnare l'anteprima nel selettore tema: presi qui,
        // fissi, invece che leggendo il vero foglio CSS, così l'anteprima di un tema
        // mostra sempre i suoi colori indipendentemente dal tema attualmente attivo
        private final String previewBase;
        private final String previewSurface;
        private final String previewAccent;
        private final String previewText;

        Theme(String displayName, String cssPath, String previewBase, String previewSurface,
              String previewAccent, String previewText) {
            this.displayName = displayName;
            this.cssPath = cssPath;
            this.previewBase = previewBase;
            this.previewSurface = previewSurface;
            this.previewAccent = previewAccent;
            this.previewText = previewText;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssPath() {
            return cssPath;
        }

        public String getPreviewBase() {
            return previewBase;
        }

        public String getPreviewSurface() {
            return previewSurface;
        }

        public String getPreviewAccent() {
            return previewAccent;
        }

        public String getPreviewText() {
            return previewText;
        }
    }

    // Percorsi di tutti i fogli tema conosciuti: servono per rimuovere quello vecchio
    // prima di aggiungere quello nuovo, senza dover tenere un riferimento separato alla
    // Scene per ogni finestra
    private static final List<String> THEME_CSS_PATHS = Stream.of(Theme.values())
            .map(Theme::getCssPath)
            .collect(Collectors.toList());

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY = "theme";

    private static Theme current = loadSavedTheme();

    private ThemeManager() {
    }

    public static Theme getCurrent() {
        return current;
    }

    private static Theme loadSavedTheme() {
        String saved = PREFS.get(PREF_KEY, Theme.INDACO.name());
        try {
            return Theme.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return Theme.INDACO;
        }
    }

    // Cambia il tema e lo applica subito a tutte le finestre attualmente aperte. Lo
    // ricorda anche per il prossimo avvio dell'app (java.util.prefs, nessuna dipendenza
    // aggiuntiva) e per le finestre che verranno aperte dopo questo momento.
    public static void applyTheme(Theme theme) {
        current = theme;
        PREFS.put(PREF_KEY, theme.name());

        for (Window window : List.copyOf(Window.getWindows())) {
            Scene scene = window.getScene();
            if (scene != null) {
                applyToScene(scene);
            }
        }
    }

    // Aggiunge il foglio del tema corrente alla Scene indicata, rimuovendo un eventuale
    // foglio tema già presente (nel caso la Scene venga ri-passata dopo un cambio tema).
    // Ogni finestra la chiama una volta, dopo aver aggiunto il proprio foglio strutturale,
    // così anche le finestre aperte dopo un cambio tema partono già con quello giusto.
    public static void applyToScene(Scene scene) {
        String cssUrl = ThemeManager.class.getResource(current.getCssPath()).toExternalForm();
        scene.getStylesheets().removeIf(sheet ->
                THEME_CSS_PATHS.stream().anyMatch(sheet::endsWith));
        scene.getStylesheets().add(cssUrl);
    }
}
