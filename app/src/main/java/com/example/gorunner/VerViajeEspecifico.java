package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

public class VerViajeEspecifico extends AppCompatActivity {
    private ImageView imagenViaje;
    private TextView distanciaTV;
    private TextView velocidadPromedioTV;
    private TextView tiempoTV;
    private TextView fechaTV;
    private TextView calificacionTV;
    private TextView comentarioTV;
    private TextView tituloTV;

    private long idViaje;

    private Handler manejador = new Handler();

    // Observador que se notifica cuando ocurre una inserción o eliminación en la URI dada
    protected class MiObservador extends ContentObserver {

        public MiObservador(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            // Llamado cuando algo en la base de datos cambia
            // Actualizar la vista
            llenarVista();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_viaje_especifico);

        Bundle bundle = getIntent().getExtras();

        imagenViaje = findViewById(R.id.ViewSingleJourney_journeyImg);
        distanciaTV = findViewById(R.id.Statistics_recordDistance);
        velocidadPromedioTV = findViewById(R.id.Statistics_distanceToday);
        tiempoTV = findViewById(R.id.Statistics_timeToday);
        fechaTV = findViewById(R.id.ViewSingleJourney_dateText);
        calificacionTV = findViewById(R.id.ViewSingleJourney_ratingText);
        comentarioTV = findViewById(R.id.ViewSingleJourney_commentText);
        tituloTV = findViewById(R.id.ViewSingleJourney_titleText);
        idViaje = bundle.getLong("idViaje");

        llenarVista();
        getContentResolver().registerContentObserver(
                RecorridosObtenidos.todas, true, new MiObservador(manejador));
    }

    public void Editar(View vista) {
        // Dirigir a la actividad para editar los campos de este viaje
        Intent actividadEditar = new Intent(VerViajeEspecifico.this, EditarCarrera.class);
        Bundle b = new Bundle();
        b.putLong("idViaje", idViaje);
        actividadEditar.putExtras(b);
        startActivity(actividadEditar);
    }

    public void Mapa(View vista) {
        // Mostrar este viaje en una actividad de Google Maps
        Intent mapa = new Intent(VerViajeEspecifico.this, MapsActivity.class);
        Bundle b = new Bundle();
        b.putLong("idViaje", idViaje);
        mapa.putExtras(b);
        startActivity(mapa);
    }

    private void llenarVista() {
        // Usar el proveedor de contenido para cargar datos de la base de datos y mostrarlos en las vistas de texto
        Cursor c = getContentResolver().query(Uri.withAppendedPath(RecorridosObtenidos.uriRecorrido,
                idViaje + ""), null, null, null, null);

        if (c.moveToFirst()) {
            double distancia = c.getDouble(c.getColumnIndex(RecorridosObtenidos.distancia_recorrido));
            long tiempo = c.getLong(c.getColumnIndex(RecorridosObtenidos.duracion_recorrido));
            double velocidadPromedio = 0;

            if (tiempo != 0) {
                velocidadPromedio = distancia / (tiempo / 3600.0);
            }

            long horas = tiempo / 3600;
            long minutos = (tiempo % 3600) / 60;
            long segundos = tiempo % 60;

            distanciaTV.setText(String.format("%.2f KM", distancia));
            velocidadPromedioTV.setText(String.format("%.2f KM/H", velocidadPromedio));
            tiempoTV.setText(String.format("%02d:%02d:%02d", horas, minutos, segundos));

            // La fecha se almacena como yyyy-mm-dd, convertir a dd-mm-yyyy
            String fecha = c.getString(c.getColumnIndex(RecorridosObtenidos.fecha_recorrido));
            String[] partesFecha = fecha.split("-");
            fecha = partesFecha[2] + "/" + partesFecha[1] + "/" + partesFecha[0];

            fechaTV.setText(fecha);
            calificacionTV.setText(c.getInt(c.getColumnIndex(RecorridosObtenidos.calificacion_recorrido)) + "");
            comentarioTV.setText(c.getString(c.getColumnIndex(RecorridosObtenidos.comentario_recorrido)));
            tituloTV.setText(c.getString(c.getColumnIndex(RecorridosObtenidos.nombre_recorrido)));

            // Si el usuario ha configurado una imagen, mostrarla; en caso contrario, mostrar la imagen predeterminada
            String uriStr = c.getString(c.getColumnIndex(RecorridosObtenidos.imagen_recorrido));
            if (uriStr != null) {
                try {
                    final Uri imagenUri = Uri.parse(uriStr);
                    final InputStream flujoImagen = getContentResolver().openInputStream(imagenUri);
                    final Bitmap imagenSeleccionada = BitmapFactory.decodeStream(flujoImagen);
                    imagenViaje.setImageBitmap(imagenSeleccionada);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

