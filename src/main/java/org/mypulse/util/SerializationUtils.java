package org.mypulse.util;

import java.io.*;

public class SerializationUtils {

    // Metodo per serializzare un oggetto e salvarlo su file
    public static <T extends Serializable> void serialize(T object, String filePath) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(filePath);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(object);  // Scrive l'oggetto nel file
            System.out.println("Oggetto serializzato e salvato su: " + filePath);
        }
    }

    // Metodo per deserializzare un oggetto da un file
    public static <T extends Serializable> T deserialize(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fileIn = new FileInputStream(filePath);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            @SuppressWarnings("unchecked")
            T object = (T) in.readObject();  // Legge l'oggetto dal file
            System.out.println("Oggetto deserializzato da: " + filePath);
            return object;
        }
    }
}
