package org.mypulse.controller;

import javafx.stage.DirectoryChooser;
import org.mypulse.model.MusicScanner;
import org.mypulse.view.components.ArtistListView;

import java.io.File;

public class MusicController {
    private MusicScanner musicScanner;
    private ArtistListView artistListView;

    // Costruttore che accetta MusicScanner e ArtistListView come parametri
    public MusicController(MusicScanner musicScanner, ArtistListView artistListView) {
        this.musicScanner = musicScanner;
        this.artistListView = artistListView;
    }

    // Metodo che esegue la scansione della cartella musicale
    public void scanMusicFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona la cartella musicale");
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            // Eseguire la scansione
            musicScanner.scanDirectory(selectedDirectory.getAbsolutePath());
            artistListView.populateArtists();  // Dopo la scansione, aggiorna la lista degli artisti
        }
    }
}
