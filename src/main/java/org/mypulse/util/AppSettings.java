package org.mypulse.util;

import java.util.prefs.Preferences;

// Impostazioni dell'app che devono restare valide tra un avvio e l'altro, con lo stesso
// meccanismo già usato da ThemeManager (java.util.prefs, nessuna dipendenza aggiuntiva).
public class AppSettings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppSettings.class);
    private static final String SYNC_METADATA_TO_FILE_KEY = "syncMetadataToFile";

    private AppSettings() {
    }

    // Se true, salvare le modifiche a testi/metadati di un brano (editor metadati,
    // finestra Lyrics) scrive anche i tag reali nel file audio, non solo la libreria
    // interna dell'app. Prima veniva chiesto con un dialogo ogni singola volta; ora è
    // una scelta fatta una volta sola in Impostazioni. Default: false (non tocca i file
    // finché l'utente non lo attiva esplicitamente, il comportamento meno invasivo).
    public static boolean isSyncMetadataToFile() {
        return PREFS.getBoolean(SYNC_METADATA_TO_FILE_KEY, false);
    }

    public static void setSyncMetadataToFile(boolean syncToFile) {
        PREFS.putBoolean(SYNC_METADATA_TO_FILE_KEY, syncToFile);
    }
}
