package com.example.gorunner;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_GPS_CODE = 1;
    private static final int PERMISSION_COAL_GPS_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void empezarviaje(View v) {

        Intent journey = new Intent(MainActivity.this, Viajes.class);
        startActivity(journey);
    }

    public void verrecorridos(View v) {

        Intent view = new Intent(MainActivity.this, VerViajes.class);
        startActivity(view);
    }

    public void peso(View v) {
        Intent stats = new Intent(MainActivity.this, pesoActivity.class);
        startActivity(stats);
    }


}
