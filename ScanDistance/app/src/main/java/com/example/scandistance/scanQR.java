package com.example.scandistance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.location.Location;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.util.SparseArray;
import android.widget.Button;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;


public class scanQR extends AppCompatActivity {
    SurfaceView surfaceView;
    TextView txtBarcodeValue;
    TextView txtCurrentLatGeo;
    TextView txtCurrentLonGeo;
    TextView txtCalculatedDis;

    Button actionButton;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        txtBarcodeValue = findViewById(R.id.txtBarcodeValue);
        surfaceView = findViewById(R.id.surfaceView);
        txtCurrentLatGeo = findViewById(R.id.currentLatVal);
        txtCurrentLonGeo = findViewById(R.id.currentLonVal);
        txtCalculatedDis = findViewById(R.id.calculatedVal);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    protected void onStart() {
        super.onStart();
        initialiseDetectorsAndSources();
    }


    @Override
    protected void onPause() {
        super.onPause();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialiseDetectorsAndSources();
        checkAndRequestLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        System.out.println("onRequestPermissionsResult: requestCode = " + requestCode);
        for (int i = 0; i < permissions.length; i++) {
            System.out.println(permissions[i] + " : " + grantResults[i]);
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            txtCurrentLatGeo.setText(Double.toString(location.getLatitude()));
                            txtCurrentLonGeo.setText(Double.toString(location.getLongitude()));
                        }
                    }
                });
    }

    private void initialiseDetectorsAndSources() {
        Toast.makeText(getApplicationContext(), "Barcode scanner started", Toast.LENGTH_SHORT).show();

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();
        actionButton = findViewById(R.id.backButton);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            scanQR.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(scanQR.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Toast.makeText(getApplicationContext(),
                        "To prevent memory leaks barcode scanner has been stopped",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(@NonNull Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    final String scanResult = barcodes.valueAt(0).displayValue;
                    txtBarcodeValue.post(() -> {
                        txtBarcodeValue.setText(scanResult);
                        if (isGeolocationFormat(scanResult)) {
                            actionButton.setText(R.string.use_this_location);
                        } else {
                            actionButton.setText(R.string.not_a_valid_code);
                        }
                    });
                }
            }
        });
    }

    private boolean isGeolocationFormat(String content) {
        // Assuming geolocation format is "geo:latitude,longitude"
        return content != null && content.startsWith("geo:") && content.split(",").length == 2;
    }

    private boolean checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, 42);
            return false; // Permissions not granted yet
        }
        return true; // Permissions are already granted
    }

    private void checkAndRequestLocationUpdates() {
        if (checkLocationPermissions()) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        // Start listening for location updates here
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            txtCurrentLatGeo.setText(Double.toString(location.getLatitude()));
                            txtCurrentLonGeo.setText(Double.toString(location.getLongitude()));
                        }
                    }
                });
    }

    public void onCalculateDistanceClick(View view) {
        // This method is called when the "Calculate Distance" button is clicked.
        calculateDistance();
    }

    public void calculateDistance() {
        String qrCodeContents = txtBarcodeValue.getText().toString();

        if (qrCodeContents.startsWith("geo:")) {
            String geoData = qrCodeContents.substring(4);
            String[] parts = geoData.split(",");
            if (parts.length >= 2) {
                try {
                    double latitude = Double.parseDouble(parts[0]);
                    double longitude = Double.parseDouble(parts[1]);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        if (location != null) {
                                            double currentLat = location.getLatitude();
                                            double currentLon = location.getLongitude();

                                            float[] distance = new float[1];
                                            Location.distanceBetween(
                                                    currentLat,
                                                    currentLon,
                                                    latitude,
                                                    longitude,
                                                    distance
                                            );
                                            double distanceInKm = distance[0] / 1000;

                                            // Update the appropriate TextView with the calculated distance
                                            txtCalculatedDis.setText(String.format("%.2f km", distanceInKm));
                                        } else {
                                            Log.d("TAG", "Location is null");
                                        }
                                    }
                                });
                    }
                } catch (NumberFormatException e) {
                    txtCalculatedDis.setText("Bad QR code!");
                    System.err.println("Invalid latitude or longitude input: " + e.getMessage());
                }
            }
        } else {
            txtCalculatedDis.setText("Invalid Value!");
        }
    }
}