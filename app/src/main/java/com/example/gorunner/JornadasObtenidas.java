package com.example.gorunner;

import android.net.Uri;

public class JornadasObtenidas {
    public static final String paquete = "com.example.gorunner.Jornadas";

    public static final Uri todas = Uri.parse("content://" + paquete + "");
    public static final Uri uriJornada = Uri.parse("content://" + paquete + "/jornada");
    public static final Uri uriUbicacion = Uri.parse("content://" + paquete + "/ubicacion");

    public static final String id_jornada = "jornadaID";
    public static final String duracion_jornada = "duracion";
    public static final String distancia_jornada = "distancia";
    public static final String nombre_jornada = "nombre";
    public static final String calificacion_jornada = "calificacion";
    public static final String comentario_jornada = "comentario";
    public static final String fecha_jornada = "fecha";
    public static final String imagen_jornada = "imagen";

    public static final String ubicacionId = "ubicacionID";
    public static final String jornadaId = "jornadaID";
    public static final String altitud_jornada = "altitud";
    public static final String longitud_jornada = "longitud";
    public static final String latitud_jornada = "latitud";
}

