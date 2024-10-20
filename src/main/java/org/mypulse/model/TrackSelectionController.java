package org.mypulse.model;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import org.mypulse.model.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackSelectionController {

    private TableView<Track> tableView;
    private List<Track> orderedSelectedTracks; // Lista per memorizzare l'ordine di selezione

    public TrackSelectionController(TableView<Track> tableView) {
        this.tableView = tableView;
        this.orderedSelectedTracks = new ArrayList<>();

        // Abilita la selezione multipla nella tabella
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Aggiungi un listener per monitorare la selezione
        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Track>) change -> {
            while (change.next()) {
                // Quando vengono aggiunti nuovi elementi
                if (change.wasAdded()) {
                    for (Track track : change.getAddedSubList()) {
                        // Aggiungi il brano selezionato alla lista mantenendo l'ordine
                        orderedSelectedTracks.add(track);
                    }
                }

                // Quando vengono rimossi elementi
                if (change.wasRemoved()) {
                    for (Track track : change.getRemoved()) {
                        // Rimuovi il brano non più selezionato dalla lista
                        orderedSelectedTracks.remove(track);
                    }
                }
            }

            // Stampa l'ordine attuale delle selezioni
            System.out.println("Ordine di selezione: ");
            for (Track track : orderedSelectedTracks) {
                System.out.println(track.getTitle());
            }
        });
    }

    public List<Track> getOrderedSelectedTracks() {
        return orderedSelectedTracks;
    }
}
