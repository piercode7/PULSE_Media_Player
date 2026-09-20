#!/bin/bash
# Avvia Pulse tramite Maven: nessuno JavaFX SDK esterno necessario,
# le dipendenze (comprese quelle native per il sistema operativo corrente)
# vengono risolte automaticamente da Maven.
set -e

cd "$(dirname "$0")"
mvn javafx:run
