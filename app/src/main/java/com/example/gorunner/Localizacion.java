package com.example.gorunner;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Localizacion extends Service implements SensorEventListener {
    private LocationManager gestorLocalizacion;
    private MiLocalizacionListener oyenteLocalizacion;
    private final IBinder enlace = new EnlaceServicioLocalizacion();

    private final String ID_CANAL = "100";
    private final int ID_NOTIFICACION = 001;
    private long tiempoInicio = 0;
    private long tiempoFin = 0;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private int pasosInicio = 0;
    private int pasosActuales = 0;
    private boolean podometroActivo = false;

    final int INTERVALO_TIEMPO = 3;
    final int INTERVALO_DISTANCIA = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("mdp", "Servicio de Localización creado");

        gestorLocalizacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        oyenteLocalizacion = new MiLocalizacionListener();
        oyenteLocalizacion.grabarUbicaciones = false;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounterSensor == null) {
                Log.d("mdp", "Sensor de pasos no disponible");
            }
        }

        try {
            gestorLocalizacion.requestLocationUpdates(gestorLocalizacion.GPS_PROVIDER, INTERVALO_TIEMPO, INTERVALO_DISTANCIA, oyenteLocalizacion);
        } catch (SecurityException e) {
            Log.d("mdp", "No se tienen permisos para el GPS");
        }
    }

    private void agregarNotificacion() {
        NotificationManager gestorNotificaciones = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombre = "Rastreo de Recorrido";
            String descripcion = "¡Sigue corriendo!";
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel(ID_CANAL, nombre, importancia);
            canal.setDescription(descripcion);
            gestorNotificaciones.createNotificationChannel(canal);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent intentoPendiente = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder constructorNotificacion = new NotificationCompat.Builder(this, ID_CANAL)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Rastreo de Recorrido")
                .setContentText("¡Sigue corriendo!")
                .setContentIntent(intentoPendiente)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        gestorNotificaciones.notify(ID_NOTIFICACION, constructorNotificacion.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gestorLocalizacion.removeUpdates(oyenteLocalizacion);
        oyenteLocalizacion = null;
        gestorLocalizacion = null;

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        podometroActivo = false;

        NotificationManager gestorNotificaciones = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        gestorNotificaciones.cancel(ID_NOTIFICACION);

        Log.d("mdp", "Servicio de Localización destruido");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return enlace;
    }

    protected float obtenerDistancia() {
        return oyenteLocalizacion.obtenerDistanciaDeRecorrido();
    }

    protected void iniciarRecorrido() {
        agregarNotificacion();
        oyenteLocalizacion.nuevoRecorrido();
        oyenteLocalizacion.grabarUbicaciones = true;
        tiempoInicio = SystemClock.elapsedRealtime();
        tiempoFin = 0;

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            podometroActivo = true;
            pasosInicio = 0;
            pasosActuales = 0;
        }
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

    protected int obtenerPasos() {
        return pasosActuales - pasosInicio;
    }

    protected void guardarRecorrido() {
        SharedPreferences sharedPreferences = getSharedPreferences("PreferenciasUsuario", MODE_PRIVATE);
        float pesoRecuperado = sharedPreferences.getFloat("peso", 0.0f);

        ContentValues datosJornada = new ContentValues();
        datosJornada.put(RecorridosObtenidos.distancia_recorrido, obtenerDistancia());
        datosJornada.put(RecorridosObtenidos.duracion_recorrido, (long) obtenerDuracion());
        datosJornada.put(RecorridosObtenidos.fecha_recorrido, obtenerFechaHora());
        datosJornada.put(RecorridosObtenidos.calorias_recorrido, obtenerCalorias(pesoRecuperado));
        datosJornada.put(RecorridosObtenidos.velocidad_recorrido, obtenerVelocidadPromedio());
        datosJornada.put(RecorridosObtenidos.pasos_recorrido, obtenerPasos());

        long idRecorrido = Long.parseLong(getContentResolver().insert(RecorridosObtenidos.uriRecorrido, datosJornada).getLastPathSegment());

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

        oyenteLocalizacion.grabarUbicaciones = false;
        tiempoFin = SystemClock.elapsedRealtime();
        tiempoInicio = 0;
        pasosInicio = 0;
        pasosActuales = 0;
        oyenteLocalizacion.nuevoRecorrido();

        Log.d("mdp", "Recorrido guardado con id = " + idRecorrido);
    }

    private String obtenerFechaHora() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
        Date fecha = new Date();
        return formato.format(fecha);
    }

    private float obtenerCalorias(float peso) {

        float distancia = obtenerDistancia();
        return  peso * distancia * 1.036f;
    }

    private  float obtenerVelocidadPromedio()
        {
        float distancia = obtenerDistancia();
        float d = (float) obtenerDuracion();

        if (d== 0) {
            return 0;
        }

        float velocidadPromedio;

        velocidadPromedio =  (distancia / (d / 3600));

        return velocidadPromedio;
        }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (pasosInicio == 0) {
                pasosInicio = (int) event.values[0];
            }
            pasosActuales = (int) event.values[0];
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se necesita implementar
    }

    public class EnlaceServicioLocalizacion extends Binder {
        public float obtenerDistancia() {
            return Localizacion.this.obtenerDistancia();
        }

        public double obtenerDuracion() {
            return Localizacion.this.obtenerDuracion();
        }

        public int obtenerPasos() {
            return Localizacion.this.obtenerPasos();
        }

        public float obtenerVelocidadPromedio() {
            return Localizacion.this.obtenerVelocidadPromedio();
        }

        public float obtenerCalorias(float peso) {
            return Localizacion.this.obtenerCalorias(peso);
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

        public boolean rastreoActivo() {
            return tiempoInicio != 0;
        }
    }
}

