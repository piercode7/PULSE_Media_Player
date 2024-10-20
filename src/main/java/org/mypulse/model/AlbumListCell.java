package org.mypulse.model;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.mypulse.model.Album;

import java.io.ByteArrayInputStream;

public class AlbumListCell extends ListCell<Album> {
    private final ImageView imageView = new ImageView();
    private final Label albumLabel = new Label();  // Etichetta per il titolo dell'album
    private final Label artistLabel = new Label(); // Etichetta per l'artista
    private final Label dateGenreLabel = new Label();  // Etichetta per la data e il genere combinati
    private final HBox hbox = new HBox(10);  // HBox principale per l'immagine e le etichette
    private Image defaultImage;

    public AlbumListCell() {
        // Configurazione delle etichette
        albumLabel.setStyle("-fx-font-weight: bold;"); // Grassetto per il titolo dell'album
        artistLabel.setStyle("-fx-text-fill: gray;");  // Colore grigio per l'artista
        dateGenreLabel.setStyle("-fx-text-fill: gray;");  // Colore grigio per la data e il genere

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
        } else {
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
