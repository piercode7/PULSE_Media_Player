#!/bin/bash

# Verifica che javafx-sdk-21.0.2 sia presente
if [ ! -d "javafx-sdk-21.0.2/lib" ]; then
  echo "Errore: La directory javafx-sdk-21.0.2/lib non esiste. Assicurati di aver estratto JavaFX SDK 21.0.2 nella directory principale del progetto."
  exit 1
fi

# Esegui il comando per avviare l'applicazione
java --module-path "${PWD}/javafx-sdk-21.0.2/lib" \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     --add-exports=javafx.graphics/com.sun.glass.utils=ALL-UNNAMED \
     --add-exports=javafx.media/com.sun.media.jfxmediaimpl=ALL-UNNAMED \
     --add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
     -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt && cat classpath.txt)" \
     org.mypulse.view.MainView
