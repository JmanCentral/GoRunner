package com.example.gorunner;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, "localDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE jornada (" +
                "jornadaID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "duracion BIGINT NOT NULL," +
                "distancia REAL NOT NULL," +
                "fecha DATETIME NOT NULL," +
                "nombre varchar(256) NOT NULL DEFAULT 'Jornada guardada'," +
                "calificacion INTEGER NOT NULL DEFAULT 1," +
                "comentario varchar(256) NOT NULL DEFAULT ''," +
                "imagen varchar(256) DEFAULT NULL);");

        db.execSQL("CREATE TABLE ubicacion (" +
                " ubicacionID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                " jornadaID INTEGER NOT NULL," +
                " altitud REAL NOT NULL," +
                " longitud REAL NOT NULL," +
                " latitud REAL NOT NULL," +
                " CONSTRAINT fk1 FOREIGN KEY (jornadaID) REFERENCES journey (jornadaID) ON DELETE CASCADE);");
    }

    // called when the database file exists but the version number stored in the db
    // is lower than that passed in the constructor
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
