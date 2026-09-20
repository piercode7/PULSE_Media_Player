# Pulse Music Player

Pulse è un'applicazione per la gestione e la riproduzione di brani musicali basata su JavaFX.

## Requisiti

- **JDK 17 o successivo**
- **Maven 3.6 o successivo**

Nessun altro strumento è richiesto: JavaFX viene scaricato automaticamente da
Maven (vedi sotto), non c'è nessuno SDK da scaricare o estrarre a mano.

Per verificare cosa è già installato:

```bash
java -version
mvn -v
```

### Se manca il JDK

- **Linux (Debian/Ubuntu)**: `sudo apt install openjdk-21-jdk`
- **macOS (Homebrew)**: `brew install openjdk@21`
- **Windows**: installare [Eclipse Temurin](https://adoptium.net/), oppure
  `winget install EclipseAdoptium.Temurin.21.JDK`

### Se manca Maven

- **Linux (Debian/Ubuntu)**: `sudo apt install maven`
- **macOS (Homebrew)**: `brew install maven`
- **Windows**: `winget install Apache.Maven`, oppure scaricare da
  [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi),
  estrarre l'archivio e aggiungere la sua cartella `bin/` al `PATH`

## Compilazione e avvio

Dalla cartella del progetto (dopo aver clonato il repository):

```bash
# Solo compilazione (al primo comando Maven scarica tutte le dipendenze,
# JavaFX incluso, per il sistema operativo in uso: serve una connessione
# a Internet solo la prima volta, poi restano in cache in ~/.m2)
mvn compile

# Avvio dell'applicazione
mvn javafx:run
```

Su Linux e macOS è disponibile anche uno script equivalente:

```bash
./run.sh
```

Il classifier delle librerie JavaFX (linux/mac/win) viene scelto automaticamente
da Maven in base al sistema operativo che esegue la build, tramite i profili
definiti in `pom.xml`: lo stesso `pom.xml`, senza modifiche, funziona su
Linux, macOS e Windows.

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
