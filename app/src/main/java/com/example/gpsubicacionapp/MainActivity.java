package com.example.gpsubicacionapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker marcadorActual;
    private boolean mostrandoRuta = false;
    private boolean enviandoAutomaticamente = false;
    private Handler handler = new Handler();
    private Runnable enviarSmsRunnable;
    private Button btnMenu; // Botón menú único
    private static final long INTERVALO_ENVIO_MS = 30000; // 30 segundos

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

        btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu == null) {
            Toast.makeText(this, "Error: botón no encontrado", Toast.LENGTH_SHORT).show();
        }
        btnMenu.setOnClickListener(this::mostrarPopupMenu);

    }

    private void mostrarPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_opciones, popup.getMenu());

        // Forzar que se muestren los iconos en el PopupMenu (solo en algunas versiones)
        try {
            Field mField = popup.getClass().getDeclaredField("mPopup");
            mField.setAccessible(true);
            Object menuPopupHelper = mField.get(popup);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceIcons.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        popup.setOnMenuItemClickListener(this::onMenuItemSelected);
        popup.show();
    }

    private boolean onMenuItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_enviar_sms) {
            String numeroGPS = "+593993270594";
            String comando = "Position123456";
            iniciarEnvioAutomaticoSMS(numeroGPS, comando);
            return true;

        } else if (id == R.id.menu_toggle_ruta) {
            if (!mostrandoRuta) {
                mostrarRutaHistorica();
                mostrandoRuta = true;
                Toast.makeText(this, "Ruta mostrada", Toast.LENGTH_SHORT).show();
            } else {
                if (mMap != null) {
                    mMap.clear();
                    marcadorActual = null;
                    Toast.makeText(this, "Mapa limpiado", Toast.LENGTH_SHORT).show();
                }
                mostrandoRuta = false;
                mostrarUltimaUbicacionEnTiempoReal();
            }
            return true;
        }

        return false;
    }


    private void requestSmsPermissions() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    1);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        LatLng guayaquil = new LatLng(-2.1894, -79.8891);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(guayaquil, 12));

        mostrarUltimaUbicacionEnTiempoReal();

        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });
    }

    private void mostrarUltimaUbicacionEnTiempoReal() {
        if (mMap == null) {
            Toast.makeText(this, "Mapa no está listo", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("gps303f");
        ref.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Double lat = snap.child("lat").getValue(Double.class);
                    Double lon = snap.child("lon").getValue(Double.class);
                    if (lat != null && lon != null) {
                        LatLng ubicacion = new LatLng(lat, lon);

                        if (marcadorActual != null) {
                            marcadorActual.remove();
                        }

                        DecimalFormat df = new DecimalFormat("#.######");
                        String latStr = df.format(lat);
                        String lonStr = df.format(lon);

                        marcadorActual = mMap.addMarker(new MarkerOptions()
                                .position(ubicacion)
                                .title("Ubicación GPS")
                                .snippet("Latitud: " + latStr + "   Longitud: " + lonStr));

                        marcadorActual.showInfoWindow();
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 17));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al cargar ubicación: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarRutaHistorica() {
        if (mMap == null) {
            Toast.makeText(this, "Mapa no está listo", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("gps303f");
        ref.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(MainActivity.this, "No hay rutas disponibles", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<LatLng> rutaPuntos = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Double lat = snap.child("lat").getValue(Double.class);
                    Double lon = snap.child("lon").getValue(Double.class);
                    if (lat != null && lon != null) {
                        rutaPuntos.add(new LatLng(lat, lon));
                    }
                }

                if (!rutaPuntos.isEmpty()) {
                    mMap.addMarker(new MarkerOptions()
                            .position(rutaPuntos.get(0))
                            .title("Inicio de Ruta"));

                    mMap.addMarker(new MarkerOptions()
                            .position(rutaPuntos.get(rutaPuntos.size() - 1))
                            .title("Fin de Ruta"));

                    mMap.addPolyline(new PolylineOptions()
                            .addAll(rutaPuntos)
                            .width(8)
                            .color(Color.BLUE));

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(rutaPuntos.get(rutaPuntos.size() - 1), 15));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al cargar ruta: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void enviarComandoSMS(String numero, String mensaje) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso para enviar SMS no concedido", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(numero, null, mensaje, null, null);
            Toast.makeText(this, "Comando enviado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al enviar SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void iniciarEnvioAutomaticoSMS(String numero, String comando) {
        if (enviandoAutomaticamente) {
            handler.removeCallbacks(enviarSmsRunnable);
            enviandoAutomaticamente = false;
            btnMenu.setText("Menú");
            Toast.makeText(this, "Envío automático detenido", Toast.LENGTH_SHORT).show();
        } else {
            enviarSmsRunnable = new Runnable() {
                @Override
                public void run() {
                    enviarComandoSMS(numero, comando);
                    handler.postDelayed(this, INTERVALO_ENVIO_MS);
                }
            };
            handler.post(enviarSmsRunnable);
            enviandoAutomaticamente = true;
            btnMenu.setText("Detener envío automático");
            Toast.makeText(this, "Envío automático iniciado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(enviarSmsRunnable);
    }
}
