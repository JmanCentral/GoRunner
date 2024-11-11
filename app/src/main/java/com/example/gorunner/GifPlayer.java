package com.example.gorunner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class GifPlayer extends View {
    private Movie animacion;
    private int ancho, alto;
    private Context contexto;
    private boolean pausado;
    private int tiempo;
    private InputStream flujo;
    private long inicio;

    public GifPlayer(Context contexto, AttributeSet atributos, int defStyleAttr) {
        super(contexto, atributos, defStyleAttr);
        this.contexto = contexto;
        if (atributos.getAttributeName(1).equals("background")) {
            int id = Integer.parseInt(atributos.getAttributeValue(1).substring(1));
            setGifImageResource(id);
        }
        pausado = false;
        tiempo = 0;
    }

    public GifPlayer(Context contexto) {
        super(contexto);
        this.contexto = contexto;
    }

    public GifPlayer(Context contexto, AttributeSet atributos) {
        this(contexto, atributos, 0);
    }

    public void pausar() {
        pausado = true;
    }

    public void reproducir() {
        pausado = false;
    }

    private void iniciar() {
        setFocusable(true);
        animacion = Movie.decodeStream(flujo);
        ancho = animacion.width();
        alto = animacion.height();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(ancho, alto);
    }

    @Override
    protected void onDraw(Canvas lienzo) {
        // reproducir la animación
        long ahora = SystemClock.uptimeMillis();

        if (inicio == 0) {
            inicio = ahora;
        }

        if (animacion != null) {
            // si está pausado, no actualizar el tiempo
            if (!pausado) {
                int duracion = animacion.duration();
                if (duracion == 0) {
                    duracion = 1000;
                }

                tiempo = (int) ((ahora - inicio) % duracion);
            }

            animacion.setTime(tiempo);
            animacion.draw(lienzo, 0, 0);
            invalidate();
        }
    }

    public void setGifImageResource(int id) {
        flujo = contexto.getResources().openRawResource(id);
        iniciar();
    }

    public void setGifImageUri(Uri uri) {
        try {
            flujo = contexto.getContentResolver().openInputStream(uri);
            iniciar();
        } catch (FileNotFoundException e) {
            Log.e("mdp", "No se pudo encontrar el archivo gif");
        }
    }
}

