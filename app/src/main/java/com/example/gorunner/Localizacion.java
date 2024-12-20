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

// Clase para el servicio de localización
public class Localizacion extends Service implements SensorEventListener {
    // Servicio de localización
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

        // Crear el gestor de localización
        gestorLocalizacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Crear el oyente de localización
        oyenteLocalizacion = new MiLocalizacionListener();
        // Habilitar la localización
        oyenteLocalizacion.grabarUbicaciones = false;

        // Obtener el sensor de pasos
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

    // Mostrar una notificación cuando se inicia el servicio de localización
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
                .setSmallIcon(R.drawable.corredor)
                .setContentTitle("Rastreo de Recorrido")
                .setContentText("¡Sigue corriendo!")
                .setContentIntent(intentoPendiente)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        gestorNotificaciones.notify(ID_NOTIFICACION, constructorNotificacion.build());
    }

    @Override
    // Cuando se destruye el servicio de localización, eliminar el oyente de localización
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
    // Cuando se crea el enlace con el servicio de localización, devolver el enlace
    public IBinder onBind(Intent intent) {
        return enlace;
    }

    // Métodos para el servicio de localización

    // Obtiene la distancia recorrida en km
    protected float obtenerDistancia() {
        return oyenteLocalizacion.obtenerDistanciaDeRecorrido();
    }

    // Inicia un nuevo recorrido
    protected void iniciarRecorrido() {

        // Si ya se está rastreando, no hacer nada
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

    // Obtiene la duración del recorrido en segundos
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

    // Obtiene los pasos del recorrido
    protected int obtenerPasos() {
        return pasosActuales - pasosInicio;
    }

    // Guarda el recorrido en la base de datos junto con sus atributos
    protected void guardarRecorrido() {

        // recupera el peso desde el shared preferences
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
        oyenteLocalizacion.grabarUbicaciones = false;



        // Reiniciar el oyente de localización

        tiempoFin = SystemClock.elapsedRealtime();
        tiempoInicio = 0;
        pasosInicio = 0;
        pasosActuales = 0;
        oyenteLocalizacion.nuevoRecorrido();

        Log.d("mdp", "Recorrido guardado con id = " + idRecorrido);
    }

    // Obtiene la fecha y hora actual
    private String obtenerFechaHora() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
        Date fecha = new Date();
        return formato.format(fecha);
    }

    // Calcula las calorias del recorrido
    private float obtenerCalorias(float peso) {

        float distancia = obtenerDistancia();
        return  peso * distancia * 1.036f;
    }

    // Calcula la velocidad promedio
    private  float obtenerVelocidadPromedio()
        {
        float distancia = obtenerDistancia();
        float d = (float) obtenerDuracion();

        float d1;
        if (d== 0) {
            return 0;
        }

        float velocidadPromedio;

        d1 = d / 3600;

        velocidadPromedio =  distancia/d1;

        return velocidadPromedio;
        }

    @Override
    // Registra los cambios en el sensor de pasos
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (pasosInicio == 0) {
                pasosInicio = (int) event.values[0];
            }
            pasosActuales = (int) event.values[0];
        }
    }

    //
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

    // el Enlace para el servicio de localización que herada de Binder se encarga de devolver los datos del servicio
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

