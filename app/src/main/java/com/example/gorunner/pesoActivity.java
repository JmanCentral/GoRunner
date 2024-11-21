package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class pesoActivity extends AppCompatActivity {

    EditText peso;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peso);

        peso = findViewById(R.id.txt_peso);

        SharedPreferences sharedPreferences = getSharedPreferences("PreferenciasUsuario", MODE_PRIVATE);
        float pesoGuardado = sharedPreferences.getFloat("peso", -1); // Valor por defecto -1 si no hay dato guardado

        // Si hay un peso guardado, mostrarlo en el EditText
        if (pesoGuardado != -1) {
            peso.setText(String.valueOf(pesoGuardado)); // Convertir float a String y mostrarlo
        }
    }

    public boolean Guardarpeso(View view) {
        // Obtener el valor del peso del EditText y convertirlo a Float
        String pesoStr = peso.getText().toString();

        if (pesoStr.isEmpty()) {
            // Si el campo está vacío, mostrar un mensaje de error
            Toast.makeText(this, "Por favor ingresa un peso", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            Float pesoValue = Float.parseFloat(pesoStr);

            boolean esPesoValido = validarPeso(pesoValue);

            if (pesoValue > 0 && esPesoValido) {
                // Guardar el peso en SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("PreferenciasUsuario", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("peso", pesoValue);
                editor.apply(); // Guardar los cambios

                // Redirigir a la actividad principal
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;  // Peso guardado correctamente
            } else {
                // Si el peso no es válido o es menor que 0
                Toast.makeText(this, "El peso debe estar entre 0.5 kg y 500 kg", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            // Si la conversión a Float falla, mostrar un mensaje de error
            Toast.makeText(this, "Por favor ingresa un valor numérico válido para el peso", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean validarPeso(Float pesoValue) {
        final float MIN_PESO = 0.5F;  // Peso mínimo permitido (en kg)
        final float MAX_PESO = 500.0F;  // Peso máximo permitido (en kg)

        return pesoValue >= MIN_PESO && pesoValue <= MAX_PESO;
    }

}
