# Pulse Music Player

Pulse è un'applicazione per la gestione e la riproduzione di brani musicali basata su JavaFX.

## Requisiti

- **Java**: JDK 17 o successivo
- **Maven**: per la gestione delle dipendenze (JavaFX incluso: nessun SDK da scaricare a mano)

## Avvio

```bash
./run.sh
```

oppure direttamente:

```bash
mvn javafx:run
```

Al primo avvio Maven scarica tutte le dipendenze (JavaFX, jaudiotagger, mp3agic, ecc.)
per il sistema operativo corrente: non serve estrarre alcuno SDK JavaFX nella
directory del progetto.

## Funzionalità principali

- Scansione ricorsiva di una cartella e catalogazione dei brani (`.mp3`) per artista/album
- Riproduzione con coda, avanti/indietro, replay, seek e volume
- Playlist con riordino drag & drop
- Editor metadati (titolo, artista, album, genere, testi, copertina, ...)
- Ricerca su artisti/album/brani
- Copertine album da Spotify (richiede credenziali API personali) e testi da Genius
  (script Python di supporto in `src/main/resources/get_lyrics.py`)

## Note

- La libreria viene salvata come `music_library.ser` nella directory del progetto
  (File > Salva / Carica dal menu).
- Le credenziali Spotify vengono salvate in `spotify_credentials.json` (non versionato).
