package com.example.gorunner;

import androidx.fragment.app.FragmentActivity;

import android.database.Cursor;
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
        linea.color(0xff0000ff);
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
        }
        mapa.addPolyline(linea);
    }
}