package com.example.gorunner;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // Método para iniciar el viaje
    public void empezarviaje(View v) {

        Intent recorrido = new Intent(MainActivity.this, Viajes.class);
        startActivity(recorrido);
    }

    // Método para ver los recorridos
    public void verrecorridos(View v) {

        Intent view = new Intent(MainActivity.this, VerViajes.class);
        startActivity(view);
    }

    // Método para ir a cacular las calorias
    public void peso(View v) {
        Intent peso = new Intent(MainActivity.this, pesoActivity.class);
        startActivity(peso);
    }


}
