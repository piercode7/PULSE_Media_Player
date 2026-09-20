package org.mypulse;

import org.mypulse.view.MainView;

public class Main {
    // Entry point alternativo (es. avvio da IDE): delega a MainView.main() invece di
    // chiamare Application.launch() direttamente, altrimenti bypassava la
    // configurazione del logging fatta lì (vedi configureLogging() in MainView)
    public static void main(String[] args) {
        MainView.main(args);
    }
}
