package com.example.gorunner;

import android.app.DatePickerDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class VerViajes extends ListActivity {

    private CalendarView fecha;
    private DatePickerDialog.OnDateSetListener escuchaFecha;

    private ListView listaViajes;
    private AdaptadorViaje adaptador;
    private ArrayList<ItemViaje> nombresViajes;

    /* Clase para almacenar toda la información necesaria para mostrar un elemento de fila de viaje */
    private class ItemViaje {
        private String nombre;
        private String uriStr;
        private long id;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public void setUriStr(String uriStr) {
            this.uriStr = uriStr;
        }

        public String getUriStr() {
            return uriStr;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    /* ListView debe mostrar el nombre del viaje junto con una imagen personalizada cargada por el usuario */
    private class AdaptadorViaje extends ArrayAdapter<ItemViaje> {
        private ArrayList<ItemViaje> items;

        public AdaptadorViaje(Context contexto, int recursoTextoVista, ArrayList<ItemViaje> items) {
            super(contexto, recursoTextoVista, items);
            this.items = items;
        }

        @Override
        public View getView(int posicion, View vistaConvertida, ViewGroup padre) {
            View vista = vistaConvertida;
            if (vista == null) {
                LayoutInflater inflador = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                vista = inflador.inflate(R.layout.journeylist, null);
            }

            ItemViaje item = items.get(posicion);
            if (item != null) {
                TextView texto = vista.findViewById(R.id.singleJourney);
                ImageView img = vista.findViewById(R.id.journeyList_journeyImg);
                if (texto != null) {
                    texto.setText(item.getNombre());
                }
                if (img != null) {
                    String uriStr = item.getUriStr();
                    if (uriStr != null) {
                        try {
                            final Uri uriImagen = Uri.parse(uriStr);
                            final InputStream flujoImagen = getContentResolver().openInputStream(uriImagen);
                            final Bitmap imagenSeleccionada = BitmapFactory.decodeStream(flujoImagen);
                            img.setImageBitmap(imagenSeleccionada);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        img.setImageDrawable(getResources().getDrawable(R.drawable.icono));
                    }
                }
            }
            return vista;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_viajes);

        nombresViajes = new ArrayList<ItemViaje>();
        adaptador = new AdaptadorViaje(this, R.layout.journeylist, nombresViajes);
        setListAdapter(adaptador);

        fecha = findViewById(R.id.calendarView);
        listaViajes = getListView();

        fecha.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView vista, int anio, int mes, int diaDelMes) {
                mes += 1; // los meses empiezan en 0
                String fechaSeleccionada = String.format("%02d/%02d/%04d", diaDelMes, mes, anio);
                listarViajes(fechaSeleccionada);
            }
        });

        listaViajes.setClickable(true);
        listaViajes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int posicion, long arg3) {
                ItemViaje viaje = (ItemViaje) listaViajes.getItemAtPosition(posicion);
                long idViaje = viaje.getId();

                // Iniciar la actividad de un viaje específico enviando el idViaje
                Bundle b = new Bundle();
                b.putLong("idViaje", idViaje);
                Intent verViaje = new Intent(VerViajes.this, VerViajeEspecifico.class);
                verViaje.putExtras(b);
                startActivity(verViaje);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String fechaSeleccionada = obtenerFechaSeleccionadaDelCalendario();
        if (fechaSeleccionada != null) {
            listarViajes(fechaSeleccionada);
        }
    }

    private String obtenerFechaSeleccionadaDelCalendario() {
        // Puedes utilizar esta función para obtener la fecha actual en formato dd/MM/yyyy si es necesario.
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(fecha.getDate());
        int dia = calendario.get(Calendar.DAY_OF_MONTH);
        int mes = calendario.get(Calendar.MONTH) + 1;
        int anio = calendario.get(Calendar.YEAR);
        return String.format("%02d/%02d/%04d", dia, mes, anio);
    }

    /* Consultar la base de datos para obtener todos los viajes en la fecha especificada en formato dd/MM/yyyy y mostrarlos en ListView */
    private void listarViajes(String fecha) {
        // El servidor SQLite espera el formato yyyy-MM-dd
        String[] partesFecha = fecha.split("/");
        fecha = partesFecha[2] + "-" + partesFecha[1] + "-" + partesFecha[0];

        Cursor c = getContentResolver().query(JornadasObtenidas.JOURNEY_URI,
                new String[] {JornadasObtenidas.J_ID + " _id", JornadasObtenidas.J_NAME, JornadasObtenidas.J_IMAGE},
                JornadasObtenidas.J_DATE + " = ?", new String[] {fecha}, JornadasObtenidas.J_NAME + " ASC");

        Log.d("mdp", "Viajes cargados: " + c.getCount());

        // Poner los elementos del cursor en ArrayList y agregarlos al adaptador
        nombresViajes = new ArrayList<ItemViaje>();
        adaptador.notifyDataSetChanged();
        adaptador.clear();
        adaptador.notifyDataSetChanged();
        try {
            while(c.moveToNext()) {
                ItemViaje item = new ItemViaje();
                item.setNombre(c.getString(c.getColumnIndex(JornadasObtenidas.J_NAME)));
                item.setUriStr(c.getString(c.getColumnIndex(JornadasObtenidas.J_IMAGE)));
                item.setId(c.getLong(c.getColumnIndex("_id")));
                nombresViajes.add(item);
            }
        } finally {
            if(nombresViajes != null && nombresViajes.size() > 0) {
                adaptador.notifyDataSetChanged();
                for(int i = 0; i < nombresViajes.size(); i++) {
                    adaptador.add(nombresViajes.get(i));
                }
            }
            c.close();
            adaptador.notifyDataSetChanged();
        }
    }

}

