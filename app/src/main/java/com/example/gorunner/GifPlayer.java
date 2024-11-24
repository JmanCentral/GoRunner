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

/**
 * GifPlayer es una vista personalizada que permite reproducir animaciones GIF en Android.
 * Utiliza la clase Movie para decodificar y reproducir archivos GIF.
 */
public class GifPlayer extends View {
    private Movie animacion;
    private int ancho, alto;
    private Context contexto;
    private boolean pausado;
    private int tiempo;
    private InputStream flujo;
    private long inicio;

    /**
     * Constructor que permite inicializar la vista desde un archivo XML, con atributos y un estilo predeterminado.
     * Si se define un GIF como fondo en el XML, se establece automáticamente.
     *
     * @param contexto   Contexto de la aplicación o actividad.
     * @param atributos  Atributos definidos en el archivo XML.
     * @param defStyleAttr Estilo predeterminado a aplicar.
     */

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

    /**
     * Constructor básico que inicializa la vista con el contexto.
     *
     * @param contexto Contexto de la aplicación o actividad.
     */

    public GifPlayer(Context contexto, AttributeSet atributos) {
        this(contexto, atributos, 0);
    }

    /**
     * Pausa la reproducción de la animación GIF.
     */
    public void pausar() {
        pausado = true;
    }


    /**
     * Reanuda la reproducción de la animación GIF.
     */
    public void reproducir() {
        pausado = false;
    }

    /**
     * Inicializa la animación decodificando el flujo de entrada y ajustando el tamaño de la vista.
     * Este método se invoca internamente después de configurar un archivo GIF.
     */
    private void iniciar() {
        setFocusable(true);
        animacion = Movie.decodeStream(flujo);
        ancho = animacion.width();
        alto = animacion.height();
        requestLayout();
    }

    /**
     * Establece las dimensiones de la vista en función del tamaño del GIF.
     *
     * @param widthMeasureSpec  Especificación del ancho.
     * @param heightMeasureSpec Especificación de la altura.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(ancho, alto);
    }

    /**
     * Dibuja cada cuadro de la animación en el lienzo.
     * Calcula el tiempo transcurrido desde el inicio y actualiza la posición del GIF.
     *
     * @param lienzo Canvas donde se dibuja la animación.
     */
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

    /**
     * Carga un archivo GIF desde los recursos y lo establece como animación.
     *
     * @param id ID del recurso GIF (por ejemplo, R.raw.mi_gif).
     */
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

