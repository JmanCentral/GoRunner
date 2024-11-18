package com.example.gorunner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
    private static final int SOLICITUD_PERMISO_ALMACENAMIENTO = 103;
    private ImageView imagenViaje;
    private EditText tituloET;
    private EditText comentarioET;
    private EditText calificacionET;
    private long idViaje;

    private Uri imagenSeleccionadaViaje;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_carrera);

        Bundle bundle = getIntent().getExtras();

        imagenViaje = findViewById(R.id.Fotico);
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

        Uri uriConsultaFila = Uri.withAppendedPath(RecorridosObtenidos.uriRecorrido, "" + idViaje);

        ContentValues valores = new ContentValues();
        valores.put(RecorridosObtenidos.calificacion_recorrido, calificacion);
        valores.put(RecorridosObtenidos.comentario_recorrido, comentarioET.getText().toString());
        valores.put(RecorridosObtenidos.nombre_recorrido, tituloET.getText().toString());

        if(imagenSeleccionadaViaje != null) {
            valores.put(RecorridosObtenidos.imagen_recorrido, imagenSeleccionadaViaje.toString());
        }

        getContentResolver().update(uriConsultaFila, valores, null, null);

        Toast.makeText(getApplicationContext(), "El recorrido se ha guardado  correctamente", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void mostrarDialogoSeleccionImagen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        builder.setMessage("Elija una opción para agregar una imagen");

        // Opción para la cámara
        builder.setPositiveButton("Cámara", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                verificarPermisoCamara(); // Llama al método que verifica los permisos de la cámara
            }
        });

        // Opción para el almacenamiento
        builder.setNegativeButton("Almacenamiento", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                verificarPermisoAlmacenamiento(); // Llama al método que verificará los permisos de almacenamiento
            }
        });

        builder.show();
    }

    private void verificarPermisoAlmacenamiento() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    SOLICITUD_PERMISO_ALMACENAMIENTO);
        } else {
            abrirAlmacenamiento();
        }
    }

    private void abrirAlmacenamiento() {
        Intent intentSeleccionarImagen = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intentSeleccionarImagen.setType("image/*");
        startActivityForResult(intentSeleccionarImagen, RESULTADO_CARGAR_IMAGEN);
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
            startActivityForResult(intentCapturarImagen, SOLICITUD_CAPTURAR_IMAGEN);
    }


    public void CambiarImagen(View v) {
        mostrarDialogoSeleccionImagen();
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
        Cursor cursor = getContentResolver().query(Uri.withAppendedPath(RecorridosObtenidos.uriRecorrido, idViaje + ""), null, null, null, null);

        if(cursor.moveToFirst()) {
            tituloET.setText(cursor.getString(cursor.getColumnIndex(RecorridosObtenidos.nombre_recorrido)));
            comentarioET.setText(cursor.getString(cursor.getColumnIndex(RecorridosObtenidos.comentario_recorrido)));
            calificacionET.setText(cursor.getString(cursor.getColumnIndex(RecorridosObtenidos.calificacion_recorrido)));

            // Si el usuario ha configurado una imagen, mostrarla; de lo contrario, se muestra la imagen predeterminada
            String strUri = cursor.getString(cursor.getColumnIndex(RecorridosObtenidos.imagen_recorrido));
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

        if(calificacion < 0 || calificacion > 10) {
            Toast.makeText(getApplicationContext(), "La calificación debe estar entre 0 y 10", Toast.LENGTH_SHORT).show();
            return -1;
        }
        return calificacion;
    }
}
