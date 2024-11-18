package com.example.gorunner;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

public class MiLocalizacionListener implements LocationListener {
    ArrayList<Location> ubicaciones;
    boolean grabarUbicaciones;

    public MiLocalizacionListener() {
        nuevaJornada();
        grabarUbicaciones = false;
    }

    public void nuevaJornada() {
        ubicaciones = new ArrayList<Location>();
    }

    public float obtenerDistanciaDeJornada() {
        if (ubicaciones.size() < 2) {
            return 0;  // Si no hay suficientes ubicaciones, no se puede calcular la distancia
        }

        float totalDistance = 0;
        for (int i = 0; i < ubicaciones.size() - 1; i++) {
            totalDistance += ubicaciones.get(i).distanceTo(ubicaciones.get(i + 1));
        }
        return totalDistance / 1000;
    }

    public ArrayList<Location> obtenerUbicaciones() {
        return ubicaciones;
    }


    @Override
    public void onLocationChanged(Location location) {
        if(grabarUbicaciones) {
            ubicaciones.add(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // information about the signal, i.e. number of satellites
        Log.d("mdp", "onStatusChanged: " + provider + " " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        // the user enabled (for example) the GPS
        Log.d("mdp", "onProviderEnabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // the user disabled (for example) the GPS
        Log.d("mdp", "onProviderDisabled: " + provider);
    }
}
