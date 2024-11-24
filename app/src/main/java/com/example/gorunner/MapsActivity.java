package com.example.gorunner;

import androidx.fragment.app.FragmentActivity;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mapa;
    private long idViaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtener el SupportMapFragment y notificar cuando el mapa esté listo para usarse.
        SupportMapFragment fragmentoMapa = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragmentoMapa.getMapAsync(this);

        Bundle bundle = getIntent().getExtras();
        idViaje = bundle.getLong("idViaje");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;

        // Dibujar polilínea
        Cursor cursor = getContentResolver().query(RecorridosObtenidos.uriUbicacion,
                null, RecorridosObtenidos.recorridoId + " = " + idViaje, null, null);

        PolylineOptions linea = new PolylineOptions().clickable(false);
        LatLng primeraLoc = null;
        LatLng ultimaLoc = null;
        try {
            while(cursor.moveToNext()) {
                LatLng loc = new LatLng(cursor.getDouble(cursor.getColumnIndex(RecorridosObtenidos.latitud_recorrido)),
                        cursor.getDouble(cursor.getColumnIndex(RecorridosObtenidos.longitud_recorrido)));
                if(cursor.isFirst()) {
                    primeraLoc = loc;
                }
                if(cursor.isLast()) {
                    ultimaLoc = loc;
                }
                linea.add(loc);
            }
        } finally {
            cursor.close();
        }

        float zoom = 17.0f;
        if(ultimaLoc != null && primeraLoc != null) {
            // Marcadores de inicio y fin
            mapa.addMarker(new MarkerOptions()
                    .position(primeraLoc)
                    .title("Inicio")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.inicio))); // Icono personalizado

            // Agregar marcador personalizado para el final
            mapa.addMarker(new MarkerOptions()
                    .position(ultimaLoc)
                    .title("Fin")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fin))); // Icono personalizado


            mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(primeraLoc, zoom));

            // Dibujar línea entre el punto inicial y el punto final con un color diferente
            PolylineOptions lineaInicioFin = new PolylineOptions()
                    .add(primeraLoc, ultimaLoc) // Agrega los puntos inicial y final
                    .width(10) // Define el ancho de la línea
                    .color(Color.BLUE) // Cambia el color de la línea
                    .clickable(false);
            mapa.addPolyline(lineaInicioFin);
        }
    }

}
