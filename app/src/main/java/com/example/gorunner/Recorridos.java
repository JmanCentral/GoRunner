package com.example.gorunner;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class Recorridos extends ContentProvider {
    DBHelper dbh;
    SQLiteDatabase db;

    private static final UriMatcher emparejador;

    //se escriben las rutas donde esta los atributos que se almacenan en la base de datos
    static {
        emparejador = new UriMatcher(UriMatcher.NO_MATCH);
        emparejador.addURI(RecorridosObtenidos.paquete, "recorrido", 1);
        emparejador.addURI(RecorridosObtenidos.paquete, "recorrido/#", 2);
        emparejador.addURI(RecorridosObtenidos.paquete, "ubicacion", 3);
        emparejador.addURI(RecorridosObtenidos.paquete, "ubicacion/#", 4);
    }

    @Override
    public boolean onCreate() {
        Log.d("mdp", "Proveedor de Contenido de recorrido creado");
        dbh = new DBHelper(this.getContext());
        db = dbh.getWritableDatabase();
        return (db != null);
    }

    @Override
    public String getType(Uri uri) {
        if (uri.getLastPathSegment() == null) {
            return "vnd.android.cursor.dir/JourneyProvider.data.text";
        } else {
            return "vnd.android.cursor.item/JourneyProvider.data.text";
        }
    }

    // implementación de las operaciones CRUD del DB helper

    @Override

    //Insertar recorrido y ubicación
    public Uri insert(Uri uri, ContentValues valores) {
        String nombreTabla;

        // URI dada -> nombre de la tabla
        switch(emparejador.match(uri)) {
            case 1:
                nombreTabla = "recorrido";
                break;
            case 3:
                nombreTabla = "ubicacion";
                break;
            default:
                nombreTabla = "";
        }

        // Inserta los valores en la tabla y devuelve la misma URI pero con el id adjunto
        long _id = db.insert(nombreTabla, null, valores);
        Uri nuevaFilaUri = ContentUris.withAppendedId(uri, _id);

        // Notificar a los observadores de contenido registrados que se ha hecho un cambio en esta tabla
        getContext().getContentResolver().notifyChange(nuevaFilaUri, null);
        return nuevaFilaUri;
    }

    @Override
    //Listar recorridos con ubicación 
    public Cursor query(Uri uri, String[] proyeccion, String seleccion, String[]
            argumentosSeleccion, String orden) {

        switch(emparejador.match(uri)) {
            case 2:
                // URI con /# para solicitar una fila específica
                seleccion = "recorridoID = " + uri.getLastPathSegment();
            case 1:
                return db.query("recorrido", proyeccion, seleccion, argumentosSeleccion, null, null, orden);
            case 4:
                seleccion = "ubicacionID = " + uri.getLastPathSegment();
            case 3:
                return db.query("ubicacion", proyeccion, seleccion, argumentosSeleccion, null, null, orden);
            default:
                return null;
        }
    }

    @Override
    //Actualizar recorridos y ubicación
    public int update(Uri uri, ContentValues valores, String seleccion, String[]
            argumentosSeleccion) {
        String nombreTabla;
        int cuenta;

        // URI dada -> nombre de la tabla
        switch(emparejador.match(uri)) {
            case 2:
                // URI con /# para solicitar una fila específica
                seleccion = "recorridoID = " + uri.getLastPathSegment();
            case 1:
                nombreTabla = "recorrido";
                cuenta = db.update(nombreTabla, valores, seleccion, argumentosSeleccion);
                break;
            case 4:
                seleccion = "ubicacionID = " + uri.getLastPathSegment();
            case 3:
                nombreTabla = "ubicacion";
                cuenta = db.update(nombreTabla, valores, seleccion, argumentosSeleccion);
                break;
            default:
                return 0;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return cuenta;
    }

    @Override
    //Eliminar recorrido e ubicación 
    public int delete(Uri uri, String seleccion, String[] argumentosSeleccion) {
        String nombreTabla;
        int cuenta;

        // URI dada -> nombre de la tabla
        switch(emparejador.match(uri)) {
            case 2:
                // URI con /# para solicitar una fila específica
                seleccion = "recorridoID = " + uri.getLastPathSegment();
            case 1:
                nombreTabla = "recorrido";
                cuenta = db.delete(nombreTabla, seleccion, argumentosSeleccion);
                break;
            case 4:
                seleccion = "ubicacionID = " + uri.getLastPathSegment();
            case 3:
                nombreTabla = "ubicacion";
                cuenta = db.delete(nombreTabla, seleccion, argumentosSeleccion);
                break;
            default:
                return 0;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return cuenta;
    }
}
