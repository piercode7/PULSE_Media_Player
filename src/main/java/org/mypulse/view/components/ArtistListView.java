package org.mypulse.view.components;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.mypulse.Main;
import org.mypulse.model.Album;
import org.mypulse.model.MusicLibrary;
import org.mypulse.view.MainView;

import java.util.List;

public class ArtistListView {
    private MainView mainView;
    private final MusicLibrary musicLibrary;

    // Costruttore per inizializzare listViewArtist e musicLibrary
    public ArtistListView(MainView mainView, MusicLibrary musicLibrary) {
        this.mainView = mainView;
        this.musicLibrary = musicLibrary;
    }

    public void populateArtists() {
        mainView.listViewArtist.getItems().clear();

        // Recupera la lista di artisti dalla libreria
        List<String> artistList = musicLibrary.getAllArtists();
        artistList.sort(String::compareTo);

        // Aggiungi un titolo con il numero di artisti
        String header = "Artisti trovati: " + artistList.size();
        System.out.println("Artisti trovati: " + artistList.size());

        mainView.listViewArtist.getItems().add(header); // Aggiungi il titolo come primo elemento della lista

        // Aggiungi il resto degli artisti
        mainView.listViewArtist.getItems().addAll(artistList);

        // Imposta il primo elemento (il titolo) come non selezionabile
        mainView.listViewArtist.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item);

                    // Rendi il titolo non cliccabile (disabilita la cella se è il titolo)
                    if (item.startsWith("Artisti trovati:")) {
                        setDisable(true);
                    } else {
                        setDisable(false); // Le altre celle rimangono abilitabili
                    }
                }
            }
        });
    }



}
