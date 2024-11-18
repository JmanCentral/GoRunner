package com.example.gorunner;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ContadorPasos implements SensorEventListener {

    private SensorManager sensorManager;  // Para manejar los sensores
    private Sensor acelerometro;          // El sensor de acelerómetro
    private float[] gravity = new float[3];   // Para almacenar los valores de gravedad
    private float[] linearAcceleration = new float[3];  // Para almacenar la aceleración lineal
    private int contadorPasos = 0;            // Contador de pasos

    public ContadorPasos(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    // Método para empezar a escuchar los eventos del acelerómetro
    public void startListening() {
        sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
    }

    // Método para detener la escucha de los eventos
    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    // Método que devuelve el número de pasos detectados
    public int obtenerPasos() {
        return contadorPasos;
    }

    // Método del SensorEventListener que se llama cuando el acelerómetro detecta un cambio
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Filtrar los valores de aceleración para obtener la aceleración lineal (sin la gravedad)
            final float alpha = 0.8f;
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Calculamos la aceleración lineal
            linearAcceleration[0] = event.values[0] - gravity[0];
            linearAcceleration[1] = event.values[1] - gravity[1];
            linearAcceleration[2] = event.values[2] - gravity[2];

            // Magnitud de la aceleración total
            float aceleracionTotal = (float) Math.sqrt(Math.pow(linearAcceleration[0], 2) +
                    Math.pow(linearAcceleration[1], 2) +
                    Math.pow(linearAcceleration[2], 2));

            // Umbral para detectar un paso (ajustable)
            if (aceleracionTotal > 12) {
                contadorPasos++;  // Incrementamos el contador de pasos

                // Imprimir en log para depuración
                Log.d("Pasos", "Paso detectado: " + contadorPasos);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
