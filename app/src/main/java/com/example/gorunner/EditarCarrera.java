package com.example.gorunner;

import androidx.annotation.NonNull;
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
import android.database.sqlite.SQLiteException;
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

    // Constantes para solicitudes de permisos e interacciones con la cámara y almacenamiento.
    private final int RESULTADO_CARGAR_IMAGEN = 1;
    private final int SOLICITUD_CAPTURAR_IMAGEN = 2;
    private final int SOLICITUD_PERMISO_CAMARA = 100;
    private static final int SOLICITUD_PERMISO_ALMACENAMIENTO = 103;

    // Variables para gestionar los elementos de la interfaz y los datos del recorrido.
    private ImageView imagenViaje;
    private EditText tituloET;
    private EditText comentarioET;
    private EditText calificacionET;
    private long idViaje;

    private Uri imagenSeleccionadaViaje;

    /**
     * Método que se ejecuta al crear la actividad. Inicializa los elementos de la interfaz
     * y carga la información del recorrido desde la base de datos.
     *
     * @param savedInstanceState Estado guardado de la actividad (si existe).
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_carrera);

        Bundle bundle = getIntent().getExtras();

        imagenViaje = findViewById(R.id.Fotico);
        tituloET = findViewById(R.id.titulo);
        comentarioET = findViewById(R.id.comentario);
        calificacionET = findViewById(R.id.calificacion);
        idViaje = bundle.getLong("idViaje");

        imagenSeleccionadaViaje = null;

        // Llena los campos con la información existente del recorrido.
        llenarCamposEdicion();
    }

    /* Guardar el nuevo título, comentario, imagen y calificación en la base de datos */
    public void Guardar(View v) {
        try {
            // Verificar la calificación antes de continuar.
            int calificacion = verificarCalificacion(calificacionET);
            if (calificacion == -1) {
                Toast.makeText(getApplicationContext(), "Por favor, ingrese una calificación válida.", Toast.LENGTH_SHORT).show();
                return; // Detiene la ejecución si la calificación no es válida.
            }

            // Validar campos de texto requeridos.
            String comentario = comentarioET.getText().toString().trim();
            String titulo = tituloET.getText().toString().trim();
            if (comentario.isEmpty() || titulo.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Los campos de comentario y título no pueden estar vacíos.", Toast.LENGTH_SHORT).show();
                return; // Detiene la ejecución si los campos están vacíos.
            }

            Uri uriConsultaFila = Uri.withAppendedPath(RecorridosObtenidos.uriRecorrido, String.valueOf(idViaje));

            // Crear un objeto ContentValues para actualizar la base de datos.
            ContentValues valores = new ContentValues();
            valores.put(RecorridosObtenidos.calificacion_recorrido, calificacion);
            valores.put(RecorridosObtenidos.comentario_recorrido, comentario);
            valores.put(RecorridosObtenidos.nombre_recorrido, titulo);

            // Verificar si hay una imagen seleccionada y añadirla.
            if (imagenSeleccionadaViaje != null) {
                valores.put(RecorridosObtenidos.imagen_recorrido, imagenSeleccionadaViaje.toString());
            }

            // Realizar la actualización en la base de datos.
            int filasActualizadas = getContentResolver().update(uriConsultaFila, valores, null, null);
            if (filasActualizadas > 0) {
                Toast.makeText(getApplicationContext(), "El recorrido se ha guardado correctamente.", Toast.LENGTH_SHORT).show();
                finish(); // Finaliza la actividad si todo fue exitoso.
            } else {
                Toast.makeText(getApplicationContext(), "No se encontró el recorrido para actualizar.", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException e) {
            // Captura problemas con argumentos no válidos.
            Toast.makeText(getApplicationContext(), "Error en los datos ingresados: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e) {
            // Captura problemas específicos de la base de datos.
            Toast.makeText(getApplicationContext(), "Error al guardar en la base de datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Captura cualquier otra excepción no prevista.
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Ocurrió un error inesperado.", Toast.LENGTH_SHORT).show();
        }
    }


    // Muestra un diálogo para seleccionar la imagen desde la cámara o el almacenamiento.
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

    /**
     * Verifica si se tiene permiso para acceder al almacenamiento externo. Si no, solicita el permiso.
     */
    private void verificarPermisoAlmacenamiento() {
        // Verificar si el permiso de almacenamiento ha sido concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Si no está concedido, solicitar el permiso
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    SOLICITUD_PERMISO_ALMACENAMIENTO);
        } else {
            // Si el permiso ya ha sido concedido, abrir el almacenamiento
            abrirAlmacenamiento();
        }
    }


    /**
     * Abre el selector de imágenes del almacenamiento externo.
     */
    private void abrirAlmacenamiento() {
        Intent intentSeleccionarImagen = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intentSeleccionarImagen.setType("image/*");
        startActivityForResult(intentSeleccionarImagen, RESULTADO_CARGAR_IMAGEN);
    }

    /**
     * Verifica si se tiene permiso para usar la cámara. Si no, solicita el permiso.
     */

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    SOLICITUD_PERMISO_CAMARA);
        }
        else {
            abrirCamara();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Si el permiso fue concedido
            if (requestCode == SOLICITUD_PERMISO_ALMACENAMIENTO) {
                abrirAlmacenamiento(); // Abre el almacenamiento al instante
            } else if (requestCode == SOLICITUD_PERMISO_CAMARA) {
                abrirCamara(); // Abre la cámara al instante
            }
        } else {
            // Si el permiso fue denegado
            if (requestCode == SOLICITUD_PERMISO_ALMACENAMIENTO) {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            } else if (requestCode == SOLICITUD_PERMISO_CAMARA) {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Abre la cámara para capturar una imagen.
     */
    private void abrirCamara() {
        Intent intentCapturarImagen = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intentCapturarImagen, SOLICITUD_CAPTURAR_IMAGEN);
    }

    // Muestra el diálogo para seleccionar la imagen
    public void CambiarImagen(View v) {
        mostrarDialogoSeleccionImagen();
    }

    /**
     * Maneja los resultados de las actividades para seleccionar o capturar imágenes.
     */
    @Override
    protected void onActivityResult(int codigoSolicitud, int codigoResultado, Intent data) {
        super.onActivityResult(codigoSolicitud, codigoResultado, data);

        if (codigoResultado == RESULT_OK) {
            switch(codigoSolicitud) {
                case RESULTADO_CARGAR_IMAGEN:
                    if (data != null && data.getData() != null) {
                        Uri uriImagen = data.getData();
                        try {
                            // Mantener permiso persistente para acceder a la imagen
                            getContentResolver().takePersistableUriPermission(uriImagen, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            // Obtener flujo de entrada de la imagen
                            InputStream flujoImagen = getContentResolver().openInputStream(uriImagen);

                            // Decodificar la imagen en un Bitmap
                            Bitmap imagenSeleccionada = BitmapFactory.decodeStream(flujoImagen);

                            // Redimensionar el Bitmap (por ejemplo, a 800x800 píxeles)
                            Bitmap imagenRedimensionada = Bitmap.createScaledBitmap(imagenSeleccionada, 800, 800, true);

                            // Asignar el Bitmap redimensionado al ImageView
                            imagenViaje.setImageBitmap(imagenRedimensionada);

                            // Guardar el URI de la imagen seleccionada
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

                    // Redimensionar el Bitmap
                    Bitmap imagenRedimensionada = Bitmap.createScaledBitmap(imagenBitmap, 800, 800, true);

                    imagenViaje.setImageBitmap(imagenRedimensionada);

                    // Opcional: Guardar la imagen redimensionada en almacenamiento
                    imagenSeleccionadaViaje = guardarImagenEnAlmacenamiento(imagenRedimensionada);
                    break;
            }
        }
    }

    // Método para guardar la imagen en almacenamiento y obtener el URI
    private Uri guardarImagenEnAlmacenamiento(Bitmap bitmap) {
        int quality = 100;
        Uri uri = null;
        try {
            File archivo = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "imagen_viaje_" + idViaje + ".jpg");
            FileOutputStream fos = new FileOutputStream(archivo);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
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

    // Verifica si la calificación es válida y si no , no lo deja actualizar la carrera
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
