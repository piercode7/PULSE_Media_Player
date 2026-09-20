package org.mypulse.view.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.mypulse.model.Album;

import java.io.ByteArrayInputStream;

public class AlbumListCell extends ListCell<Album> {
    // Prefisso usato per riconoscere l'album "finto" di intestazione ("Album trovati: N",
    // aggiunto come primo elemento della lista da AlbumListView) - stesso meccanismo già
    // usato per la lista Artisti, adattato a un ListView<Album> invece che <String>: qui
    // non si può semplicemente aggiungere una stringa, quindi si usa un Album vero e
    // proprio (con solo il nome impostato) riconosciuto da questo prefisso.
    public static final String HEADER_PREFIX = "Album trovati: ";

    private final ImageView imageView = new ImageView();
    private final Label albumLabel = new Label();  // Etichetta per il titolo dell'album
    private final Label artistLabel = new Label(); // Etichetta per l'artista
    private final Label dateGenreLabel = new Label();  // Etichetta per la data e il genere combinati
    private final HBox hbox = new HBox(10);  // HBox principale per l'immagine e le etichette
    private Image defaultImage;

    // Riga di intestazione: etichetta col conteggio + due pulsanti di ordinamento. La
    // cella dell'intestazione NON è più disabilitata (setDisable disabiliterebbe anche
    // questi pulsanti, essendo figli suoi): la non-selezionabilità è garantita altrove,
    // nei listener di selezione di MainView, che ignorano/annullano la selezione se cade
    // su questa riga - vedi il commento lì.
    private final Label headerLabel = new Label();
    private final Button sortByTitleButton = new Button("A-Z");
    private final Button sortByDateButton = new Button("Anno");
    private final HBox headerBox = new HBox(8);

    public AlbumListCell(Runnable onSortByTitle, Runnable onSortByDate) {
        headerLabel.getStyleClass().add("album-cell-title");
        sortByTitleButton.getStyleClass().add("album-sort-button");
        sortByDateButton.getStyleClass().add("album-sort-button");
        sortByTitleButton.setOnAction(e -> onSortByTitle.run());
        sortByDateButton.setOnAction(e -> onSortByDate.run());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(headerLabel, headerSpacer, sortByTitleButton, sortByDateButton);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        buildLabelsAndImage();
    }

    // Costruttore usato dove l'ordinamento non serve (nessuno, per ora tutte le liste
    // album lo usano) - tenuto per non forzare sempre i due Runnable dall'esterno
    public AlbumListCell() {
        this(() -> {}, () -> {});
    }

    private void buildLabelsAndImage() {
        // Configurazione delle etichette: classi invece di un colore fisso inline, così
        // quando la cella è selezionata (.list-cell:selected in dark-theme.css) il testo
        // passa al colore -on-accent invece di restare grigio e poco leggibile sullo
        // sfondo chiaro dell'accento (un colore inline vincerebbe sempre sulla regola CSS)
        albumLabel.getStyleClass().add("album-cell-title");
        artistLabel.getStyleClass().add("album-cell-subtitle");
        dateGenreLabel.getStyleClass().add("album-cell-subtitle");

        // Aggiungi le etichette alla VBox (in ordine verticale)
        // VBox con spaziatura per le etichette
        VBox labelContainer = new VBox(5);
        labelContainer.getChildren().addAll(albumLabel, artistLabel, dateGenreLabel);
        labelContainer.setAlignment(Pos.CENTER_LEFT); // Allinea la VBox a sinistra

        // Aggiungi immagine e VBox alla HBox principale
        hbox.getChildren().addAll(imageView, labelContainer);
        hbox.setAlignment(Pos.CENTER_LEFT); // Allinea verticalmente il tutto al centro sinistra

        // Caricamento dell'immagine di default
        try {
            String imagePath = getClass().getResource("/undefinedAlbum.jpg").toExternalForm();
            defaultImage = new Image(imagePath, 100, 100, true, true);
        } catch (Exception e) {
            System.out.println("Errore nel caricamento dell'immagine di default: " + e.getMessage());
        }
    }

    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);

        if (empty || album == null) {
            setGraphic(null);
            setText(null);
        } else if (album.getName() != null && album.getName().startsWith(HEADER_PREFIX)) {
            // Riga di intestazione: etichetta col conteggio + pulsanti di ordinamento.
            // La cella non va disabilitata (bloccherebbe anche i pulsanti): la
            // non-selezionabilità è gestita nei listener di selezione in MainView.
            headerLabel.setText(album.getName());
            setText(null);
            setGraphic(headerBox);
        } else {
            setText(null); // Una cella riciclata dalla riga di intestazione avrebbe ancora quel testo impostato
            // Use the coverImageData byte array if available, otherwise use coverImagePath
            if (album.getCoverImage() != null) {
                // If coverImageData is not null, use it to set the image
                try {
                    Image albumCover = new Image(new ByteArrayInputStream(album.getCoverImage()), 100, 100, false, true);
                    imageView.setImage(albumCover);
                } catch (Exception e) {
                    imageView.setImage(defaultImage);
                    System.out.println("Errore nel caricamento dell'immagine dalla copertina: " + e.getMessage());
                }
            } else if (album.getCoverImagePath() != null && !album.getCoverImagePath().isEmpty()) {
                // If coverImageData is null but coverImagePath is available, use the path to set the image
                try {
                    Image albumCover = new Image("file:" + album.getCoverImagePath(), 100, 100, false, true);
                    imageView.setImage(albumCover);
                } catch (Exception e) {
                    imageView.setImage(defaultImage);
                    System.out.println("Errore nel caricamento dell'immagine dal percorso: " + e.getMessage());
                }
            } else {
                // If neither coverImageData nor coverImagePath is available, use the default image
                imageView.setImage(defaultImage);
            }

            // Imposta il titolo dell'album
            albumLabel.setText(album.getName());

            // Imposta l'artista
            artistLabel.setText(album.getArtistAlbum());

            // Imposta la data e il genere sulla stessa riga, separati da una virgola
            String releaseDate = (album.getReleaseDate() != null) ? album.getReleaseDate().toString() : "Data sconosciuta";
            String genre = (album.getGenre() != null && !album.getGenre().isEmpty()) ? album.getGenre() : "Genere sconosciuto";
            dateGenreLabel.setText(releaseDate + ", " + genre);

            // Imposta il layout della cella
            setGraphic(hbox);
        }
    }

}
