package com.nevaloss.sign_lang_app;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    public TextView prediction;
    private Uri photoUri;
    private Interpreter tflite;

    private final int imageSize = 96;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private SpeechRecognizer speechRecognizer;
    private TextView resultText;
    private Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);
        prediction = findViewById(R.id.prediction);
        EditText inputAlphabet = findViewById(R.id.inputAlphabet);
        Button btnShowSign = findViewById(R.id.btnShowSign);
        ImageView signImage = findViewById(R.id.signImage);
        LinearLayout signContainer = findViewById(R.id.signContainer);
        resultText = findViewById(R.id.resultText);
        startBtn = findViewById(R.id.startBtn);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        TextView headerImage = findViewById(R.id.headerImageToText);
        TextView headerText = findViewById(R.id.headerTextToImage);
        TextView headerAudio = findViewById(R.id.headerAudioToText);

        LinearLayout sectionImage = findViewById(R.id.sectionImageToText);
        LinearLayout sectionText = findViewById(R.id.sectionTextToImage);
        LinearLayout sectionAudio = findViewById(R.id.sectionAudioToText);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnProfile = findViewById(R.id.btnProfile);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            // Optional: Show toast
            Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Navigate back to login screen
            Intent intent = new Intent(MainActivity.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear backstack
            startActivity(intent);
        });


        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Profile.class);
                startActivity(intent);
            }
        });

        headerImage.setOnClickListener(v -> {
            sectionImage.setVisibility(View.VISIBLE);
            sectionText.setVisibility(View.GONE);
            sectionAudio.setVisibility(View.GONE);
        });

        headerText.setOnClickListener(v -> {
            sectionText.setVisibility(View.VISIBLE);
            sectionImage.setVisibility(View.GONE);
            sectionAudio.setVisibility(View.GONE);
        });

        headerAudio.setOnClickListener(v -> {
            sectionAudio.setVisibility(View.VISIBLE);
            sectionImage.setVisibility(View.GONE);
            sectionText.setVisibility(View.GONE);
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            tflite = new Interpreter(loadModelFile("asl_model.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> openGallery());


        startBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission not granted, ask for it
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            } else {
                // Permission already granted, start listening
                speechRecognizer.startListening(intent);
            }
        });
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        imageView.setImageURI(photoUri);
                        try {
//                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
//                            predictAlphabet(bitmap);
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                            bitmap = handleRotation(photoUri, bitmap); // Fix rotation like in gallery
                            predictAlphabet(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        imageView.setImageURI(selectedImage);
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                            predictAlphabet(bitmap);


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        btnShowSign.setOnClickListener(view -> {
            String text = inputAlphabet.getText().toString().toLowerCase();
            signContainer.removeAllViews(); // Clear previous images

            for (char c : String.valueOf(text).toLowerCase().toCharArray()) {
                if (Character.isLetter(c)) {
                    ImageView imageView = new ImageView(this);
                    int resId = getResources().getIdentifier(String.valueOf(c), "drawable", getPackageName());
                    if (resId != 0) {
                        imageView.setImageResource(resId);
                        imageView.setLayoutParams(new LinearLayout.LayoutParams(150, 150));
                        imageView.setPadding(8, 0, 8, 0);
                        signContainer.addView(imageView);
                    }
                }
            }
        });

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    resultText.setText(matches.get(0));
                    //resultText.setText(result); // jo already likha hai
                    displaySignImagesFromSpeech(matches.get(0)); // naya add karna
                }
            }

            // Optional: You can implement other override methods if needed
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

    }
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }

    private void displaySignImagesFromSpeech(String spokenText) {
        LinearLayout signContainer_audio = findViewById(R.id.signContainer_audio);
        signContainer_audio.removeAllViews(); // Purane views hatao

        spokenText = spokenText.toLowerCase(); // Safe side ke liye
        for (int i = 0; i < spokenText.length(); i++) {
            char character = spokenText.charAt(i);

            // Sirf alphabets ko hi dikhana
            if (Character.isLetter(character)) {
                ImageView signImage = new ImageView(this);
                int imageResId = getResources().getIdentifier(
                        String.valueOf(character),
                        "drawable",
                        getPackageName()
                );

                if (imageResId != 0) {
                    signImage.setImageResource(imageResId);
                    signImage.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                    signImage.setPadding(8, 8, 8, 8);
                    signContainer_audio.addView(signImage);
                }
            }
        }
    }


    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(intent);
        }
    }
    private Bitmap centerCrop(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        int xOffset = (src.getWidth() - size) / 2;
        int yOffset = (src.getHeight() - size) / 2;
        return Bitmap.createBitmap(src, xOffset, yOffset, size, size);
    }

    private File createImageFile() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile("IMG_", ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private MappedByteBuffer loadModelFile(String modelFile) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelFile);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private void predictAlphabet(Bitmap bitmap) {
        bitmap = centerCrop(bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedBitmap);

        float[][] output = new float[1][26]; // 26 alphabets
        tflite.run(byteBuffer, output);

        int maxIndex = 0;
        float maxConfidence = 0;
        StringBuilder allScores = new StringBuilder("Scores:\n");
        for (int i = 0; i < 26; i++) {
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i];
                maxIndex = i;
            }

            Log.d("TFLITE_SCORES", allScores.toString());
        }
        if (maxConfidence >= 0.7) {
            char predictedChar = (char) ('A' + maxIndex);
            prediction.setText(String.valueOf(predictedChar));
            Toast.makeText(this, "Predicted: " + predictedChar, Toast.LENGTH_LONG).show();


        } else {
            prediction.setText(String.valueOf("Not defined"));
            Toast.makeText(this, "Prediction: Not Defined", Toast.LENGTH_LONG).show();
        }

    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1 * imageSize * imageSize * 3 * 4);
        buffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[imageSize * imageSize];
        bitmap.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize);

        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            buffer.putFloat(r / 255.0f);
            buffer.putFloat(g / 255.0f);
            buffer.putFloat(b / 255.0f);
        }
        return buffer;
    }
    private Bitmap handleRotation(Uri imageUri, Bitmap bitmap) throws IOException {
        InputStream input = getContentResolver().openInputStream(imageUri);
        ExifInterface ei = new ExifInterface(input);

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}