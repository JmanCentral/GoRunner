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

    // Se escriben las rutas donde están los atributos que se almacenan en la base de datos.
    static {
        emparejador = new UriMatcher(UriMatcher.NO_MATCH);
        emparejador.addURI(RecorridosObtenidos.paquete, "recorrido", 1); // Ruta para los recorridos
        emparejador.addURI(RecorridosObtenidos.paquete, "recorrido/#", 2); // Ruta para un recorrido específico
        emparejador.addURI(RecorridosObtenidos.paquete, "ubicacion", 3); // Ruta para las ubicaciones
        emparejador.addURI(RecorridosObtenidos.paquete, "ubicacion/#", 4); // Ruta para una ubicación específica
    }

    /**
     * Inicializa el proveedor de contenido.
     * Se crea la instancia de `DBHelper` y se abre la base de datos en modo escritura.
     *
     * @return true si la base de datos se abrió correctamente, false si no.
     */

    @Override
    public boolean onCreate() {
        Log.d("mdp", "Proveedor de Contenido de recorrido creado");
        dbh = new DBHelper(this.getContext());
        db = dbh.getWritableDatabase();
        return (db != null);
    }

    /**
     * Devuelve el tipo MIME de los datos que gestiona este proveedor de contenido.
     *
     * @param uri La URI que está siendo consultada.
     * @return El tipo MIME correspondiente a la URI solicitada.
     */

    @Override
    public String getType(Uri uri) {
        if (uri.getLastPathSegment() == null) {
            return "vnd.android.cursor.dir/JourneyProvider.data.text";
        } else {
            return "vnd.android.cursor.item/JourneyProvider.data.text";
        }
    }

    // implementar operaciones CRUD

    /**
     * Inserta un nuevo recorrido o ubicación en la base de datos.
     *
     * @param uri La URI que define el tipo de datos que se va a insertar.
     * @param valores Los valores a insertar, proporcionados como un objeto `ContentValues`.
     * @return La URI que representa la fila insertada, con el ID de la fila agregado.
     */

    @Override
    public Uri insert(Uri uri, ContentValues valores) {
        String nombreTabla;

        // URI dada -> nombre de la tabla
        switch(emparejador.match(uri)) {
            case 1:
                // Recorrido
                nombreTabla = "recorrido";
                break;
            case 3:
                // Ubicación
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
    /**
     * Consulta los datos de los recorridos o ubicaciones de la base de datos.
     *
     * @param uri La URI que especifica los datos que se van a consultar.
     * @param proyeccion Las columnas que se deben devolver.
     * @param seleccion La cláusula WHERE para filtrar los resultados.
     * @param argumentosSeleccion Los valores que corresponden a los parámetros de selección.
     * @param orden El orden en el que deben ser devueltos los resultados.
     * @return Un `Cursor` que contiene los resultados de la consulta.
     */

    @Override
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
    /**
     * Actualiza un recorrido o una ubicación en la base de datos.
     *
     * @param uri La URI que especifica qué datos se van a actualizar.
     * @param valores Los valores nuevos que deben actualizarse.
     * @param seleccion La cláusula WHERE para identificar los registros a actualizar.
     * @param argumentosSeleccion Los valores para la cláusula WHERE.
     * @return El número de filas que fueron actualizadas.
     */

    @Override
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

        // Notificar a los observadores de contenido que la base de datos ha cambiado
        getContext().getContentResolver().notifyChange(uri, null);
        return cuenta;
    }

    /**
     * Elimina un recorrido o una ubicación de la base de datos.
     *
     * @param uri La URI que especifica qué datos se van a eliminar.
     * @param seleccion La cláusula WHERE para identificar los registros a eliminar.
     * @param argumentosSeleccion Los valores para la cláusula WHERE.
     * @return El número de filas eliminadas.
     */

    @Override
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

        // Notificar a los observadores de contenido que la base de datos ha cambiado
        getContext().getContentResolver().notifyChange(uri, null);
        return cuenta;
    }
}
