package com.example.gorunner;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, "GoRunnerPrueba3", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE recorrido (" +
                "recorridoID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "duracion BIGINT NOT NULL," +
                "distancia REAL NOT NULL," +
                "calorias REAL NOT NULL," +
                "pasos INTEGER NOT NULL DEFAULT 0,"+
                "velocidad_promedio REAL NOT NULL," +
                "fecha DATETIME NOT NULL," +
                "nombre varchar(256) NOT NULL DEFAULT 'Recorrido guardado'," +
                "calificacion INTEGER NOT NULL DEFAULT 1," +
                "comentario varchar(256) NOT NULL DEFAULT ''," +
                "imagen varchar(256) DEFAULT NULL);");

        db.execSQL("CREATE TABLE ubicacion (" +
                " ubicacionID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                " recorridoID INTEGER NOT NULL," +
                " altitud REAL NOT NULL," +
                " longitud REAL NOT NULL," +
                " latitud REAL NOT NULL," +
                " CONSTRAINT fk1 FOREIGN KEY (recorridoID) REFERENCES recorrido (recorridoID) ON DELETE CASCADE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
