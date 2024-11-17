package com.example.gorunner;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // el receptor de difusión puede enviar mensajes sobre batería baja en los que el paquete contiene información de la batería
        if (intent != null) {
            Bundle paquete = intent.getExtras();
            if (paquete != null && paquete.getBoolean("bateria")) {
                // reducir la frecuencia de solicitud del GPS
                cambiarFrecuenciaSolicitudGPS(INTERVALO_TIEMPO * 3, INTERVALO_DISTANCIA * 3);
            }
        }

        return START_NOT_STICKY;
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


    /* Obtener la duración de la jornada actual */
    protected double obtenerDuracion() {
        if (tiempoInicio == 0) {
            return 0.0;
        }

        long tiempoFinal = SystemClock.elapsedRealtime();

        if (tiempoFin != 0) {
            // se ha llamado a guardarJornada, hasta que se llame a iniciarJornada se muestra un tiempo constante
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
        // guardar jornada en la base de datos usando el proveedor de contenido
        ContentValues datosJornada = new ContentValues();
        datosJornada.put(RecorridosObtenidos.distancia_recorrido, obtenerDistancia());
        datosJornada.put(RecorridosObtenidos.duracion_recorrido, (long) obtenerDuracion());
        datosJornada.put(RecorridosObtenidos.fecha_recorrido, obtenerFechaHora());

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

