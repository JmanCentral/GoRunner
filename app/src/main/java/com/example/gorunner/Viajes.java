package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Viajes extends AppCompatActivity {
        private GifPlayer gif;
        private Localizacion.EnlaceServicioLocalizacion locationService;

        private TextView distanciaTexto;
        private TextView velocidadPromedioTexto;
        private TextView duracionTexto;
        private TextView caloriasPromedio;
        private TextView pasosTotales;

        private Button botonIniciar;
        private Button botonDetener;
        private static final int PERMISSION_GPS_CODE = 1;
        private static final int PERMISSION_ACTIVITY_RECOGNITION_CODE = 2;

        @SuppressLint("MissingInflatedId")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_viajes);

            if (!isLocationEnabled()) {
                showLocationDisabledAlert();
            }

            gif = findViewById(R.id.gif);
            gif.setGifImageResource(R.drawable.atleta32);
            gif.pausar();

            distanciaTexto = findViewById(R.id.distanceText);
            duracionTexto = findViewById(R.id.durationText);
            velocidadPromedioTexto = findViewById(R.id.avgSpeedText);
            caloriasPromedio = findViewById(R.id.avgSpeedText2);
            pasosTotales = findViewById(R.id.pasos);

            botonIniciar = findViewById(R.id.startButton);
            botonDetener = findViewById(R.id.stopButton);

            botonDetener.setEnabled(false);
            botonIniciar.setEnabled(false);


            handlePermissions();

            startService(new Intent(this, Localizacion.class));
            bindService(
                    new Intent(this, Localizacion.class), lsc, Context.BIND_AUTO_CREATE);
        }


        // revisará el servicio de ubicación para obtener la distancia y la duración
        private Handler postBack = new Handler();

        private ServiceConnection lsc = new ServiceConnection() {
            @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                locationService = (Localizacion.EnlaceServicioLocalizacion) iBinder;

                // si actualmente se está rastreando, habilitar el botón de detener y deshabilitar el de iniciar
                iniciarbotones();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (locationService != null) {
                            // obtener la distancia y duración desde el servicio
                            float d = (float) locationService.obtenerDuracion();
                            long duracion = (long) d;  // en segundos
                            float distancia = locationService.obtenerDistancia();

                            final int pasos = locationService.obtenerPasos();
                            //Log.d("Viajes", "Pasos totales obtenidos: " + pasos);

                            long horas = duracion / 3600;
                            long minutos = (duracion % 3600) / 60;
                            long segundos = duracion % 60;

                            float velocidadPromedio = locationService.obtenerVelocidadPromedio();


                            SharedPreferences sharedPreferences = getSharedPreferences("PreferenciasUsuario", MODE_PRIVATE);
                            float pesoRecuperado = sharedPreferences.getFloat("peso", 0.0f);

                            // Calcular calorías usando el método del servicio
                            float caloriasQuemadas = locationService.obtenerCalorias(pesoRecuperado);

                            final String tiempo = String.format("%02d:%02d:%02d", horas, minutos, segundos);
                            final String dist = String.format("%.2f KM", distancia);
                            final String promedio = String.format("%.2f KM/H", velocidadPromedio);
                            final String calorias = String.format("%.2f CAL", caloriasQuemadas);

                            postBack.post(new Runnable() {
                                @Override
                                public void run() {
                                    // enviar cambios a la UI en el hilo principal
                                    duracionTexto.setText(tiempo);
                                    velocidadPromedioTexto.setText(promedio);
                                    distanciaTexto.setText(dist);
                                    caloriasPromedio.setText(calorias);
                                    pasosTotales.setText(String.valueOf(pasos));

                                }
                            });

                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            locationService = null;
        }
    };

    // cada vez que la actividad se recarga mientras aún se rastrea un viaje (si se hace clic en el botón atrás, por ejemplo)
    private void iniciarbotones() {
        // sin permisos no hay botones
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            botonDetener.setEnabled(false);
            botonIniciar.setEnabled(false);
            return;
        }

        // si actualmente se está rastreando, habilitar el botón de detener y deshabilitar el de iniciar
        if(locationService != null && locationService.rastreoActivo()) {
            botonDetener.setEnabled(true);
            botonIniciar.setEnabled(false);
            gif.reproducir();
        } else {
            botonDetener.setEnabled(false);
            botonIniciar.setEnabled(true);
        }
    }




    public void Inicio(View view) {
        SharedPreferences sharedPreferences = getSharedPreferences("PreferenciasUsuario", MODE_PRIVATE);
        float pesoRecuperado = sharedPreferences.getFloat("peso", 0.0f);

        if (pesoRecuperado == 0.0f) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Peso no configurado");
            alertDialogBuilder.setMessage("Por favor, configure su peso antes de iniciar la actividad.");
            alertDialogBuilder.setPositiveButton("Configurar", (dialog, which) -> {
                // Redirigir al usuario a la pantalla de configuración de peso
                Intent intent = new Intent(this, pesoActivity.class);
                startActivity(intent);
            });
            alertDialogBuilder.create().show();
        }
        else {
            gif.reproducir();
            // Iniciar el temporizador y el rastreo de ubicaciones GPS
            locationService.iniciarRecorrido();
            botonIniciar.setEnabled(false);
            botonDetener.setEnabled(true);
        }
    }

    public void Fin(View view) {
        float distancia = locationService.obtenerDistancia();
        locationService.guardarRecorrido();

        botonIniciar.setEnabled(false);
        botonDetener.setEnabled(false);

        gif.pausar();

        DialogFragment modal = FinishedTrackingDialogue.newInstance(String.format("%.2f KM", distancia));
        modal.show(getSupportFragmentManager(), "Finished");

        long idViaje = obtenerIdUltimoViaje(); // Método para obtener el ID del último viaje

        // Crear un Intent para iniciar la actividad EditarCarrera
        Intent intent = new Intent(this, EditarCarrera.class);
        intent.putExtra("idViaje", idViaje);
        startActivity(intent);

    }

    private long obtenerIdUltimoViaje() {
        long idUltimoViaje = -1;


        Cursor cursor = getContentResolver().query(
                RecorridosObtenidos.uriRecorrido,
                new String[]{"recorridoID"},
                null,
                null,
                "recorridoID DESC LIMIT 1"
        );

        if (cursor != null && cursor.moveToFirst()) {
            idUltimoViaje = cursor.getLong(cursor.getColumnIndex("recorridoID")); // Usamos 'recorridoID'
            cursor.close();
        }

        return idUltimoViaje;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(lsc != null) {
            unbindService(lsc);
            lsc = null;
        }
    }

    public static class FinishedTrackingDialogue extends DialogFragment {
        public static FinishedTrackingDialogue newInstance(String distancia) {
            Bundle savedInstanceState = new Bundle();
            savedInstanceState.putString("distancia", distancia);
            FinishedTrackingDialogue frag = new FinishedTrackingDialogue();
            frag.setArguments(savedInstanceState);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Tu viaje ha sido guardado. Corriste un total de " + getArguments().getString("distancia") + " KM")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // regresar a la pantalla principal
                            getActivity().finish();
                        }
                    });
            // Crear el objeto AlertDialog y devolverlo
            return builder.create();
        }
    }

    public static class NoPermissionDialogue extends DialogFragment {
        public static NoPermissionDialogue newInstance() {
            return new NoPermissionDialogue();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage("¡Se requiere GPS para rastrear tu viaje!")
                    .setPositiveButton("Habilitar GPS", (dialog, id) ->
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GPS_CODE))
                    .setNegativeButton("Cancelar", null)
                    .create();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationDisabledAlert() {
        new AlertDialog.Builder(this)
                .setMessage("La ubicación está deshabilitada. ¿Deseas habilitarla?")
                .setCancelable(false)
                .setPositiveButton("Sí", (dialog, id) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    private void requestPermission(String permission, int requestCode, String rationaleMessage, Runnable onPermissionGranted) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                new AlertDialog.Builder(this)
                        .setMessage(rationaleMessage)
                        .setPositiveButton("Aceptar", (dialog, which) ->
                                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode))
                        .setNegativeButton("Cancelar", null)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        } else {
            onPermissionGranted.run();
        }
    }

    private void handlePermissions() {
        requestPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                PERMISSION_GPS_CODE,
                "Se necesita acceso al GPS para rastrear tu ubicación.",
                this::onGpsPermissionGranted
        );
    }

    private void onGpsPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermission(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    PERMISSION_ACTIVITY_RECOGNITION_CODE,
                    "Se necesita acceso al sensor de actividad para rastrear tus pasos.",
                    () -> Log.d("Viajes", "Permiso de reconocimiento de actividad concedido.")
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_GPS_CODE:
                    onGpsPermissionGranted();
                    iniciarbotones();
                    if (locationService != null) {
                        locationService.notificarGPS();
                    }
                    break;
                case PERMISSION_ACTIVITY_RECOGNITION_CODE:
                    Log.d("Viajes", "Permiso de reconocimiento de actividad concedido.");
                    break;
            }
        } else {
            Log.d("Viajes", "Permiso no concedido para el código: " + requestCode);
        }
    }

}
