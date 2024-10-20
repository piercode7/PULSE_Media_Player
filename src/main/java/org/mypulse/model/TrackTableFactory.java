package org.mypulse.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class TrackTableFactory {

    // Metodo per creare la prima tabella (standard)
    public static TableView<Track> createTrackTable() {
        return createCustomTrackTable(false);
    }

    // Metodo per creare la seconda tabella (per tutti i brani)
    public static TableView<Track> createTrackTableAll() {
        return createCustomTrackTable(true);
    }

    // Metodo personalizzato per creare una tabella in base a un flag
    private static TableView<Track> createCustomTrackTable(boolean isForAllTracks) {
        TableView<Track> tableViewTracks = new TableView<>();
        tableViewTracks.setPlaceholder(new javafx.scene.control.Label("Nessun brano disponibile"));
        tableViewTracks.setVisible(false); // Nascondi la tabella finché non viene selezionato un album o brani
        tableViewTracks.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableViewTracks.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Definizione delle colonne
        TableColumn<Track, String> trackNumberColumn = new TableColumn<>("Indice");
        trackNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getTrackNumber() != null ? cellData.getValue().getTrackNumber().toString() : "N/A"));
        trackNumberColumn.setPrefWidth(isForAllTracks ? 60 : 80);  // Personalizzazione della larghezza
        trackNumberColumn.setResizable(true);
        trackNumberColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Track, String> titleColumn = new TableColumn<>("Titolo");
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        titleColumn.setPrefWidth(isForAllTracks ? 180 : 200);  // Personalizzazione della larghezza
        titleColumn.setResizable(true);
        titleColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> albumColumn = new TableColumn<>("Album");
        albumColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAlbumName()));
        albumColumn.setPrefWidth(isForAllTracks ? 130 : 150);  // Personalizzazione della larghezza
        albumColumn.setResizable(true);
        albumColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> artistColumn = new TableColumn<>("Artista");
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArtist()));
        artistColumn.setPrefWidth(isForAllTracks ? 130 : 150);  // Personalizzazione della larghezza
        artistColumn.setResizable(true);
        artistColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Track, String> durationColumn = new TableColumn<>("Durata");
        durationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                formatDurationInHMS(cellData.getValue().getDuration())));
        durationColumn.setPrefWidth(isForAllTracks ? 70 : 80);  // Personalizzazione della larghezza
        durationColumn.setResizable(true);
        durationColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Track, String> discNumberColumn = new TableColumn<>("Disco");
        discNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDiscNumber() != null ? cellData.getValue().getDiscNumber().toString() : "N/A"));
        discNumberColumn.setPrefWidth(isForAllTracks ? 70 : 80);  // Personalizzazione della larghezza
        discNumberColumn.setResizable(true);
        discNumberColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Aggiungi tutte le colonne alla tabella
        tableViewTracks.getColumns().addAll(trackNumberColumn, titleColumn, albumColumn, artistColumn, durationColumn, discNumberColumn);

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
