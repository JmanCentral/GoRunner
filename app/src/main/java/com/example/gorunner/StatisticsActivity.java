package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class StatisticsActivity extends AppCompatActivity {

    private BarChart barChart;
    private TextView recordDistance;
    private TextView distanceToday;
    private TextView timeToday;
    private TextView distanceAllTime;
    private TextView dateText;

    private DatePickerDialog.OnDateSetListener dateListener;


    private Handler postBack = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);


        recordDistance  = findViewById(R.id.Statistics_recordDistance);
        distanceToday   = findViewById(R.id.Statistics_distanceToday);
        timeToday       = findViewById(R.id.Statistics_timeToday);
        distanceAllTime = findViewById(R.id.Statistics_distanceAllTime);
        dateText        = findViewById(R.id.Statistics_selectDate);
        barChart        = findViewById(R.id.barchart);

        setUpDateDialogue();


    }

    private void setUpDateDialogue() {
        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int yyyy;
                int mm;
                int dd;

                // if first time selecting date choose current date, else last selected date
                if(dateText.getText().toString().toLowerCase().equals("select date")) {
                    Calendar calender = Calendar.getInstance();
                    yyyy = calender.get(Calendar.YEAR);
                    mm = calender.get(Calendar.MONTH);
                    dd = calender.get(Calendar.DAY_OF_MONTH);
                } else {
                    String[] dateParts = dateText.getText().toString().split("/");
                    yyyy = Integer.parseInt(dateParts[2]);
                    mm = Integer.parseInt(dateParts[1]) - 1;
                    dd = Integer.parseInt(dateParts[0]);
                }

                DatePickerDialog dialog = new DatePickerDialog(
                        StatisticsActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        dateListener,
                        yyyy, mm, dd);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        dateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int yyyy, int mm, int dd) {
                // user has selected a date on which to get statistics about
                mm = mm + 1;
                String date;

                // format the date so its like dd/mm/yyyy
                if(mm < 10) {
                    date = dd + "/0" + mm + "/" + yyyy;
                } else {
                    date = dd + "/" + mm + "/" + yyyy;
                }

                if(dd < 10) {
                    date = "0" + date;
                }

                dateText.setText(date);
                displayStatsOnDate(date);
            }
        };
    }

    private void displayStatsOnDate(String d) {
        // sqlite server expects yyyy-mm-dd
        final String[] dateParts = d.split("/");
        final String date = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];

        // some heavy lifting here so lets run the processing code another thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                // get the time today and distance today
                Cursor c = getContentResolver().query(JornadasObtenidas.JOURNEY_URI,
                        null, JornadasObtenidas.J_DATE + " = ?", new String[] {date}, null);
                double distaneTodayKM = 0;
                long   timeTodayS = 0;
                try {
                    while(c.moveToNext()) {
                        distaneTodayKM += c.getDouble(c.getColumnIndex(JornadasObtenidas.J_distance));
                        timeTodayS     += c.getLong(c.getColumnIndex(JornadasObtenidas.J_DURATION));
                    }
                } finally {
                    c.close();
                }

                final long hours = timeTodayS / 3600;
                final long minutes = (timeTodayS % 3600) / 60;
                final long seconds = timeTodayS % 60;
                final double distanceTodayKM_ = distaneTodayKM;


                // calculate record distance in 1 day and total distance travelled all time
                c = getContentResolver().query(JornadasObtenidas.JOURNEY_URI,
                        null, null, null, null);
                double totalDistanceKM = 0;
                double recordDistanceKM = 0;
                try {
                    while(c.moveToNext()) {
                        double d = c.getDouble(c.getColumnIndex(JornadasObtenidas.J_distance));
                        if(recordDistanceKM < d) {
                            recordDistanceKM = d;
                        }
                        totalDistanceKM += d;
                    }
                } finally {
                    c.close();
                }

                final double totalDistanceKM_  = totalDistanceKM;
                final double recordDistanceKM_ = recordDistanceKM;


                // load journeys for this week to display on the bar chart
                ArrayList<BarEntry> entries = new ArrayList<BarEntry>();
                float[] distancesOnDays = {0, 0, 0, 0, 0, 0, 0};
                try {
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    cal.setTime(sdf.parse(date));
                    cal.setFirstDayOfWeek(Calendar.MONDAY);

                    // set the calendar to monday of the current week
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

                    // print dates of the current week starting on Monday
                    String mon = sdf.format(cal.getTime());
                    for (int i = 0; i <6; i++) {
                        cal.add(Calendar.DATE, 1);
                    }
                    String sun = sdf.format(cal.getTime());

                    Log.d("mdp", "Mon = " + mon + ", Sun = " + sun);
                    c = getContentResolver().query(JornadasObtenidas.JOURNEY_URI,
                            null, JornadasObtenidas.J_DATE + " BETWEEN ? AND ?",
                            new String[] {mon, sun}, null);
                    try {
                        for(int i = 0; c.moveToNext(); i++) {
                            // put each journey into the bar chart depending on what day it is
                            String date = c.getString(c.getColumnIndex(JornadasObtenidas.J_DATE));
                            cal = Calendar.getInstance();
                            cal.setTime(sdf.parse(date));
                            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = sunday, 2 = mon ... 7 = sat
                            distancesOnDays[dayOfWeek - 1] += (float) c.getDouble(c.getColumnIndex(JornadasObtenidas.J_distance));
                        }
                    } finally {
                        c.close();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                for(int i = 1; i < distancesOnDays.length; i++) {
                    // don't add sunday first, add it last
                    entries.add(new BarEntry(distancesOnDays[i], i - 1));
                }
                entries.add(new BarEntry(distancesOnDays[0], distancesOnDays.length - 1));
                final ArrayList<BarEntry> entries_ = entries;

                // post back text view updates to UI thread
                postBack.post(new Runnable() {
                   public void run() {
                       distanceToday.setText(String.format("%.2f KM", distanceTodayKM_));
                       timeToday.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                       recordDistance.setText(String.format("%.2f KM", recordDistanceKM_));
                       distanceAllTime.setText(String.format("%.2f KM", totalDistanceKM_));
                       loadBarChart(entries_);
                   }
                });
            }
        }).start();

    }


    private void loadBarChart(ArrayList<BarEntry> entries) {
        BarDataSet bardataset = new BarDataSet(entries, "Days");

        ArrayList<String> labels = new ArrayList<String>();
        labels.add("Mon");
        labels.add("Tue");
        labels.add("Wed");
        labels.add("Thur");
        labels.add("Fri");
        labels.add("Sat");
        labels.add("Sun");

        BarData data = new BarData(labels, bardataset);
        barChart.setData(data); // set the data and list of labels into chart
        barChart.setDescription("Distance (KM)");  // set the description
        bardataset.setColors(ColorTemplate.COLORFUL_COLORS);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinValue(0f);
        barChart.animateY(3000);
    }
}
