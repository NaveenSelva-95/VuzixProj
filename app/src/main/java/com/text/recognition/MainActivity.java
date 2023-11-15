package com.text.recognition;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView textureView;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextRecognizer textRecognizer;
    private TextRecognizerOptions options;
    private GraphicOverlay overlayView;

    private String resultString = "NATIONAL INDUSTRIES LIMITED";
    String[] filteredWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        filteredWords = resultString.split("[-\n\\s]+");
        textureView = findViewById(R.id.live_textureView);
        overlayView = findViewById(R.id.overlayContainer);
        options = new TextRecognizerOptions.Builder().build();
        // Initialize the TextRecognizer
        textRecognizer = TextRecognition.getClient(options);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 100);
        } else {
            startCamera();
        }
    }

    private void startCamera() {

//
//        CameraSourceCore cameraSourceCore = new CameraSourceCore(this, textureView, overlayView,textRecognizer);
//        cameraSourceCore.startCamera(this);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);

                preview.setSurfaceProvider(textureView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(executor, this::processImage);

                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void processImage(@NonNull ImageProxy imageProxy) {
        // Use the ImageProxy directly for text recognition
        InputImage inputImage = InputImage.fromMediaImage(Objects.requireNonNull(imageProxy.getImage()),
                imageProxy.getImageInfo().getRotationDegrees());

//        resultString = "it was the best";
//        filteredWords = resultString.split("[-\n\\s]+");

        // Use ML Kit's Text Recognition API
        textRecognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    overlayView.clear();
                    overlayView.setText(text, resultString, filteredWords);
                })
                .addOnFailureListener(e -> {
                    // Handle text recognition failure
                })
                .addOnCompleteListener(result -> {
                    // Close the ImageProxy when processing is done
                    imageProxy.close();
                });
        overlayView.setCameraInfo(imageProxy.getWidth(), imageProxy.getHeight());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
        }
    }

}