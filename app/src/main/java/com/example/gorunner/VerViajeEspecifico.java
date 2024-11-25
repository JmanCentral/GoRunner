package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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

    // Definición de vistas para mostrar los detalles del viaje
    private ImageView imagenViaje;
    private TextView distanciaTV;
    private TextView velocidadPromedioTV;
    private TextView tiempoTV;
    private TextView fechaTV;
    private TextView calificacionTV;
    private TextView comentarioTV;
    private TextView tituloTV;
    private TextView caloriasTV;
    private TextView pasosTV;

    // Identificador único del viaje a mostrar
    private long idViaje;

    // Manejador para el ContentObserver
    private Handler manejador = new Handler();

    // Clase interna para observar cambios en la base de datos
    protected class MiObservador extends ContentObserver {

        // Método para manejar cambios en la base de datos
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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_viaje_especifico);

        // Obtener los datos del Intent
        Bundle bundle = getIntent().getExtras();

        // Inicialización de las vistas (TextViews e ImageView)
        imagenViaje = findViewById(R.id.Imagenviaje);
        distanciaTV = findViewById(R.id.distanciarecorrido);
        velocidadPromedioTV = findViewById(R.id.velociadpromedio);
        tiempoTV = findViewById(R.id.tiemporecorrido);
        fechaTV = findViewById(R.id.fecharecorrido);
        calificacionTV = findViewById(R.id.calificacionrecorrido);
        comentarioTV = findViewById(R.id.comentariosrecorrido);
        tituloTV = findViewById(R.id.viajeespecificorecorrido);
        caloriasTV = findViewById(R.id.caloriasquemadas);
        pasosTV = findViewById(R.id.pasospromedio);

        // Obtener el ID del viaje desde el Bundle
        idViaje = bundle.getLong("idViaje");

        // Llenar la vista con los datos del viaje
        llenarVista();
        // Registrar un ContentObserver para observar cambios en los datos
        getContentResolver().registerContentObserver(
                RecorridosObtenidos.todas, true, new MiObservador(manejador));
    }

    // Botones de la vista
    public void Editar(View vista) {
        // Dirigir a la actividad para editar los campos de este viaje
        Intent actividadEditar = new Intent(VerViajeEspecifico.this, EditarCarrera.class);
        Bundle b = new Bundle();
        b.putLong("idViaje", idViaje);
        actividadEditar.putExtras(b);
        startActivity(actividadEditar);
    }

    // Posible emplementación para eliminar el viaje
    public void Eliminar(View vista) {
        // Eliminar este viaje de la base de datos
        getContentResolver().delete(Uri.withAppendedPath(RecorridosObtenidos.uriRecorrido,
                idViaje + ""), null, null);
    }

    // Botón para ver el mapa del viaje
    public void Mapa(View vista) {
        // Mostrar este viaje en una actividad de Google Maps
        Intent mapa = new Intent(VerViajeEspecifico.this, MapsActivity.class);
        Bundle b = new Bundle();
        b.putLong("idViaje", idViaje);
        mapa.putExtras(b);
        startActivity(mapa);
    }

    // Mostrar los datos del viaje en la vista
    private void llenarVista() {

        // Consultar la base de datos para obtener los datos del viaje
        Cursor c = getContentResolver().query(Uri.withAppendedPath(RecorridosObtenidos.uriRecorrido,
                idViaje + ""), null, null, null, null);

        // Si la consulta tiene resultados, llenar los elementos de la vista
        if (c.moveToFirst()) {
            double distancia = c.getDouble(c.getColumnIndex(RecorridosObtenidos.distancia_recorrido));
            long tiempo = c.getLong(c.getColumnIndex(RecorridosObtenidos.duracion_recorrido));
            float calorias = c.getFloat(c.getColumnIndex(RecorridosObtenidos.calorias_recorrido));
            int pasos = c.getInt(c.getColumnIndex(RecorridosObtenidos.pasos_recorrido));
            float velocidadPromedio = c.getFloat(c.getColumnIndex(RecorridosObtenidos.velocidad_recorrido));

            // Calcular las horas, minutos y segundos a partir del tiempo en segundos
            long horas = tiempo / 3600;
            long minutos = (tiempo % 3600) / 60;
            long segundos = tiempo % 60;

            // Establecer los valores en los TextViews correspondientes
            distanciaTV.setText(String.format("%.2f KM", distancia));
            velocidadPromedioTV.setText(String.format("%.2f KM/H", velocidadPromedio));
            tiempoTV.setText(String.format("%02d:%02d:%02d", horas, minutos, segundos));
            caloriasTV.setText(String.format("%.2f CAL", calorias));
            pasosTV.setText(String.format("%d", pasos));

            // La fecha se almacena como yyyy-mm-dd, convertir a dd-mm-yyyy
            String fecha = c.getString(c.getColumnIndex(RecorridosObtenidos.fecha_recorrido));
            String[] partesFecha = fecha.split("-");
            fecha = partesFecha[2] + "/" + partesFecha[1] + "/" + partesFecha[0];

            fechaTV.setText(fecha);
            // Mostrar la calificación, comentario y título del recorrido
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
                    Bitmap imagenRedimensionada = Bitmap.createScaledBitmap(imagenSeleccionada, 800, 800, true);

                    imagenViaje.setImageBitmap(imagenRedimensionada);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

