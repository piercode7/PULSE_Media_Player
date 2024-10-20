package org.mypulse.view.components;

import org.mypulse.model.Album;
import org.mypulse.model.AlbumListCell;
import org.mypulse.view.MainView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AlbumListView {
    private final MainView mainView;

    // Costruttore che accetta un'istanza di MainView
    public AlbumListView(MainView mainView) {
        this.mainView = mainView;
    }

    // Metodo per popolare gli album
    public void populateAlbums() {
        // Accedi agli elementi tramite mainView
        mainView.getListViewAlbumAll().getItems().clear();
        List<Album> albumList = new ArrayList<>(mainView.getMusicLibrary().getAlbums());
        albumList.sort(Comparator.comparing(Album::getName));

        // Stampa il numero di album trovati
        System.out.println("Album trovati: " + albumList.size());

        // Aggiungi gli album alla lista
        mainView.getListViewAlbumAll().getItems().addAll(albumList);
        mainView.getListViewAlbumAll().setCellFactory(param -> new AlbumListCell());
    }



    public void populateAlbumsByArtist(String artist) {
        mainView.listViewAlbum.getItems().clear();
        List<Album> albumList = mainView.getMusicLibrary().getAlbumsByArtist(artist);

        // Ordina la lista, ignorando i valori nulli mettendoli per ultimi
        albumList.sort(Comparator.comparing(Album::getName, Comparator.nullsLast(String::compareTo)));

        // Stampa solo il numero di album trovati
        System.out.println("Numero di album trovati per " + artist + ": " + albumList.size());

        mainView.listViewAlbum.getItems().addAll(albumList);
        mainView.listViewAlbum.setCellFactory(param -> new AlbumListCell());
    }

}
