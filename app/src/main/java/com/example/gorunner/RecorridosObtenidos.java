package com.example.gorunner;

import android.net.Uri;

public class RecorridosObtenidos {
    public static final String paquete = "com.example.gorunner.Recorridos";

    public static final Uri todas = Uri.parse("content://" + paquete + "");
    public static final Uri uriRecorrido = Uri.parse("content://" + paquete + "/recorrido");
    public static final Uri uriUbicacion = Uri.parse("content://" + paquete + "/ubicacion");

    public static final String id_recorrido = "recorridoID";
    public static final String duracion_recorrido= "duracion";
    public static final String distancia_recorrido = "distancia";
    public static final String nombre_recorrido = "nombre";
    public static final String calificacion_recorrido = "calificacion";
    public static final String comentario_recorrido = "comentario";
    public static final String fecha_recorrido = "fecha";
    public static final String imagen_recorrido = "imagen";

    public static final String ubicacionId = "ubicacionID";
    public static final String recorridoId = "recorridoID";
    public static final String altitud_recorrido = "altitud";
    public static final String longitud_recorrido = "longitud";
    public static final String latitud_recorrido = "latitud";
}

