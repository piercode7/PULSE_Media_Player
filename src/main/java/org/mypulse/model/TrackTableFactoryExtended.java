package org.mypulse.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class TrackTableFactoryExtended {

    // Metodo per creare la prima tabella (standard con colonne estese)
    public static TableView<Track> createExtendedTrackTable() {
        return createCustomExtendedTrackTable(false);
    }

    // Metodo per creare la seconda tabella (per tutti i brani con colonne estese)
    public static TableView<Track> createExtendedTrackTableAll() {
        return createCustomExtendedTrackTable(true);
    }

    // Metodo personalizzato per creare una tabella in base a un flag con colonne estese
    private static TableView<Track> createCustomExtendedTrackTable(boolean isForAllTracks) {
        TableView<Track> tableViewTracks = new TableView<>();
        tableViewTracks.setPlaceholder(new javafx.scene.control.Label("Nessun brano disponibile"));
        tableViewTracks.setVisible(false); // Nascondi la tabella finché non viene selezionato un album o brani
        tableViewTracks.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableViewTracks.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Definizione delle colonne standard
        TableColumn<Track, String> trackNumberColumn = new TableColumn<>("Indice");
        trackNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getTrackNumber() != null ? cellData.getValue().getTrackNumber().toString() : "N/A"));
        trackNumberColumn.setPrefWidth(isForAllTracks ? 60 : 80);
        trackNumberColumn.setResizable(true);
        trackNumberColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Track, String> titleColumn = new TableColumn<>("Titolo");
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        titleColumn.setPrefWidth(isForAllTracks ? 180 : 200);
        titleColumn.setResizable(true);
        titleColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> albumColumn = new TableColumn<>("Album");
        albumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAlbumName()));
        albumColumn.setPrefWidth(isForAllTracks ? 130 : 150);
        albumColumn.setResizable(true);
        albumColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> artistColumn = new TableColumn<>("Artista");
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtist()));
        artistColumn.setPrefWidth(isForAllTracks ? 130 : 150);
        artistColumn.setResizable(true);
        artistColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> artistAlbumColumn = new TableColumn<>("Artista Album");
        artistAlbumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtistAlbum()));
        artistAlbumColumn.setPrefWidth(isForAllTracks ? 130 : 150);
        artistAlbumColumn.setResizable(true);
        artistAlbumColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> composerColumn = new TableColumn<>("Compositore");
        composerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getComposer()));
        composerColumn.setPrefWidth(isForAllTracks ? 100 : 120);
        composerColumn.setResizable(true);
        composerColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> genreColumn = new TableColumn<>("Genere");
        genreColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGenre()));
        genreColumn.setPrefWidth(isForAllTracks ? 100 : 120);
        genreColumn.setResizable(true);
        genreColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> yearColumn = new TableColumn<>("Anno");
        yearColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getReleaseYear() != null ? cellData.getValue().getReleaseYear().toString() : "N/A"));
        yearColumn.setPrefWidth(isForAllTracks ? 70 : 80);
        yearColumn.setResizable(true);
        yearColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Track, String> durationColumn = new TableColumn<>("Durata");
        durationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                formatDurationInHMS(cellData.getValue().getDuration())));
        durationColumn.setPrefWidth(isForAllTracks ? 70 : 80);
        durationColumn.setResizable(true);
        durationColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Track, String> discNumberColumn = new TableColumn<>("Disco");
        discNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDiscNumber() != null ? cellData.getValue().getDiscNumber().toString() : "N/A"));
        discNumberColumn.setPrefWidth(isForAllTracks ? 70 : 80);
        discNumberColumn.setResizable(true);
        discNumberColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Track, String> playCountColumn = new TableColumn<>("Play Count");
        playCountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getPlayCount() != null ? cellData.getValue().getPlayCount().toString() : "0"));
        playCountColumn.setPrefWidth(isForAllTracks ? 80 : 90);
        playCountColumn.setResizable(true);
        playCountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Aggiungi tutte le colonne alla tabella
        tableViewTracks.getColumns().addAll(
                trackNumberColumn, titleColumn, albumColumn, artistColumn, artistAlbumColumn,
                composerColumn, genreColumn, yearColumn, durationColumn, discNumberColumn, playCountColumn);

        return tableViewTracks;
    }

    // Metodo per formattare la durata in formato HH:MM:SS o MM:SS
    private static String formatDurationInHMS(Integer durationInSeconds) {
        if (durationInSeconds == null) {
            return "N/A";
        }
        int hours = durationInSeconds / 3600;
        int minutes = (durationInSeconds % 3600) / 60;
        int seconds = durationInSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
