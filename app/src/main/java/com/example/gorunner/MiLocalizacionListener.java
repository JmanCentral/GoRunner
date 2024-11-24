package com.example.gorunner;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

// Clase para el oyente de localización
public class MiLocalizacionListener implements LocationListener {
    ArrayList<Location> ubicaciones;
    boolean grabarUbicaciones;

    // Constructor de la clase
    public MiLocalizacionListener() {
        nuevoRecorrido();
        grabarUbicaciones = false;
    }


    // Crea un nuevo recorrido
    public void nuevoRecorrido() {
        ubicaciones = new ArrayList<Location>();
    }

    public float obtenerDistanciaDeRecorrido() {

        if(ubicaciones.size() <= 1) {
            return 0;
        }

        return ubicaciones.get(0).distanceTo(ubicaciones.get(ubicaciones.size() - 1)) / 1000;
    }


    // Obtiene la lista de ubicaciones
    public ArrayList<Location> obtenerUbicaciones() {
        return ubicaciones;
    }


    @Override
    // Cuando se cambia la ubicación, se añade a la lista de ubicaciones
    public void onLocationChanged(Location location) {
        if(grabarUbicaciones) {
            ubicaciones.add(location);
        }
    }

    @Override
    // Cuando se cambia el estado de la localización, se muestra un mensaje
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // information about the signal, i.e. number of satellites
        Log.d("mdp", "onStatusChanged: " + provider + " " + status);
    }

    @Override
    // Cuando se habilita la localización, se muestra un mensaje
    public void onProviderEnabled(String provider) {
        // the user enabled (for example) the GPS
        Log.d("mdp", "onProviderEnabled: " + provider);
    }

    @Override
    // Cuando se deshabilita la localización, se muestra un mensaje
    public void onProviderDisabled(String provider) {
        // the user disabled (for example) the GPS
        Log.d("mdp", "onProviderDisabled: " + provider);
    }
}
