package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
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

        // Guardar el peso en SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("PreferenciasUsuario", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("peso", pesoValue);
        editor.apply(); // Guardar los cambios

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}
