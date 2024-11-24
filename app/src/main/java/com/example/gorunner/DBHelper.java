package com.example.gorunner;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// La clase DBHelper extiende SQLiteOpenHelper y se utiliza para gestionar la base de datos SQLite.
// Permite crear y actualizar la base de datos, en este caso, para almacenar información de recorridos y ubicaciones.
public class DBHelper extends SQLiteOpenHelper {

    // Constructor que llama al constructor de SQLiteOpenHelper con el nombre de la base de datos y la versión.
    public DBHelper(Context context) {
        super(context, "GoRunnerBase", null, 1); // Nombre de la base de datos: "GoRunner", versión: 1.
    }

    // Método que se ejecuta cuando se crea la base de datos.
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crea la tabla 'recorrido' que almacena información sobre los recorridos.
        db.execSQL("CREATE TABLE recorrido (" +
                "recorridoID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," + // ID del recorrido (clave primaria autoincremental).
                "duracion BIGINT NOT NULL," + // Duración del recorrido en milisegundos.
                "distancia REAL NOT NULL," + // Distancia del recorrido en kilómetros o metros.
                "calorias REAL NOT NULL," + // Calorías quemadas durante el recorrido.
                "pasos INTEGER NOT NULL DEFAULT 0," + // Número de pasos (valor por defecto 0).
                "velocidad_promedio REAL NOT NULL," + // Velocidad promedio durante el recorrido.
                "fecha DATETIME NOT NULL," + // Fecha y hora del recorrido.
                "nombre varchar(256) NOT NULL DEFAULT 'Recorrido guardado'," + // Nombre del recorrido (por defecto 'Recorrido guardado').
                "calificacion INTEGER NOT NULL DEFAULT 1," + // Calificación del recorrido (por defecto 1).
                "comentario varchar(256) NOT NULL DEFAULT 'No hay Comentarios para este recorrido'," + // Comentarios sobre el recorrido.
                "imagen varchar(256) DEFAULT NULL);"); // Ruta de la imagen asociada al recorrido (puede ser nula).

        // Crea la tabla 'ubicacion' que almacena información sobre las ubicaciones de cada recorrido.
        db.execSQL("CREATE TABLE ubicacion (" +
                " ubicacionID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," + // ID de la ubicación (clave primaria autoincremental).
                " recorridoID INTEGER NOT NULL," + // ID del recorrido al que pertenece la ubicación.
                " altitud REAL NOT NULL," + // Altitud de la ubicación.
                " longitud REAL NOT NULL," + // Longitud de la ubicación.
                " latitud REAL NOT NULL," + // Latitud de la ubicación.
                " CONSTRAINT fk1 FOREIGN KEY (recorridoID) REFERENCES recorrido (recorridoID) ON DELETE CASCADE);"); // Clave foránea a la tabla 'recorrido'. Al eliminar un recorrido, se eliminan sus ubicaciones asociadas.
    }

    // Método que se ejecuta cuando se realiza una actualización de la base de datos (no implementado en este caso).
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // En este caso no se ha implementado la lógica para manejar actualizaciones de la base de datos.
    }
}

