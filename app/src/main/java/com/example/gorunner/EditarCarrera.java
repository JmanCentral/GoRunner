package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditarCarrera extends AppCompatActivity {
    private final int RESULTADO_CARGAR_IMAGEN = 1;
    private final int SOLICITUD_CAPTURAR_IMAGEN = 2;
    private final int SOLICITUD_PERMISO_CAMARA = 100;
    private ImageView imagenViaje;
    private EditText tituloET;
    private EditText comentarioET;
    private EditText calificacionET;
    private long idViaje;

    private Uri imagenSeleccionadaViaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_carrera);

        Bundle bundle = getIntent().getExtras();

        imagenViaje = findViewById(R.id.journeyImg);
        tituloET = findViewById(R.id.titleEditText);
        comentarioET = findViewById(R.id.commentEditText);
        calificacionET = findViewById(R.id.ratingEditText);
        idViaje = bundle.getLong("idViaje");

        imagenSeleccionadaViaje = null;

        llenarCamposEdicion();
    }

    /* Guardar el nuevo título, comentario, imagen y calificación en la base de datos */
    public void Guardar(View v) {
        int calificacion = verificarCalificacion(calificacionET);
        if(calificacion == -1) {
            return;
        }

        Uri uriConsultaFila = Uri.withAppendedPath(JornadasObtenidas.uriJornada, "" + idViaje);

        ContentValues valores = new ContentValues();
        valores.put(JornadasObtenidas.calificacion_jornada, calificacion);
        valores.put(JornadasObtenidas.comentario_jornada, comentarioET.getText().toString());
        valores.put(JornadasObtenidas.nombre_jornada, tituloET.getText().toString());

        if(imagenSeleccionadaViaje != null) {
            valores.put(JornadasObtenidas.imagen_jornada, imagenSeleccionadaViaje.toString());
        }

        getContentResolver().update(uriConsultaFila, valores, null, null);
        finish();
    }

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    SOLICITUD_PERMISO_CAMARA);
        } else {
            // Si el permiso ya ha sido concedido, abrir la cámara
            abrirCamara();
        }
    }

    private void abrirCamara() {
        Intent intentCapturarImagen = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intentCapturarImagen.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intentCapturarImagen, SOLICITUD_CAPTURAR_IMAGEN);
        } else {
            Toast.makeText(this, "Cámara no disponible", Toast.LENGTH_SHORT).show();
        }
    }


    public void CambiarImagen(View v) {
        verificarPermisoCamara();
    }

    @Override
    protected void onActivityResult(int codigoSolicitud, int codigoResultado, Intent data) {
        super.onActivityResult(codigoSolicitud, codigoResultado, data);

        if (codigoResultado == RESULT_OK) {
            switch(codigoSolicitud) {
                case RESULTADO_CARGAR_IMAGEN:
                    if (data != null && data.getData() != null) {
                        Uri uriImagen = data.getData();
                        try {
                            getContentResolver().takePersistableUriPermission(uriImagen, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            InputStream flujoImagen = getContentResolver().openInputStream(uriImagen);
                            Bitmap imagenSeleccionada = BitmapFactory.decodeStream(flujoImagen);
                            imagenViaje.setImageBitmap(imagenSeleccionada);
                            imagenSeleccionadaViaje = uriImagen;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(this, "No seleccionaste una imagen", Toast.LENGTH_LONG).show();
                    }
                    break;

                case SOLICITUD_CAPTURAR_IMAGEN:
                    Bundle extras = data.getExtras();
                    Bitmap imagenBitmap = (Bitmap) extras.get("data");
                    imagenViaje.setImageBitmap(imagenBitmap);

                    // Opcional: Guardar el Bitmap en almacenamiento para obtener un URI
                    imagenSeleccionadaViaje = guardarImagenEnAlmacenamiento(imagenBitmap);
                    break;
            }
        }
    }

    // Método para guardar la imagen en almacenamiento y obtener el URI
    private Uri guardarImagenEnAlmacenamiento(Bitmap bitmap) {
        Uri uri = null;
        try {
            File archivo = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "imagen_viaje_" + idViaje + ".jpg");
            FileOutputStream fos = new FileOutputStream(archivo);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            uri = Uri.fromFile(archivo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    /* Asigna a los EditTexts el texto inicial desde la base de datos */
    private void llenarCamposEdicion() {
        Cursor cursor = getContentResolver().query(Uri.withAppendedPath(JornadasObtenidas.uriJornada, idViaje + ""), null, null, null, null);

        if(cursor.moveToFirst()) {
            tituloET.setText(cursor.getString(cursor.getColumnIndex(JornadasObtenidas.nombre_jornada)));
            comentarioET.setText(cursor.getString(cursor.getColumnIndex(JornadasObtenidas.comentario_jornada)));
            calificacionET.setText(cursor.getString(cursor.getColumnIndex(JornadasObtenidas.calificacion_jornada)));

            // Si el usuario ha configurado una imagen, mostrarla; de lo contrario, se muestra la imagen predeterminada
            String strUri = cursor.getString(cursor.getColumnIndex(JornadasObtenidas.imagen_jornada));
            if(strUri != null) {
                try {
                    final Uri uriImagen = Uri.parse(strUri);
                    final InputStream flujoImagen = getContentResolver().openInputStream(uriImagen);
                    final Bitmap imagenSeleccionada = BitmapFactory.decodeStream(flujoImagen);
                    imagenViaje.setImageBitmap(imagenSeleccionada);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int verificarCalificacion(EditText nuevaCalificacion) {
        int calificacion;
        try {
            calificacion = Integer.parseInt(nuevaCalificacion.getText().toString());
        } catch(Exception e) {
            Toast.makeText(getApplicationContext(), "Lo siguiente no es un número: " + nuevaCalificacion.getText().toString(), Toast.LENGTH_SHORT).show();
            return -1;
        }

        if(calificacion < 0 || calificacion > 5) {
            Toast.makeText(getApplicationContext(), "La calificación debe estar entre 0 y 5", Toast.LENGTH_SHORT).show();
            return -1;
        }
        return calificacion;
    }
}
