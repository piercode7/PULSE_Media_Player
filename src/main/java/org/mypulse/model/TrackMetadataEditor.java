package org.mypulse.model;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.mypulse.util.LyricsFetcher;
import org.mypulse.util.ThemeManager;
import org.mypulse.view.MainView;
import org.mypulse.view.components.AllViews;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrackMetadataEditor {
    private List<Track> tracks;
    private Stage metadataStage;
    private TextField titleField;
    private CheckBox titleCheckBox;
    private TextField artistField;
    private CheckBox artistCheckBox;
    private TextField albumField;
    private CheckBox albumCheckBox;
    private TextField albumArtistField;
    private CheckBox albumArtistCheckBox;
    private TextField composerField;
    private CheckBox composerCheckBox;
    private TextField genreField;
    private CheckBox genreCheckBox;
    private TextField yearField;
    private CheckBox yearCheckBox;
    private TextField trackNumberField;
    private CheckBox trackNumberCheckBox;
    private TextField discNumberField;
    private CheckBox discNumberCheckBox;
    private TextArea lyricsArea;
    private CheckBox lyricsCheckBox;
    private ImageView coverImageView;
    private byte[] newCoverImage;
    private Label filePathLabel;
    private Label formatLabel;
    private Label bitrateLabel;
    private MainView mainView;
    private AllViews allView;

    public TrackMetadataEditor(List<Track> tracks, MainView mainView, AllViews allView) {
        this.tracks = tracks;
        this.mainView = mainView;
        this.allView = allView;
        createEditor();
    }

    private void createEditor() {
        metadataStage = new Stage();
        metadataStage.setTitle("Edit Metadata - Multiple Tracks");

        // Create TabPane
        TabPane tabPane = new TabPane();

        // Editable Metadata Tab
        Tab detailsTab = new Tab("Details", createEditableMetadataPane());

        // Lyrics Tab
        Tab lyricsTab = new Tab("Lyrics", createEditableLyricsPane());

        // File Info Tab
        Tab fileInfoTab = new Tab("File Info", createFileInfoPane());
        fileInfoTab.setClosable(false);
        detailsTab.setClosable(false);
        lyricsTab.setClosable(false);

        // Add all tabs to the TabPane
        tabPane.getTabs().addAll(detailsTab, lyricsTab, fileInfoTab);

        // Save and Cancel buttons
        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 10, 10, 10));

        saveButton.setOnAction(event -> saveMetadata());
        cancelButton.setOnAction(event -> metadataStage.close());

        VBox root = new VBox(10, tabPane, buttonBox);
        root.setPadding(new Insets(15));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/dark-editor.css").toExternalForm());
        ThemeManager.applyToScene(scene); // Applica il tema (colori) attualmente scelto
        metadataStage.setScene(scene);

        // Il layout è comunque fisso (campi a larghezza costante, niente che debba
        // ridimensionarsi), quindi non ha senso lasciare la finestra ridimensionabile:
        // si calcola la misura giusta una volta sola e si blocca lì.
        //
        // root.applyCss() + root.layout() PRIMA di sizeToScene() sono fondamentali: senza
        // di essi sizeToScene() misura il contenuto prima che il CSS sia stato applicato
        // (incluso il font per i caratteri giapponesi/cinesi/coreani nella tab Lyrics, che
        // ha metriche diverse da quello di default), quindi calcolava una dimensione più
        // piccola di quella vera - la finestra si apriva comunque troppo piccola.
        root.applyCss();
        root.layout();
        metadataStage.sizeToScene();
        metadataStage.setResizable(false);
    }

    // Collega un campo alla sua checkbox: la spunta si seleziona da sola non appena il
    // testo digitato differisce dal valore originale (una vera modifica), invece di dover
    // spuntarla a mano dopo aver scritto. Si scollega altrettanto da sola se si ritorna
    // al valore originale digitando. Resta però possibile togliere la spunta a mano in
    // qualunque momento (es. non si è convinti di una modifica) senza dover riscrivere il
    // testo precedente: l'ascoltatore reagisce solo a un cambiamento del TESTO, non a un
    // click sulla checkbox, quindi una spunta tolta a mano resta tolta finché non si
    // riprende a digitare in quel campo.
    private void bindCheckboxToChanges(TextInputControl field, CheckBox checkBox, String originalValue) {
        field.textProperty().addListener((obs, oldValue, newValue) ->
                checkBox.setSelected(!newValue.equals(originalValue)));
    }

    private VBox createEditableLyricsPane() {
        VBox lyricsPane = new VBox(10);
        lyricsPane.setPadding(new Insets(15));

        // Create the TextArea for Lyrics
        String originalLyrics = getUnifiedValue(tracks.stream().map(Track::getLyrics).collect(Collectors.toList()));
        lyricsArea = new TextArea(originalLyrics);
        lyricsArea.setPrefHeight(500); // Set preferred height for TextArea
        lyricsArea.setEditable(true); // Allow editing

        lyricsCheckBox = new CheckBox("Modifica i testi");
        bindCheckboxToChanges(lyricsArea, lyricsCheckBox, originalLyrics);

        // Pulsante per recuperare il testo da Genius, come nella finestra dedicata
        // (LyricsController): prima qui non c'era alcun modo per scaricarlo, solo per
        // vedere/modificare quello già presente nei metadati
        Button fetchLyricsButton = new Button("Fetch Lyrics");
        fetchLyricsButton.setOnAction(event -> fetchLyricsFromGenius());

        // Add components to the lyrics pane
        lyricsPane.getChildren().addAll(new Label("Lyrics:"), fetchLyricsButton, lyricsArea, lyricsCheckBox);
        return lyricsPane;
    }

    // Cerca il testo su Genius per il titolo/artista attualmente nei campi della tab
    // Details, e lo mette nella TextArea (che fa scattare da sola la checkbox "Modifica i
    // testi" tramite bindCheckboxToChanges, visto che il testo cambia). Ha senso solo con
    // un titolo e un artista univoci: se si stanno modificando più brani con valori
    // diversi per quei campi (mostrano "*"), non c'è un singolo brano per cui cercare.
    private void fetchLyricsFromGenius() {
        String titleValue = titleField.getText();
        String artistValue = artistField.getText();

        if (titleValue == null || titleValue.isEmpty() || titleValue.equals("*")
                || artistValue == null || artistValue.isEmpty() || artistValue.equals("*")) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Titolo o artista non univoci");
            alert.setHeaderText(null);
            alert.setContentText("Per cercare il testo serve un titolo e un artista univoci. "
                    + "Stai modificando più brani con valori diversi per questi campi (mostrano \"*\").");
            alert.showAndWait();
            return;
        }

        String fetched = LyricsFetcher.fetchLyrics(artistValue, titleValue);
        if (fetched != null) {
            lyricsArea.setText(fetched);
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Testo non trovato");
            alert.setHeaderText(null);
            alert.setContentText("Non è stato possibile trovare il testo per \"" + titleValue + "\" di " + artistValue + ".");
            alert.showAndWait();
        }
    }

    private GridPane createEditableMetadataPane() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(15));

        // Imposta la larghezza preferita
        double fieldWidth = 550; // Imposta la larghezza desiderata

        // Editable Fields with Checkboxes. Ogni campo tiene il proprio valore originale e
        // collega la checkbox tramite bindCheckboxToChanges(): si seleziona da sola quando
        // si scrive qualcosa di diverso dall'originale, invece di doverla spuntare a mano.
        String originalTitle = getUnifiedValue(tracks.stream().map(Track::getTitle).collect(Collectors.toList()));
        titleField = new TextField(originalTitle);
        titleField.setPrefWidth(fieldWidth); // Imposta la larghezza
        titleCheckBox = new CheckBox("");
        bindCheckboxToChanges(titleField, titleCheckBox, originalTitle);

        String originalArtist = getUnifiedValue(tracks.stream().map(Track::getArtist).collect(Collectors.toList()));
        artistField = new TextField(originalArtist);
        artistField.setPrefWidth(fieldWidth);
        artistCheckBox = new CheckBox("");
        bindCheckboxToChanges(artistField, artistCheckBox, originalArtist);

        String originalAlbum = getUnifiedValue(tracks.stream().map(Track::getAlbumName).collect(Collectors.toList()));
        albumField = new TextField(originalAlbum);
        albumField.setPrefWidth(fieldWidth);
        albumCheckBox = new CheckBox("");
        bindCheckboxToChanges(albumField, albumCheckBox, originalAlbum);

        String originalAlbumArtist = getUnifiedValue(tracks.stream().map(Track::getArtistAlbum).collect(Collectors.toList()));
        albumArtistField = new TextField(originalAlbumArtist);
        albumArtistField.setPrefWidth(fieldWidth);
        albumArtistCheckBox = new CheckBox("");
        bindCheckboxToChanges(albumArtistField, albumArtistCheckBox, originalAlbumArtist);

        String originalComposer = getUnifiedValue(tracks.stream().map(Track::getComposer).collect(Collectors.toList()));
        composerField = new TextField(originalComposer);
        composerField.setPrefWidth(fieldWidth);
        composerCheckBox = new CheckBox("");
        bindCheckboxToChanges(composerField, composerCheckBox, originalComposer);

        String originalGenre = getUnifiedValue(tracks.stream().map(Track::getGenre).collect(Collectors.toList()));
        genreField = new TextField(originalGenre);
        genreField.setPrefWidth(fieldWidth);
        genreCheckBox = new CheckBox("");
        bindCheckboxToChanges(genreField, genreCheckBox, originalGenre);

        String originalYear = getUnifiedValue(tracks.stream().map(track -> track.getReleaseYear() != null ? track.getReleaseYear().toString() : "").collect(Collectors.toList()));
        yearField = new TextField(originalYear);
        yearField.setPrefWidth(fieldWidth);
        yearCheckBox = new CheckBox("");
        bindCheckboxToChanges(yearField, yearCheckBox, originalYear);

        String originalTrackNumber = getUnifiedValue(tracks.stream().map(track -> track.getTrackNumber() != null ? track.getTrackNumber().toString() : "").collect(Collectors.toList()));
        trackNumberField = new TextField(originalTrackNumber);
        trackNumberField.setPrefWidth(fieldWidth);
        trackNumberCheckBox = new CheckBox("");
        bindCheckboxToChanges(trackNumberField, trackNumberCheckBox, originalTrackNumber);

        String originalDiscNumber = getUnifiedValue(tracks.stream().map(track -> track.getDiscNumber() != null ? track.getDiscNumber().toString() : "").collect(Collectors.toList()));
        discNumberField = new TextField(originalDiscNumber);
        discNumberField.setPrefWidth(fieldWidth);
        discNumberCheckBox = new CheckBox("");
        bindCheckboxToChanges(discNumberField, discNumberCheckBox, originalDiscNumber);

        // Cover Image. Stessa dimensione (300x300) della copertina nella finestra
        // principale, per coerenza visiva, e riempie lo spazio vuoto che restava sotto i
        // campi in questa tab (l'altezza della finestra è condivisa con la tab Lyrics,
        // che ha bisogno di più spazio per la TextArea).
        coverImageView = new ImageView();
        coverImageView.setFitWidth(300);
        coverImageView.setFitHeight(300);
        coverImageView.setPreserveRatio(true);

        // Mostra sempre la copertina del primo brano come anteprima, non solo se tutti i
        // brani selezionati hanno esattamente la stessa immagine: prima il controllo
        // confrontava gli array di byte con equals() di default (identità di riferimento),
        // quindi con più di un brano selezionato non risultava praticamente mai vera anche
        // quando le copertine erano di fatto identiche - l'anteprima restava sempre vuota
        // quando si modificava un album.
        if (tracks.get(0).getCoverImage() != null) {
            coverImageView.setImage(new Image(new ByteArrayInputStream(tracks.get(0).getCoverImage())));
        }

        Button changeCoverButton = new Button("Change Cover");
        changeCoverButton.setOnAction(event -> changeCoverImage());

        // Add fields and checkboxes to grid
        gridPane.add(new Label("Titolo:"), 0, 0);
        gridPane.add(titleField, 1, 0);
        gridPane.add(titleCheckBox, 2, 0);
        gridPane.add(new Label("Artista:"), 0, 1);
        gridPane.add(artistField, 1, 1);
        gridPane.add(artistCheckBox, 2, 1);
        gridPane.add(new Label("Album:"), 0, 2);
        gridPane.add(albumField, 1, 2);
        gridPane.add(albumCheckBox, 2, 2);
        gridPane.add(new Label("Artist album:"), 0, 3);
        gridPane.add(albumArtistField, 1, 3);
        gridPane.add(albumArtistCheckBox, 2, 3);
        gridPane.add(new Label("Compositore:"), 0, 4);
        gridPane.add(composerField, 1, 4);
        gridPane.add(composerCheckBox, 2, 4);
        gridPane.add(new Label("Genere:"), 0, 5);
        gridPane.add(genreField, 1, 5);
        gridPane.add(genreCheckBox, 2, 5);
        gridPane.add(new Label("Anno:"), 0, 6);
        gridPane.add(yearField, 1, 6);
        gridPane.add(yearCheckBox, 2, 6);
        gridPane.add(new Label("Numero track:"), 0, 7);
        gridPane.add(trackNumberField, 1, 7);
        gridPane.add(trackNumberCheckBox, 2, 7);
        gridPane.add(new Label("Numero disco:"), 0, 8);
        gridPane.add(discNumberField, 1, 8);
        gridPane.add(discNumberCheckBox, 2, 8);
        gridPane.add(new Label("Cover:"), 0, 9);
        gridPane.add(coverImageView, 1, 9);
        gridPane.add(changeCoverButton, 1, 10);

        return gridPane;
    }


    private VBox createFileInfoPane() {
        VBox fileInfoPane = new VBox(10);
        fileInfoPane.setPadding(new Insets(15));

        // Crea una TextArea per mostrare i dettagli dei file selezionati
        TextArea fileInfoTextArea = new TextArea();
        fileInfoTextArea.setPrefHeight(535); // Imposta un'altezza prefissata per la TextArea
        fileInfoTextArea.setEditable(false); // Rendi la TextArea non modificabile

        // Popola la TextArea con i dettagli di ogni traccia
        StringBuilder fileDetails = new StringBuilder();
        tracks.forEach(track -> {
            fileDetails.append("File Path: ").append(track.getFilePath() != null ? track.getFilePath() : "N/A").append("\n");
            fileDetails.append("Format: ").append(track.getFormat() != null ? track.getFormat() : "N/A").append("\n");
            fileDetails.append("Bitrate: ").append(track.getBitrate() != null ? track.getBitrate() + " kbps" : "N/A").append("\n");
            fileDetails.append("\n");
        });

        fileInfoTextArea.setText(fileDetails.toString()); // Imposta il testo nella TextArea

        // Aggiungi la TextArea alla pagina dei dettagli
        fileInfoPane.getChildren().addAll(new Label("Informazioni File:"), fileInfoTextArea);
        return fileInfoPane;
    }




    private void changeCoverImage() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Scegli la nuova cover");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.jpeg"));
            File selectedFile = fileChooser.showOpenDialog(metadataStage);
            if (selectedFile != null) {
                Image newImage = new Image(selectedFile.toURI().toString());
                coverImageView.setImage(newImage);

                // Converti la nuova immagine in un array di byte
                newCoverImage = Files.readAllBytes(selectedFile.toPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveMetadata() {
        // Nome dell'album da usare per aggiornare la tabella dopo il salvataggio: se il
        // campo Album non è stato effettivamente modificato (checkbox non spuntata, o
        // spuntata ma il testo è rimasto "*" perché i brani avevano album diversi),
        // albumField.getText() può valere "*" e non un nome di album reale
        String newAlbum = (albumCheckBox.isSelected() && !albumField.getText().equals("*"))
                ? albumField.getText() : tracks.get(0).getAlbumName();

        // Prompt user to choose between modifying file metadata or only updating the track instances
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salva metadati");
        alert.setHeaderText("Vuoi aggiornare solo la libreria o anche i metadati del file?");

        ButtonType modifyFileButton = new ButtonType("Libreria e metadati");
        ButtonType updateInstanceButton = new ButtonType("Libreria");
        ButtonType cancelButton = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(modifyFileButton, updateInstanceButton, cancelButton);

        alert.showAndWait().ifPresent(type -> {
            if (type == modifyFileButton) {
                // Modify file metadata and update the library
                modifyFileMetadata();
                createNewTrackInstances();
                mainView.autoSaveLibrary();
            } else if (type == updateInstanceButton) {
                // Only create new track instances without modifying the file
                createNewTrackInstances();
                mainView.autoSaveLibrary();
            } else {
                // Annulla: non è stata fatta nessuna modifica, non c'è nulla da salvare
                return;
            }

            // Refresh the album details
            mainView.refreshAlbumDetail();

            // Ottieni la selezione corrente dal menu
            String selectedMenu = mainView.getListViewMenu().getSelectionModel().getSelectedItem();

            // Aggiorna la tabella in base alla selezione attuale
            if ("Artisti".equals(selectedMenu) || "Album".equals(selectedMenu)) {
                // Aggiorna la tabella dei brani per l'album corrente
                mainView.getTrackTableView().populateTracksByAlbum(newAlbum);
                mainView.getTableViewTracks().refresh();
            } else if ("Coda".equals(selectedMenu)) {
                // Aggiorna la tabella della coda
                mainView.getTrackTableView().populateAllTracksInQueue();
                mainView.getTableViewTrackAllInQueue().refresh();
            } else {
                // Aggiorna la tabella estesa con tutti i brani
                mainView.getTrackTableView().populateExtendedTrackTable();
                mainView.getTableViewTrackAll().refresh();
            }

            // Chiudi la finestra di modifica dei metadati
            metadataStage.close();
        });
    }


    private void createNewTrackInstances() {
        // Il brano attualmente in riproduzione (e la coda) sono tracciati per identità di
        // oggetto: ricreando le istanze qui sotto, senza questa mappa resterebbero agganciati
        // all'istanza vecchia appena rimossa dalla libreria (evidenziazione della riga persa,
        // riproduzione della coda arenata). Il filePath non è modificabile in questo editor,
        // quindi è una chiave stabile per ritrovare la nuova istanza corrispondente.
        Map<String, Track> newTrackByPath = new HashMap<>();

        for (Track track : tracks) {
            // Remove the old track from the library
            mainView.getMusicLibrary().removeTrack(track);

            // Retrieve the album associated with the track before making changes
            Album oldAlbum = mainView.getMusicLibrary().getAlbumByName(track.getAlbumName());

            // Remove the track from the album
            if (oldAlbum != null) {
                oldAlbum.removeTrack(track);

                // If the album has no more tracks, remove the album from the library
                if (oldAlbum.getTracks().isEmpty()) {
                    mainView.getMusicLibrary().removeAlbum(oldAlbum);
                }
            }

            // Collect metadata fields for the new Track.
            // Se i brani selezionati avevano valori diversi per un campo, il campo mostra
            // "*" (vedi getUnifiedValue): applicarlo alla cieca quando la checkbox è
            // spuntata ma il testo non è stato toccato scriverebbe "*" come valore reale
            // in libreria. Va applicato solo un valore effettivamente digitato dall'utente.
            String title = (titleCheckBox.isSelected() && !titleField.getText().equals("*"))
                    ? titleField.getText() : track.getTitle();
            Integer trackNumber = track.getTrackNumber();
            try {
                if (trackNumberCheckBox.isSelected()) {
                    trackNumber = !trackNumberField.getText().isEmpty() ? Integer.parseInt(trackNumberField.getText()) : track.getTrackNumber();
                }
            } catch (NumberFormatException e) {
                trackNumber = track.getTrackNumber();
            }

            Integer discNumber = track.getDiscNumber();
            try {
                if (discNumberCheckBox.isSelected()) {
                    discNumber = !discNumberField.getText().isEmpty() ? Integer.parseInt(discNumberField.getText()) : track.getDiscNumber();
                }
            } catch (NumberFormatException e) {
                discNumber = track.getDiscNumber();
            }

            String filePath = track.getFilePath();
            Integer duration = track.getDuration();

            String genre = (genreCheckBox.isSelected() && !genreField.getText().equals("*"))
                    ? genreField.getText() : track.getGenre();
            String artistAlbum = (albumArtistCheckBox.isSelected() && !albumArtistField.getText().equals("*"))
                    ? albumArtistField.getText() : track.getArtistAlbum();
            String artist = (artistCheckBox.isSelected() && !artistField.getText().equals("*"))
                    ? artistField.getText() : track.getArtist();
            String albumName = (albumCheckBox.isSelected() && !albumField.getText().equals("*"))
                    ? albumField.getText() : track.getAlbumName();

            Integer releaseYear = track.getReleaseYear();
            try {
                if (yearCheckBox.isSelected()) {
                    releaseYear = !yearField.getText().isEmpty() ? Integer.parseInt(yearField.getText()) : track.getReleaseYear();
                }
            } catch (NumberFormatException e) {
                releaseYear = track.getReleaseYear();
            }

            byte[] coverImage = newCoverImage != null ? newCoverImage : track.getCoverImage();

            // Retrieve or create the album
            Album album = mainView.getMusicLibrary().getAlbumByName(albumName);
            if (album == null) {
                // Nuovo album (es. rinominato): non esiste ancora un coverImagePath su
                // disco per questo nome, il percorso del file mp3 non è quella cosa
                album = new Album(albumName, artistAlbum, coverImage, null);
                mainView.getMusicLibrary().addAlbum(album);
            } else {
                // L'album esisteva già con questo nome (caso comune: si è modificato solo
                // l'artista, non il nome dell'album). Senza questo l'artista nuovo veniva
                // scritto solo sulle nuove istanze Track qui sotto, ma l'oggetto Album -
                // da cui la lista a sinistra legge il nome dell'artista mostrato - restava
                // con il valore vecchio per sempre, anche dopo il salvataggio.
                album.setArtistAlbum(artistAlbum);
            }

            // If the cover was changed, update the album's cover
            if (newCoverImage != null) {
                album.setCoverImage(newCoverImage);
            }

            String lyrics = (lyricsCheckBox.isSelected() && !lyricsArea.getText().equals("*"))
                    ? lyricsArea.getText() : track.getLyrics();
            String composer = (composerCheckBox.isSelected() && !composerField.getText().equals("*"))
                    ? composerField.getText() : track.getComposer();

            // Create a new Track instance using the constructor
            Track newTrack = new Track(title, trackNumber, discNumber, filePath, duration, genre, artistAlbum, artist, albumName, releaseYear, coverImage, album, lyrics, composer);

            // Il costruttore non porta bitrate/formato (letti dallo scanner, non modificabili
            // qui) né il play count: senza riportarli esplicitamente si perdevano ad ogni salvataggio
            newTrack.setBitrate(track.getBitrate());
            newTrack.setFormat(track.getFormat());
            newTrack.setPlayCount(track.getPlayCount());

            // Add the new track to the library and album
            mainView.getMusicLibrary().addTrack(newTrack);
            album.addTrack(newTrack);

            newTrackByPath.put(filePath, newTrack);
        }

        // Se il brano in riproduzione è tra quelli appena modificati, aggancia il
        // riferimento e la coda alla nuova istanza (vedi commento sopra)
        Track currentlyPlaying = mainView.getCurrentlyPlayingTrack();
        if (currentlyPlaying != null && newTrackByPath.containsKey(currentlyPlaying.getFilePath())) {
            mainView.setCurrentlyPlayingTrack(newTrackByPath.get(currentlyPlaying.getFilePath()));
        }
        List<Track> queuedTracks = mainView.getQueueTracks();
        for (int i = 0; i < queuedTracks.size(); i++) {
            Track replacement = newTrackByPath.get(queuedTracks.get(i).getFilePath());
            if (replacement != null) {
                queuedTracks.set(i, replacement);
            }
        }
    }

    private String getUnifiedValue(List<String> values) {
        return values.stream().distinct().count() == 1 ? values.get(0) : "*";
    }

    private void modifyFileMetadata() {
        for (Track track : tracks) {
            try {
                // Utilizza Jaudiotagger per aggiornare i metadati normali
                File audioFile = new File(track.getFilePath());
                AudioFile f = AudioFileIO.read(audioFile);
                Tag tag = f.getTag();

                if (tag != null) {
                    if (titleCheckBox.isSelected() && !titleField.getText().equals("*")) {
                        tag.setField(FieldKey.TITLE, titleField.getText());
                    }
                    if (artistCheckBox.isSelected() && !artistField.getText().equals("*")) {
                        tag.setField(FieldKey.ARTIST, artistField.getText());
                    }
                    if (albumCheckBox.isSelected() && !albumField.getText().equals("*")) {
                        tag.setField(FieldKey.ALBUM, albumField.getText());
                    }
                    if (albumArtistCheckBox.isSelected() && !albumArtistField.getText().equals("*")) {
                        tag.setField(FieldKey.ALBUM_ARTIST, albumArtistField.getText());
                    }
                    if (composerCheckBox.isSelected() && !composerField.getText().equals("*")) {
                        tag.setField(FieldKey.COMPOSER, composerField.getText());
                    }
                    if (genreCheckBox.isSelected() && !genreField.getText().equals("*")) {
                        tag.setField(FieldKey.GENRE, genreField.getText());
                    }
                    if (yearCheckBox.isSelected() && !yearField.getText().equals("*")) {
                        tag.setField(FieldKey.YEAR, yearField.getText());
                    }
                    if (trackNumberCheckBox.isSelected() && !trackNumberField.getText().equals("*")) {
                        tag.setField(FieldKey.TRACK, trackNumberField.getText());
                    }
                    if (discNumberCheckBox.isSelected() && !discNumberField.getText().equals("*")) {
                        tag.setField(FieldKey.DISC_NO, discNumberField.getText());
                    }
                    if (lyricsCheckBox.isSelected() && !lyricsArea.getText().equals("*")) {
                        tag.setField(FieldKey.LYRICS, lyricsArea.getText());
                    }

                    // Salva i metadati aggiornati utilizzando Jaudiotagger
                    f.commit();
                }

                // Utilizza MP3agic per aggiornare la cover image
                if (newCoverImage != null) {
                    Mp3File mp3File = new Mp3File(track.getFilePath());
                    ID3v2 id3v2Tag;
                    if (mp3File.hasId3v2Tag()) {
                        id3v2Tag = mp3File.getId3v2Tag();
                    } else {
                        // Crea un nuovo tag ID3v2 se non esiste
                        id3v2Tag = new ID3v24Tag();
                        mp3File.setId3v2Tag(id3v2Tag);
                    }

                    // Imposta la nuova cover image con MP3agic
                    id3v2Tag.setAlbumImage(newCoverImage, "image/jpeg");

                    // Salva il file aggiornato con MP3agic (sovrascrivi il file originale)
                    String originalFilePath = track.getFilePath();
                    mp3File.save(originalFilePath + "_temp");

                    // Sovrascrivi il file originale con il file temporaneo
                    File tempFile = new File(originalFilePath + "_temp");
                    File originalFile = new File(originalFilePath);
                    if (originalFile.delete()) {
                        tempFile.renameTo(originalFile);
                        System.out.println("Cover aggiornata con successo: " + originalFilePath);
                    } else {
                        System.out.println("Errore durante l'aggiornamento della cover.");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void show() {
        metadataStage.show();
    }
}