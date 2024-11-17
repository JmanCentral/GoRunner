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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Viajes extends AppCompatActivity {
    private GifPlayer gif;
    private Localizacion.EnlaceServicioLocalizacion locationService;

    private TextView distanciaTexto;
    private TextView velocidadPromedioTexto;
    private TextView duracionTexto;
    private TextView caloriasPromedio;

    private Button botonIniciar;
    private Button botonDetener;
    private static final int PERMISSION_GPS_CODE = 1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viajes);

        if (!isLocationEnabled()) {
            showLocationDisabledAlert();
        }


        gif = findViewById(R.id.gif);
        gif.setGifImageResource(R.drawable.atleta31);
        gif.pausar();

        distanciaTexto = findViewById(R.id.distanceText);
        duracionTexto = findViewById(R.id.durationText);
        velocidadPromedioTexto = findViewById(R.id.avgSpeedText);
        caloriasPromedio = findViewById(R.id.avgSpeedText2);

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
            initButtons();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (locationService != null) {
                        // obtener la distancia y duración desde el servicio
                        float d = (float) locationService.obtenerDuracion();
                        long duracion = (long) d;  // en segundos
                        float distancia = locationService.obtenerDistancia();

                        long horas = duracion / 3600;
                        long minutos = (duracion % 3600) / 60;
                        long segundos = duracion % 60;

                        float velocidadPromedio = 0;
                        if(d != 0) {
                            velocidadPromedio = distancia / (d / 3600);
                        }

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
    private void initButtons() {
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




    public void onClickPlay(View view) {
        gif.reproducir();
        // iniciar el temporizador y el rastreo de ubicaciones GPS
        locationService.iniciarRecorrido();
        botonIniciar.setEnabled(false);
        botonDetener.setEnabled(true);
    }

    public void onClickStop(View view) {
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
        intent.putExtra("idViaje", idViaje);  // Pasar el ID del viaje al Activity de edición
        startActivity(intent);

    }

    private long obtenerIdUltimoViaje() {
        long idUltimoViaje = -1;

        // Modificamos la consulta para usar 'recorridoID' en lugar de '_id'
        Cursor cursor = getContentResolver().query(
                RecorridosObtenidos.uriRecorrido,
                new String[]{"recorridoID"}, // Usamos 'recorridoID' en lugar de '_id'
                null,
                null,
                "recorridoID DESC LIMIT 1" // Ordenamos por 'recorridoID' en orden descendente
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

// MANEJO DE PERMISOS

    @Override
    public void onRequestPermissionsResult(int reqCode, String[] permissions, int[] results) {
        if (reqCode == PERMISSION_GPS_CODE) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                initButtons();
                if (locationService != null) {
                    locationService.notificarGPS();
                }
            }
        }
    }


    public static class NoPermissionDialogue extends DialogFragment {
        public static NoPermissionDialogue newInstance() {
            NoPermissionDialogue frag = new NoPermissionDialogue();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("¡Se requiere GPS para rastrear tu viaje!")
                    .setPositiveButton("Habilitar GPS", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // el usuario aceptó habilitar el GPS
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GPS_CODE);
                        }
                    })
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationDisabledAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("La ubicación está deshabilitada. ¿Deseas habilitarla?")
                .setCancelable(false)
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }



    private void handlePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                DialogFragment modal = NoPermissionDialogue.newInstance();
                modal.show(getSupportFragmentManager(), "Permissions");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GPS_CODE);
            }
        }
    }
}
