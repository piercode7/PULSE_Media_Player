# Pulse Music Player

Pulse è un'applicazione per la gestione e la riproduzione di brani musicali basata su JavaFX.

## Requisiti

- **Java**: versione 17 o successiva
- **Maven**: per la gestione delle dipendenze

## Configurazione di JavaFX

Questo progetto utilizza JavaFX per l'interfaccia grafica e per la riproduzione dei media. JavaFX non viene distribuito automaticamente da Maven, quindi è necessario scaricare manualmente l'SDK.

### Scaricare JavaFX SDK

1. Vai a questo [link di Google Drive](https://drive.google.com/drive/folders/1qtOxk5RiR0dMnRJ8KpFafXfWpyidyLSw) per scaricare il file `javafx-sdk-21.0.2-linux-x64.zip`.
2. Salva il file `.zip` nella directory principale del progetto.
3. Estrai il contenuto nella directory principale in modo che il percorso della libreria JavaFX sia `javafx-sdk-21.0.2/lib`.



Per avviare l'applicazione esegui il comando:


```bash
./run.sh