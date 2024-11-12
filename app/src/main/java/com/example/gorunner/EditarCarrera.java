package com.example.gorunner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditarCarrera extends AppCompatActivity {
    private final int RESULT_LOAD_IMG = 1;
    private final int REQUEST_IMAGE_CAPTURE = 2;
    private final int REQUEST_CAMERA_PERMISSION = 100;
    private ImageView journeyImg;
    private EditText titleET;
    private EditText commentET;
    private EditText ratingET;
    private long journeyID;

    private Uri selectedJourneyImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_carrera);

        Bundle bundle = getIntent().getExtras();

        journeyImg = findViewById(R.id.journeyImg);
        titleET = findViewById(R.id.titleEditText);
        commentET = findViewById(R.id.commentEditText);
        ratingET = findViewById(R.id.ratingEditText);
        journeyID = bundle.getLong("journeyID");

        selectedJourneyImg = null;

        populateEditText();
    }

    /* Save the new title, comment, image and rating to the DB */
    public void onClickSave(View v) {
        int rating = checkRating(ratingET);
        if(rating == -1) {
            return;
        }

        Uri rowQueryUri = Uri.withAppendedPath(JornadasObtenidas.JOURNEY_URI, "" + journeyID);

        ContentValues cv = new ContentValues();
        cv.put(JornadasObtenidas.J_RATING, rating);
        cv.put(JornadasObtenidas.J_COMMENT, commentET.getText().toString());
        cv.put(JornadasObtenidas.J_NAME, titleET.getText().toString());

        if(selectedJourneyImg != null) {
            cv.put(JornadasObtenidas.J_IMAGE, selectedJourneyImg.toString());
        }

        getContentResolver().update(rowQueryUri, cv, null, null);
        finish();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // Si el permiso ya ha sido concedido, abrir la cámara
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }


    public void onClickChangeImage(View v) {
        checkCameraPermission();
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch(reqCode) {
                case RESULT_LOAD_IMG:
                    if (data != null && data.getData() != null) {
                        Uri imageUri = data.getData();
                        try {
                            getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            InputStream imageStream = getContentResolver().openInputStream(imageUri);
                            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                            journeyImg.setImageBitmap(selectedImage);
                            selectedJourneyImg = imageUri;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(this, "You didn't pick an Image", Toast.LENGTH_LONG).show();
                    }
                    break;

                case REQUEST_IMAGE_CAPTURE:
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    journeyImg.setImageBitmap(imageBitmap);

                    // Opcional: Guardar el Bitmap en almacenamiento para obtener un URI
                    selectedJourneyImg = saveImageToStorage(imageBitmap);
                    break;
            }
        }
    }

    // Método para guardar la imagen en almacenamiento y obtener el URI
    private Uri saveImageToStorage(Bitmap bitmap) {
        Uri uri = null;
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "journey_image_" + journeyID + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            uri = Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }


    /* Give the edit texts some initial text from what they were, get this by accessing DB */
    private void populateEditText() {
        Cursor c = getContentResolver().query(Uri.withAppendedPath(JornadasObtenidas.JOURNEY_URI,
                journeyID + ""), null, null, null, null);

        if(c.moveToFirst()) {
            titleET.setText(c.getString(c.getColumnIndex(JornadasObtenidas.J_NAME)));
            commentET.setText(c.getString(c.getColumnIndex(JornadasObtenidas.J_COMMENT)));
            ratingET.setText(c.getString(c.getColumnIndex(JornadasObtenidas.J_RATING)));

            // if an image has been set by user display it, else default image is displayed
            String strUri = c.getString(c.getColumnIndex(JornadasObtenidas.J_IMAGE));
            if(strUri != null) {
                try {
                    final Uri imageUri = Uri.parse(strUri);
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    journeyImg.setImageBitmap(selectedImage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* Ensure a rating is between 1-5 */
    private int checkRating(EditText newRating) {
        int rating;
        try {
            rating = Integer.parseInt(newRating.getText().toString());
        } catch(Exception e) {
            Log.d("mdp", "The following is not a number: " + newRating.getText().toString());
            return -1;
        }

        if(rating < 0 || rating > 5) {
            Log.d("mdp", "Rating must be between 0-5");
            return -1;
        }
        return rating;
    }

}
