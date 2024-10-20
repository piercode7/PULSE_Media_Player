package org.mypulse.model;

import javafx.scene.control.TextInputDialog;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

public class SpotifyAPIDialog {

    public static void requestAndSaveAPICredentials() {
        // Creare il dialogo per richiedere il Client ID
        TextInputDialog clientIdDialog = new TextInputDialog();
        clientIdDialog.setTitle("Inserisci Client ID di Spotify");
        clientIdDialog.setHeaderText("API di Spotify");
        clientIdDialog.setContentText("Inserisci il tuo Client ID:");

        Optional<String> clientIdResult = clientIdDialog.showAndWait();
        if (!clientIdResult.isPresent()) {
            System.out.println("Inserimento Client ID annullato.");
            return;
        }
        String clientId = clientIdResult.get();

        // Creare il dialogo per richiedere il Client Secret
        TextInputDialog clientSecretDialog = new TextInputDialog();
        clientSecretDialog.setTitle("Inserisci Client Secret di Spotify");
        clientSecretDialog.setHeaderText("API di Spotify");
        clientSecretDialog.setContentText("Inserisci il tuo Client Secret:");

        Optional<String> clientSecretResult = clientSecretDialog.showAndWait();
        if (!clientSecretResult.isPresent()) {
            System.out.println("Inserimento Client Secret annullato.");
            return;
        }
        String clientSecret = clientSecretResult.get();

        // Salva le credenziali API nel file locale
        saveAPICredentials(clientId, clientSecret);
    }

    private static void saveAPICredentials(String clientId, String clientSecret) {
        try (FileWriter writer = new FileWriter("spotify_credentials.json")) {
            writer.write("{\n");
            writer.write("\"SPOTIFY_CLIENT_ID\": \"" + clientId + "\",\n");
            writer.write("\"SPOTIFY_CLIENT_SECRET\": \"" + clientSecret + "\"\n");
            writer.write("}");
            System.out.println("Credenziali salvate con successo.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
