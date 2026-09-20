package org.mypulse.model;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.mypulse.util.TextSizeManager;
import org.mypulse.util.ThemeManager;
import org.mypulse.util.ThemeManager.Theme;

import java.util.EnumMap;
import java.util.Map;

// Finestra per scegliere il tema colori dell'applicazione. Ogni tema è una "card" con
// nome e una piccola anteprima a blocchi (barra menu, sidebar con una voce evidenziata
// nell'accento, un bottone) coi colori veri di quel tema, non un semplice elenco
// testuale - così ci si fa un'idea reale prima di scegliere, come chiesto. Cliccare una
// card applica subito quel tema a tutte le finestre aperte, inclusa questa stessa
// finestra: nessun pulsante "Applica" separato, il risultato si vede all'istante.
public class ThemePickerFrame {

    private static final double PREVIEW_WIDTH = 176;
    private static final double PREVIEW_HEIGHT = 100;
    private static final double CARD_WIDTH = 200;

    private final Stage stage;
    private final Map<Theme, VBox> cardsByTheme = new EnumMap<>(Theme.class);

    private ThemePickerFrame() {
        stage = new Stage();
        stage.setTitle("Scegli il tema");

        Label title = new Label("Scegli il tema");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        FlowPane cardsPane = new FlowPane(20, 20);
        cardsPane.setPrefWrapLength(2 * CARD_WIDTH + 40); // due card per riga
        cardsPane.setAlignment(Pos.CENTER);

        for (Theme theme : Theme.values()) {
            VBox card = buildThemeCard(theme);
            cardsByTheme.put(theme, card);
            cardsPane.getChildren().add(card);
        }

        Button closeButton = new Button("Chiudi");
        closeButton.setOnAction(event -> stage.close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, title, cardsPane, buttonBox);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        ThemeManager.applyToScene(scene);
        TextSizeManager.applyToScene(scene);

        // Stessa correzione già usata per l'editor metadati: senza forzare CSS e layout
        // prima di sizeToScene(), la finestra si aprirebbe più piccola del contenuto vero
        root.applyCss();
        root.layout();
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setResizable(false);

        updateSelectedCard();
    }

    private VBox buildThemeCard(Theme theme) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(CARD_WIDTH);
        card.setCursor(Cursor.HAND);

        Label nameLabel = new Label(theme.getDisplayName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        card.getChildren().addAll(nameLabel, buildPreview(theme));
        card.setOnMouseClicked(event -> {
            ThemeManager.applyTheme(theme);
            updateSelectedCard();
        });

        return card;
    }

    // Anteprima "a blocchi" del tema: una finestrella finta con barra del menu, sidebar
    // (con una voce evidenziata nell'accento) e un bottone. Costruita con i colori fissi
    // del tema (Theme.getPreviewXxx()) e non con il foglio di stile del tema attivo, così
    // mostra sempre l'aspetto del tema a cui appartiene, indipendentemente da quale sia
    // il tema attualmente applicato a questa stessa finestra.
    private Pane buildPreview(Theme theme) {
        Rectangle background = new Rectangle(PREVIEW_WIDTH, PREVIEW_HEIGHT, Color.web(theme.getPreviewSurface()));
        background.setArcWidth(8);
        background.setArcHeight(8);

        Rectangle menuBar = new Rectangle(PREVIEW_WIDTH, 14, Color.web(theme.getPreviewBase()));

        Rectangle sidebar = new Rectangle(50, PREVIEW_HEIGHT - 14, Color.web(theme.getPreviewBase()));
        sidebar.setLayoutY(14);

        Rectangle selectedItem = new Rectangle(38, 10, Color.web(theme.getPreviewAccent()));
        selectedItem.setArcWidth(3);
        selectedItem.setArcHeight(3);
        selectedItem.setLayoutX(6);
        selectedItem.setLayoutY(22);

        Rectangle textLine1 = new Rectangle(90, 6, Color.web(theme.getPreviewText()));
        textLine1.setLayoutX(60);
        textLine1.setLayoutY(28);

        Rectangle textLine2 = new Rectangle(60, 6, Color.web(theme.getPreviewText()));
        textLine2.setOpacity(0.6);
        textLine2.setLayoutX(60);
        textLine2.setLayoutY(42);

        Rectangle button = new Rectangle(50, 16, Color.web(theme.getPreviewAccent()));
        button.setArcWidth(4);
        button.setArcHeight(4);
        button.setLayoutX(60);
        button.setLayoutY(70);

        Pane preview = new Pane(background, menuBar, sidebar, selectedItem, textLine1, textLine2, button);
        preview.setPrefSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        preview.setMaxSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        preview.setStyle("-fx-border-color: derive(" + theme.getPreviewBase() + ", -15%); -fx-border-radius: 8;");
        return preview;
    }

    // Evidenzia con un bordo colorato la card del tema attualmente attivo, e lo aggiorna
    // subito dopo ogni click (anche su un'altra card, spostando l'evidenziazione lì)
    private void updateSelectedCard() {
        Theme active = ThemeManager.getCurrent();
        for (Map.Entry<Theme, VBox> entry : cardsByTheme.entrySet()) {
            boolean isActive = entry.getKey() == active;
            entry.getValue().setStyle(isActive
                    ? "-fx-border-color: " + active.getPreviewAccent() + "; -fx-border-width: 2; "
                        + "-fx-border-radius: 10; -fx-background-radius: 10;"
                    : "-fx-border-color: transparent; -fx-border-width: 2;");
        }
    }

    public static void show() {
        new ThemePickerFrame().stage.show();
    }
}
