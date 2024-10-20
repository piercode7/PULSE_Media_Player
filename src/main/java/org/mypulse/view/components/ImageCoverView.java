package org.mypulse.view.components;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.mypulse.model.Album;
import org.mypulse.model.SpotifyAPI;
import org.mypulse.model.Track;
import org.mypulse.util.Utils;
import org.mypulse.view.MainView;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

public class ImageCoverView {
    private final MainView mainView;

    public ImageCoverView(MainView mainView) {
        this.mainView = mainView;
    }

    public void editAlbumCover(Album album) {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Modifica Copertina");
            alert.setHeaderText("Come vuoi aggiornare la copertina dell'album?");
            alert.setContentText("Scegli l'opzione:");

            ButtonType manualButton = new ButtonType("Carica manualmente");
            ButtonType onlineButton = new ButtonType("Cerca online");
            ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(manualButton, onlineButton, cancelButton);

            alert.showAndWait().ifPresent(type -> {
                if (type == manualButton) {
                    uploadCoverManually(album);
                } else if (type == onlineButton) {
                    searchAlbumCoverOnline(album);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadCoverManually(Album album) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Scegli una nuova copertina per l'album");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("File immagine", "*.jpg", "*.png", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                byte[] newCoverImage = Files.readAllBytes(selectedFile.toPath());
                updateAlbumCover(album, newCoverImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void searchAlbumCoverOnline(Album album) {
        try {
            SpotifyAPI spotifyAPI = new SpotifyAPI();
            String accessToken = spotifyAPI.getAccessToken();
            String imageUrl = spotifyAPI.getAlbumCoverUrl(accessToken, album.getName(), album.getArtistName());

            if (imageUrl != null) {
                ImageView coverPreview = new ImageView(new Image(imageUrl, 400, 400, true, true));
                Alert previewAlert = Utils.createAlert(Alert.AlertType.CONFIRMATION, "Conferma Copertina", "Questa è la copertina trovata. Vuoi usarla?", null);
                previewAlert.getDialogPane().setContent(coverPreview);

                ButtonType confirmButton = new ButtonType("Conferma");
                ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
                previewAlert.getButtonTypes().setAll(confirmButton, cancelButton);

                previewAlert.showAndWait().ifPresent(type -> {
                    if (type == confirmButton) {
                        try {
                            byte[] newCoverImage = downloadImageFromURL(imageUrl);
                            updateAlbumCover(album, newCoverImage);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Alert errorAlert = Utils.createAlert(Alert.AlertType.ERROR, "Errore", "Errore durante il download", "Non è stato possibile scaricare l'immagine.");
                            errorAlert.showAndWait();
                        }
                    }
                });
            } else {
                Alert errorAlert = Utils.createAlert(Alert.AlertType.ERROR, "Errore", "Copertina non trovata", "Non è stato possibile trovare la copertina dell'album online.");
                errorAlert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert errorAlert = Utils.createAlert(Alert.AlertType.ERROR, "Errore", "Errore durante la ricerca online", "Si è verificato un errore durante la ricerca della copertina online.");
            errorAlert.showAndWait();
        }
    }

    private byte[] downloadImageFromURL(String imageUrl) throws IOException {
        try (InputStream in = new URL(imageUrl).openStream()) {
            return in.readAllBytes();
        }
    }

    private void updateAlbumCover(Album album, byte[] newCoverImage) {
        album.setCoverImage(newCoverImage);
        for (Track track : mainView.getMusicLibrary().getTracksByAlbum(album.getName())) {
            track.setCoverImage(newCoverImage);
        }

        Alert alert = Utils.createAlert(Alert.AlertType.CONFIRMATION, "Modifica Metadati", "Vuoi aggiornare anche i metadati effettivi dei brani associati?", "Questa azione modificherà permanentemente i file audio.");
        ButtonType modifyFileButton = new ButtonType("Modifica file");
        ButtonType skipButton = new ButtonType("Salta");
        ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(modifyFileButton, skipButton, cancelButton);

        alert.showAndWait().ifPresent(type -> {
            if (type == modifyFileButton) {
                for (Track track : mainView.getMusicLibrary().getTracksByAlbum(album.getName())) {
                    modifyTrackMetadataCover(track, newCoverImage);
                }
            }

            // Update the album view
            updateAlbumCoverView(album);
            mainView.getAllViews().refreshAllViews();
        });
    }

    private void updateAlbumCoverView(Album album) {
        if (album.getCoverImage() != null) {
            mainView.getAlbumCoverView().setImage(new Image(new ByteArrayInputStream(album.getCoverImage()), 300, 300, true, false));
        } else if (album.getCoverImagePath() != null && !album.getCoverImagePath().isEmpty()) {
            mainView.getAlbumCoverView().setImage(new Image("file:" + album.getCoverImagePath(), 300, 300, true, false));
        } else {
            mainView.getAlbumCoverView().setImage(mainView.getDefaultImage());
        }
    }

    private void modifyTrackMetadataCover(Track track, byte[] newCoverImage) {
        try {
            Mp3File mp3File = new Mp3File(track.getFilePath());
            ID3v2 id3v2Tag;
            if (mp3File.hasId3v2Tag()) {
                id3v2Tag = mp3File.getId3v2Tag();
            } else {
                id3v2Tag = new ID3v24Tag();
                mp3File.setId3v2Tag(id3v2Tag);
            }

            id3v2Tag.setAlbumImage(newCoverImage, "image/jpeg");

            String originalFilePath = track.getFilePath();
            mp3File.save(originalFilePath + "_temp");

            File tempFile = new File(originalFilePath + "_temp");
            File originalFile = new File(originalFilePath);
            if (originalFile.delete()) {
                tempFile.renameTo(originalFile);
                System.out.println("Cover aggiornata nei metadati: " + originalFilePath);
            } else {
                System.out.println("Errore durante l'aggiornamento della cover nei metadati.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
