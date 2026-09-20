package org.mypulse.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

// Esegue src/main/resources/get_lyrics.py per recuperare il testo di un brano da Genius
// (scraping non ufficiale). Condivisa tra LyricsController (finestra dedicata ai lyrics)
// e TrackMetadataEditor (tab "Lyrics" dell'editor metadati), che prima duplicava questa
// logica in un posto e non l'aveva affatto nell'altro.
public class LyricsFetcher {

    private LyricsFetcher() {
        // Solo metodi statici
    }

    // Ritorna il testo pulito dei lyrics, oppure null se non trovato/fallito. Lo script
    // scrive SOLO i lyrics su stdout in caso di successo (niente messaggi di debug
    // mescolati) ed esce con un codice diverso da zero quando non trova nulla.
    public static String fetchLyrics(String artist, String songTitle) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "get_lyrics.py", artist, songTitle);
            processBuilder.directory(new File("src/main/resources")); // Percorso dello script

            Process process = processBuilder.start();

            // Lo stderr (usato dallo script per i messaggi di stato/debug) va letto su un
            // thread separato mentre si legge lo stdout: se lo si ignorasse del tutto, un
            // output abbastanza lungo da riempire il buffer del pipe di sistema
            // bloccherebbe lo script in scrittura, e process.waitFor() sotto resterebbe in
            // attesa per sempre di un processo a sua volta bloccato
            StringBuilder errorOutput = new StringBuilder();
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errLine;
                    while ((errLine = errReader.readLine()) != null) {
                        errorOutput.append(errLine).append("\n");
                    }
                } catch (IOException ignored) {
                    // Il processo potrebbe essere già terminato: nulla da leggere, va bene così
                }
            });
            stderrReader.setDaemon(true);
            stderrReader.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            stderrReader.join(2000); // Dà tempo al thread di finire di leggere quanto resta

            if (exitCode == 0 && !output.toString().isBlank()) {
                return output.toString().trim();
            } else {
                System.out.println("[Lyrics] Script terminato senza trovare il testo (codice " + exitCode + "): "
                        + errorOutput.toString().trim());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
