package com.example.gpsubicacionapp;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnAction;
    private final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestSmsPermissions();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnAction = findViewById(R.id.btn_action);
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap != null) {
                    LatLng quito = new LatLng(-0.1807, -78.4678);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(quito, 12));
                    mMap.addMarker(new MarkerOptions().position(quito).title("Quito"));
                }
            }
        });
    }

    private void requestSmsPermissions() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
        };

        boolean permissionNeeded = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionNeeded = true;
                break;
            }
        }

        if (permissionNeeded) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Posición inicial en Guayaquil
        LatLng guayaquil = new LatLng(-2.1894, -79.8891);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(guayaquil, 12));
        mMap.addMarker(new MarkerOptions().position(guayaquil).title("Guayaquil"));

        // Referencia a Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("gps303f");

        ref.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Double lat = snap.child("lat").getValue(Double.class);
                    Double lon = snap.child("lon").getValue(Double.class);

                    if (lat != null && lon != null) {
                        LatLng ubicacion = new LatLng(lat, lon);

                        mMap.clear();

                        String titulo = "GPS 303F";
                        String snippet = String.format("Lat: %.6f, Lon: %.6f", lat, lon);

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(ubicacion)
                                .title(titulo)
                                .snippet(snippet));

                        if (marker != null) {
                            marker.showInfoWindow();
                        }

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Manejo de errores aquí (opcional)
            }
        });
    }
}
