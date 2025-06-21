package com.example.gpsubicacionapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                String mensaje = sms.getMessageBody();

                // Extrae latitud y longitud
                Double lat = extraerValor(mensaje, "lat");
                Double lon = extraerValor(mensaje, "lon");

                if (lat != null && lon != null) {
                    subirAFirebase(lat, lon);
                }
            }
        }
    }

    private Double extraerValor(String texto, String clave) {
        try {
            Pattern pattern = Pattern.compile(clave + "[:\\s]+(-?\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(texto.toLowerCase());
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void subirAFirebase(Double lat, Double lon) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference("gps303f");
        Map<String, Object> data = new HashMap<>();
        data.put("lat", lat);
        data.put("lon", lon);
        data.put("timestamp", System.currentTimeMillis());
        ref.push().setValue(data);
    }
}
