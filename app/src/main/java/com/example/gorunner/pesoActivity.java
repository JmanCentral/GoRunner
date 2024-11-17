package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class pesoActivity extends AppCompatActivity {

    EditText peso;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        peso = findViewById(R.id.txt_peso);
    }

    public void Guardarpeso(View view) {
        // Obtener el valor del peso del EditText y convertirlo a Float
        Float pesoValue = Float.parseFloat(peso.getText().toString());

        // Crear un Intent para la Activity de destino
        Intent intent = new Intent(pesoActivity.this, Viajes.class);
        Intent intent2 = new Intent(pesoActivity.this, Localizacion.class);

        intent.putExtra("peso", pesoValue);
        intent2.putExtra("peso", pesoValue);

    }

}
