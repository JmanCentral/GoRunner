package com.example.gorunner;

import android.net.Uri;

public class RecorridosObtenidos {

    // Constante que define el paquete base del proveedor de contenido.
    // Este paquete identifica el origen de los datos dentro de la aplicación.
    public static final String paquete = "com.example.gorunner.Recorridos";

    // URI para acceder a todos los elementos del proveedor de contenido.
    public static final Uri todas = Uri.parse("content://" + paquete + "");

    // URI específica para acceder a los datos relacionados con los recorridos.
    public static final Uri uriRecorrido = Uri.parse("content://" + paquete + "/recorrido");

    // URI específica para acceder a los datos relacionados con las ubicaciones.
    public static final Uri uriUbicacion = Uri.parse("content://" + paquete + "/ubicacion");

    // --- Constantes que definen las claves de los campos de los recorridos ---

    // Identificador único de un recorrido.
    public static final String id_recorrido = "recorridoID";

    // Duración del recorrido (puede ser en minutos, segundos, etc.).
    public static final String duracion_recorrido = "duracion";

    // Distancia recorrida (puede estar en metros o kilómetros).
    public static final String distancia_recorrido = "distancia";

    // Calorías quemadas durante el recorrido.
    public static final String calorias_recorrido = "calorias";

    // Cantidad de pasos dados en el recorrido.
    public static final String pasos_recorrido = "pasos";

    // Velocidad promedio alcanzada durante el recorrido.
    public static final String velocidad_recorrido = "velocidad_promedio";

    // Nombre asignado al recorrido por el usuario o el sistema.
    public static final String nombre_recorrido = "nombre";

    // Calificación asignada al recorrido (por ejemplo, en una escala del 1 al 5).
    public static final String calificacion_recorrido = "calificacion";

    // Comentarios o notas adicionales sobre el recorrido.
    public static final String comentario_recorrido = "comentario";

    // Fecha en la que se realizó el recorrido.
    public static final String fecha_recorrido = "fecha";

    // Imagen asociada al recorrido (puede ser un mapa, foto, etc.).
    public static final String imagen_recorrido = "imagen";

    // --- Constantes que definen las claves de los campos de las ubicaciones ---

    // Identificador único de una ubicación.
    public static final String ubicacionId = "ubicacionID";

    // Identificador del recorrido al que pertenece esta ubicación.
    public static final String recorridoId = "recorridoID";

    // Altitud registrada en una ubicación.
    public static final String altitud_recorrido = "altitud";

    // Longitud geográfica de la ubicación.
    public static final String longitud_recorrido = "longitud";

    // Latitud geográfica de la ubicación.
    public static final String latitud_recorrido = "latitud";
}

