package com.example.gorunner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Localizacion extends Service {
    private LocationManager gestorLocalizacion;
    private MiLocalizacionListener oyenteLocalizacion;
    private final IBinder enlace = new EnlaceServicioLocalizacion();

    private final String ID_CANAL = "100";
    private final int ID_NOTIFICACION = 001;
    private long tiempoInicio = 0;
    private long tiempoFin = 0;

    final int INTERVALO_TIEMPO = 3;
    final int INTERVALO_DISTANCIA = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("mdp", "Servicio de Localización creado");

        gestorLocalizacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        oyenteLocalizacion = new MiLocalizacionListener();
        oyenteLocalizacion.grabarUbicaciones = false;


        try {
            gestorLocalizacion.requestLocationUpdates(gestorLocalizacion.GPS_PROVIDER, INTERVALO_TIEMPO, INTERVALO_DISTANCIA, oyenteLocalizacion);
        } catch (SecurityException e) {
            // no se tiene permiso para acceder al GPS
            Log.d("mdp", "No se tienen permisos para el GPS");
        }
    }


    private void agregarNotificacion() {
        NotificationManager gestorNotificaciones = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombre = "Rastreo de Jornada";
            String descripcion = "¡Sigue corriendo!";
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel(ID_CANAL, nombre, importancia);
            canal.setDescription(descripcion);
            gestorNotificaciones.createNotificationChannel(canal);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent intentoPendiente = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder constructorNotificacion = new NotificationCompat.Builder(this, ID_CANAL)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Rastreo de Jornada")
                .setContentText("¡Sigue corriendo!")
                .setContentIntent(intentoPendiente)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        gestorNotificaciones.notify(ID_NOTIFICACION, constructorNotificacion.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // el usuario ha cerrado la aplicación, cancelar la jornada actual y detener el rastreo del GPS
        gestorLocalizacion.removeUpdates(oyenteLocalizacion);
        oyenteLocalizacion = null;
        gestorLocalizacion = null;

        NotificationManager gestorNotificaciones = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        gestorNotificaciones.cancel(ID_NOTIFICACION);

        Log.d("mdp", "Servicio de Localización destruido");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return enlace;
    }

    protected float obtenerDistancia() {
        return oyenteLocalizacion.obtenerDistanciaDeJornada();
    }

    /* Mostrar notificación e iniciar grabación de ubicaciones GPS para una nueva jornada, también iniciar el temporizador */
    protected void iniciarRecorrido() {
        agregarNotificacion();
        oyenteLocalizacion.nuevaJornada();
        oyenteLocalizacion.grabarUbicaciones = true;
        tiempoInicio = SystemClock.elapsedRealtime();
        tiempoFin = 0;
    }

    protected float obtenerCalorias(float pesoUsuario) {
        float distancia = obtenerDistancia(); // Obtener distancia
        double duracionHoras = obtenerDuracion() / 3600.0; // Convertir duración a horas

        if (duracionHoras == 0) {
            return 0.0f; // Evitar división por cero
        }

        // Calcular la velocidad (km/h)
        double velocidad = distancia / duracionHoras;

        // Determinar MET basado en la velocidad
        float MET;
        if (velocidad < 5) {
            MET = 3.5f; // Caminar
        } else if (velocidad < 8) {
            MET = 6.0f; // Trotar
        } else {
            MET = 9.0f; // Correr
        }

        // Calcular calorías quemadas (en minutos)
        double duracionMinutos = obtenerDuracion() / 60.0; // Convertir duración a minutos
        return (float) (MET * pesoUsuario * 0.0175 * duracionMinutos);
    }


    protected double obtenerDuracion() {
        if (tiempoInicio == 0) {
            return 0.0;
        }

        long tiempoFinal = SystemClock.elapsedRealtime();

        if (tiempoFin != 0) {
            tiempoFinal = tiempoFin;
        }

        long milisegundosTranscurridos = tiempoFinal - tiempoInicio;
        return milisegundosTranscurridos / 1000.0;
    }

    protected boolean rastreoActivo() {
        return tiempoInicio != 0;
    }

    /* Guardar la jornada en la base de datos y detener el guardado de ubicaciones GPS, también elimina la notificación */
    protected void guardarRecorrido() {

        SharedPreferences sharedPreferences = getSharedPreferences("PreferenciasUsuario", MODE_PRIVATE);
        float pesoRecuperado = sharedPreferences.getFloat("peso", 0.0f);

        ContentValues datosJornada = new ContentValues();
        datosJornada.put(RecorridosObtenidos.distancia_recorrido, obtenerDistancia());
        datosJornada.put(RecorridosObtenidos.duracion_recorrido, (long) obtenerDuracion());
        datosJornada.put(RecorridosObtenidos.fecha_recorrido, obtenerFechaHora());
        datosJornada.put(RecorridosObtenidos.calorias_recorrido, obtenerCalorias(pesoRecuperado));

        long idRecorrido = Long.parseLong(getContentResolver().insert(RecorridosObtenidos.uriRecorrido, datosJornada).getLastPathSegment());

        // para cada ubicación perteneciente a esta jornada, guardarla en la tabla de ubicaciones vinculada a esta jornada
        for (Location ubicacion : oyenteLocalizacion.obtenerUbicaciones()) {
            ContentValues datosUbicacion = new ContentValues();
            datosUbicacion.put(RecorridosObtenidos.recorridoId, idRecorrido);
            datosUbicacion.put(RecorridosObtenidos.altitud_recorrido, ubicacion.getAltitude());
            datosUbicacion.put(RecorridosObtenidos.latitud_recorrido, ubicacion.getLatitude());
            datosUbicacion.put(RecorridosObtenidos.longitud_recorrido, ubicacion.getLongitude());

            getContentResolver().insert(RecorridosObtenidos.uriUbicacion, datosUbicacion);
        }

        NotificationManager gestorNotificaciones = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        gestorNotificaciones.cancel(ID_NOTIFICACION);

        // reiniciar el estado limpiando ubicaciones, detener la grabación, reiniciar tiempoInicio
        oyenteLocalizacion.grabarUbicaciones = false;
        tiempoFin = SystemClock.elapsedRealtime();
        tiempoInicio = 0;
        oyenteLocalizacion.nuevaJornada();

        Log.d("mdp", "Jornada guardada con id = " + idRecorrido);
    }

    protected void cambiarFrecuenciaSolicitudGPS(int tiempo, int distancia) {
        // se puede usar para cambiar la frecuencia de solicitud de GPS para conservación de batería
        try {
            gestorLocalizacion.removeUpdates(oyenteLocalizacion);
            gestorLocalizacion.requestLocationUpdates(gestorLocalizacion.GPS_PROVIDER, tiempo, distancia, oyenteLocalizacion);
            Log.d("mdp", "Nuevo tiempo mínimo = " + tiempo + ", distancia mínima = " + distancia);
        } catch (SecurityException e) {
            // no se tiene permiso para acceder al GPS
            Log.d("mdp", "No se tienen permisos para el GPS");
        }
    }

    protected void notificarGPSHabilitado() {
        try {
            gestorLocalizacion.requestLocationUpdates(gestorLocalizacion.GPS_PROVIDER, 3, 3, oyenteLocalizacion);
        } catch (SecurityException e) {
            // no se tiene permiso para acceder al GPS
            Log.d("mdp", "No se tienen permisos para el GPS");
        }
    }

    private String obtenerFechaHora() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
        Date fecha = new Date();
        return formato.format(fecha);
    }

    public class EnlaceServicioLocalizacion extends Binder {

        public float obtenerDistancia() {
            return Localizacion.this.obtenerDistancia();
        }

        public double obtenerDuracion() {
            return Localizacion.this.obtenerDuracion();
        }

        public float obtenerCalorias(float pesoUsuario) {
            return Localizacion.this.obtenerCalorias(pesoUsuario);
        }

        public boolean rastreoActivo() {
            return Localizacion.this.rastreoActivo();
        }

        public void iniciarRecorrido() {
            Localizacion.this.iniciarRecorrido();
        }

        public void guardarRecorrido() {
            Localizacion.this.guardarRecorrido();
        }

        public void notificarGPS() {
            Localizacion.this.notificarGPSHabilitado();
        }

        public void cambiarSolicitudGPS(int tiempo, int distancia) {
            Localizacion.this.cambiarFrecuenciaSolicitudGPS(tiempo, distancia);
        }
    }
}

