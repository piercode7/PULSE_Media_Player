package org.mypulse.view.components;

import org.mypulse.model.Album;
import org.mypulse.model.AlbumListCell;
import org.mypulse.view.MainView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AlbumListView {
    private final MainView mainView;

    // Modalità di ordinamento scelta dai due pulsanti nell'intestazione della lista
    // ("A-Z" / "Anno"). Le due liste album (tutti gli album, e quelli di un artista)
    // ricordano la propria scelta indipendentemente, così cambiare vista non la
    // resetta a sorpresa.
    public enum AlbumSortMode {
        TITLE, DATE
    }

    private AlbumSortMode allAlbumsSortMode = AlbumSortMode.TITLE;
    private AlbumSortMode artistAlbumsSortMode = AlbumSortMode.TITLE;
    // Ultimo artista mostrato: serve per poter ripopolare la lista con un nuovo
    // ordinamento senza dover ripassare l'artista da fuori ogni volta (i pulsanti di
    // ordinamento richiamano populate* senza argomenti aggiuntivi)
    private String currentArtist;

    // Costruttore che accetta un'istanza di MainView
    public AlbumListView(MainView mainView) {
        this.mainView = mainView;
    }

    private static Comparator<Album> comparatorFor(AlbumSortMode sortMode) {
        return sortMode == AlbumSortMode.DATE
                ? Comparator.comparing(Album::getReleaseDate, Comparator.nullsLast(Integer::compareTo))
                : Comparator.comparing(Album::getName, Comparator.nullsLast(String::compareTo));
    }

    // Metodo per popolare gli album
    public void populateAlbums() {
        populateAlbums(allAlbumsSortMode);
    }

    public void populateAlbums(AlbumSortMode sortMode) {
        this.allAlbumsSortMode = sortMode;

        // Accedi agli elementi tramite mainView
        mainView.getListViewAlbumAll().getItems().clear();
        List<Album> albumList = new ArrayList<>(mainView.getMusicLibrary().getAlbums());
        albumList.sort(comparatorFor(sortMode));

        // Stampa il numero di album trovati
        System.out.println("Album trovati: " + albumList.size());

        // Intestazione non selezionabile con il conteggio e i due pulsanti di
        // ordinamento, come "Artisti trovati: N" nella lista Artisti - qui non si può
        // aggiungere una semplice stringa (la lista è tipizzata su Album), quindi si usa
        // un Album "finto" con solo il nome impostato, riconosciuto da AlbumListCell
        // tramite HEADER_PREFIX
        mainView.getListViewAlbumAll().getItems().add(
                new Album(AlbumListCell.HEADER_PREFIX + albumList.size(), null, null, null));

        // Aggiungi gli album alla lista
        mainView.getListViewAlbumAll().getItems().addAll(albumList);
        mainView.getListViewAlbumAll().setCellFactory(param -> new AlbumListCell(
                () -> populateAlbums(AlbumSortMode.TITLE),
                () -> populateAlbums(AlbumSortMode.DATE)));
    }



    public void populateAlbumsByArtist(String artist) {
        populateAlbumsByArtist(artist, artistAlbumsSortMode);
    }

    public void populateAlbumsByArtist(String artist, AlbumSortMode sortMode) {
        this.currentArtist = artist;
        this.artistAlbumsSortMode = sortMode;

        mainView.listViewAlbum.getItems().clear();
        List<Album> albumList = mainView.getMusicLibrary().getAlbumsByArtist(artist);
        albumList.sort(comparatorFor(sortMode));

        // Stampa solo il numero di album trovati
        System.out.println("Numero di album trovati per " + artist + ": " + albumList.size());

        // Stessa intestazione non selezionabile di populateAlbums() qui sopra: era
        // rimasta fuori, questa è una lista diversa (album del singolo artista, non
        // "tutti gli album") con un proprio metodo di popolamento separato
        mainView.listViewAlbum.getItems().add(
                new Album(AlbumListCell.HEADER_PREFIX + albumList.size(), null, null, null));

        mainView.listViewAlbum.getItems().addAll(albumList);
        mainView.listViewAlbum.setCellFactory(param -> new AlbumListCell(
                () -> populateAlbumsByArtist(currentArtist, AlbumSortMode.TITLE),
                () -> populateAlbumsByArtist(currentArtist, AlbumSortMode.DATE)));
    }

}
