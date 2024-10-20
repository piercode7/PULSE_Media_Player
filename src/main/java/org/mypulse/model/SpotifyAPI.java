package org.mypulse.model;

import javafx.scene.control.Alert;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SpotifyAPI {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String CREDENTIALS_FILE = "spotify_credentials.json";

    // Metodo per ottenere le credenziali dal file
    private Map<String, String> getCredentials() throws IOException {
        File credentialsFile = new File(CREDENTIALS_FILE);
        if (!credentialsFile.exists()) {
            throw new IOException("File credenziali non trovato. Inserisci le credenziali nel menu API.");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(credentialsFile))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
            Map<String, String> credentials = new HashMap<>();
            credentials.put("CLIENT_ID", jsonObject.getString("SPOTIFY_CLIENT_ID"));
            credentials.put("CLIENT_SECRET", jsonObject.getString("SPOTIFY_CLIENT_SECRET"));
            return credentials;
        }
    }

    public String getAccessToken() {
        Map<String, String> credentials;
        try {
            credentials = getCredentials();
        } catch (IOException e) {
            // Mostra un messaggio di errore per credenziali mancanti o non valide
            showErrorAlert("Errore Credenziali", "File credenziali non trovato.", e.getMessage());
            return null;
        }

        String clientId = credentials.get("CLIENT_ID");
        String clientSecret = credentials.get("CLIENT_SECRET");

        String encodedCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .header("Authorization", "Basic " + encodedCredentials)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Mostra un messaggio di errore per credenziali non valide
                showErrorAlert("Errore Credenziali", "Le credenziali fornite non sono valide.", "Verifica e aggiorna le credenziali nel menu API.");
                return null;
            }
            String responseBody = response.body().string();
            return parseToken(responseBody); // Estrai il token JSON
        } catch (IOException e) {
            showErrorAlert("Errore di rete", "Errore di rete durante la richiesta del token.", e.getMessage());
            return null;
        }
    }

    public String getAlbumCoverUrl(String accessToken, String albumName, String artistName) {
        if (accessToken == null) {
            // Messaggio già mostrato in getAccessToken(), esci
            return null;
        }

        OkHttpClient client = new OkHttpClient();
        String url = "https://api.spotify.com/v1/search?q=album:" + albumName + "%20artist:" + artistName + "&type=album";
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                showErrorAlert("Errore Ricerca Album", "Non è stato possibile trovare la copertina dell'album.", "Verifica l'album e l'artista.");
                return null;
            }
            String responseBody = response.body().string();
            String coverUrl = parseAlbumCoverUrl(responseBody);
            if (coverUrl == null) {
                showErrorAlert("Copertina non trovata", "Non è stata trovata alcuna immagine per l'album specificato.", "Controlla i dettagli dell'album o riprova con un altro.");
            }
            return coverUrl;
        } catch (IOException e) {
            showErrorAlert("Errore di rete", "Errore di rete durante la ricerca dell'album.", e.getMessage());
            return null;
        }
    }

    private String parseAlbumCoverUrl(String json) {
        JSONObject jsonObject = new JSONObject(json);
        JSONArray items = jsonObject.getJSONObject("albums").getJSONArray("items");

        if (items.length() > 0) {
            JSONObject album = items.getJSONObject(0);
            JSONArray images = album.getJSONArray("images");
            if (images.length() > 0) {
                return images.getJSONObject(0).getString("url");
            }
        }
        return null;
    }

    private String parseToken(String json) {
        JSONObject jsonObject = new JSONObject(json);
        return jsonObject.getString("access_token");
    }

    // Metodo per mostrare un Alert di errore
    private void showErrorAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }
}
