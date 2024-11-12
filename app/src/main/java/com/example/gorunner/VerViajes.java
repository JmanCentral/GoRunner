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
    private DatePickerDialog.OnDateSetListener dateListener;

    private ListView journeyList;
    private JourneyAdapter adapter;
    private ArrayList<JourneyItem> journeyNames;

    /* Class to store all the information needed to display journey row item */
    private class JourneyItem {
        private String name;
        private String strUri;
        private long _id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setStrUri(String strUri) {
            this.strUri = strUri;
        }

        public String getStrUri() {
            return strUri;
        }

        public void set_id(long _id) {
            this._id = _id;
        }

        public long get_id() {
            return _id;
        }
    }

    /* ListView should display journey name along side a custom image uploaded by the user */
    private class JourneyAdapter extends ArrayAdapter<JourneyItem> {
        //
        private ArrayList<JourneyItem> items;

        public JourneyAdapter(Context context, int textViewResourceId, ArrayList<JourneyItem> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.journeylist, null);
            }

            JourneyItem item = items.get(position);
            if (item != null) {
                TextView text = v.findViewById(R.id.singleJourney);
                ImageView img = v.findViewById(R.id.journeyList_journeyImg);
                if (text != null) {
                    text.setText(item.getName());
                }
                if(img != null) {
                    String strUri = item.getStrUri();
                    if(strUri != null) {
                        try {
                            final Uri imageUri = Uri.parse(strUri);
                            final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                            img.setImageBitmap(selectedImage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        img.setImageDrawable(getResources().getDrawable(R.drawable.icono));
                    }
                }
            }
            return v;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_viajes);

        journeyNames = new ArrayList<JourneyItem>();
        adapter = new JourneyAdapter(this, R.layout.journeylist, journeyNames);
        setListAdapter(adapter);

        fecha = findViewById(R.id.calendarView);
        journeyList = getListView();

        fecha.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                month += 1; // los meses empiezan en 0
                String date = String.format("%02d/%02d/%04d", dayOfMonth, month, year);
                listJourneys(date);
            }
        });

        journeyList.setClickable(true);
        journeyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                JourneyItem o = (JourneyItem) journeyList.getItemAtPosition(position);
                long journeyID = o.get_id();

                // start the single journey activity sending it the journeyID
                Bundle b = new Bundle();
                b.putLong("journeyID", journeyID);
                Intent singleJourney = new Intent(VerViajes.this, VerViajeEspecifico.class);
                singleJourney.putExtras(b);
                startActivity(singleJourney);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String selectedDate = getSelectedDateFromCalendar();
        if (selectedDate != null) {
            listJourneys(selectedDate);
        }
    }

    private String getSelectedDateFromCalendar() {
        // Puedes utilizar esta función para obtener la fecha actual en formato dd/MM/yyyy si es necesario.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(fecha.getDate());
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        return String.format("%02d/%02d/%04d", day, month, year);
    }



    /* Query database to get all journeys in specified date in dd/mm/yyyy format and display them in listview */
    private void listJourneys(String date) {
        // sqlite server expects yyyy-mm-dd
        String[] dateParts = date.split("/");
        date = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];


        Cursor c = getContentResolver().query(JornadasObtenidas.JOURNEY_URI,
                new String[] {JornadasObtenidas.J_ID + " _id", JornadasObtenidas.J_NAME, JornadasObtenidas.J_IMAGE}, JornadasObtenidas.J_DATE + " = ?", new String[] {date}, JornadasObtenidas.J_NAME + " ASC");

        Log.d("mdp", "Journeys Loaded: " + c.getCount());

        // put cursor items into ArrayList and add those items to the adapter
        journeyNames = new ArrayList<JourneyItem>();
        adapter.notifyDataSetChanged();
        adapter.clear();
        adapter.notifyDataSetChanged();
        try {
            while(c.moveToNext()) {
                JourneyItem i = new JourneyItem();
                i.setName(c.getString(c.getColumnIndex(JornadasObtenidas.J_NAME)));
                i.setStrUri(c.getString(c.getColumnIndex(JornadasObtenidas.J_IMAGE)));
                i.set_id(c.getLong(c.getColumnIndex("_id")));
                journeyNames.add(i);
            }
        } finally {
            if(journeyNames != null && journeyNames.size() > 0) {
                adapter.notifyDataSetChanged();
                for(int i = 0; i < journeyNames.size(); i++) {
                    adapter.add(journeyNames.get(i));
                }
            }
            c.close();
            adapter.notifyDataSetChanged();
        }
    }
}
