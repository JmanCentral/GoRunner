package com.example.gorunner;

import androidx.fragment.app.FragmentActivity;

import android.database.Cursor;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
        Cursor cursor = getContentResolver().query(JornadasObtenidas.uriUbicacion,
                null, JornadasObtenidas.uriUbicacion + " = " + idViaje, null, null);

        PolylineOptions linea = new PolylineOptions().clickable(false);
        LatLng primeraLoc = null;
        LatLng ultimaLoc = null;
        try {
            while(cursor.moveToNext()) {
                LatLng loc = new LatLng(cursor.getDouble(cursor.getColumnIndex(JornadasObtenidas.latitud_jornada)),
                        cursor.getDouble(cursor.getColumnIndex(JornadasObtenidas.longitud_jornada)));
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

        float zoom = 15.0f;
        if(ultimaLoc != null && primeraLoc != null) {
            mapa.addMarker(new MarkerOptions().position(primeraLoc).title("Inicio"));
            mapa.addMarker(new MarkerOptions().position(ultimaLoc).title("Fin"));
            mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(primeraLoc, zoom));
        }
        mapa.addPolyline(linea);
    }
}
