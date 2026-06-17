package com.khridoapp.mithai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BackupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("MithaiData", Context.MODE_PRIVATE);
        String ordersJson  = prefs.getString("orders_json", "[]");
        String productsJson = prefs.getString("products_json", "[]");

        // ── Data khali ho to backup mat karo ──
        try {
            if (new JSONArray(ordersJson).length() == 0) return;
        } catch (Exception e) {
            return;
        }

        // ── Device ID ──
        String deviceId;
        try {
            deviceId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) deviceId = "MITHAIPOS404";
        } catch (Exception e) {
            deviceId = "MITHAIPOS404";
        }

        // ── Aaj ki date key ──
        String dateKey    = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String backupTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(new Date());

        // ── Firebase pe save karo ──
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("mithai_backup")
                .child(deviceId)
                .child(dateKey);

        ref.child("orders").setValue(ordersJson);
        ref.child("products").setValue(productsJson);
        ref.child("backupTime").setValue(backupTime);

        // ── 10 din se purane backups delete karo ──
        pruneOldBackups(deviceId);
    }

    private void pruneOldBackups(String deviceId) {
        DatabaseReference deviceRef = FirebaseDatabase.getInstance()
                .getReference("mithai_backup")
                .child(deviceId);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        // Day 11 se Day 60 tak delete karne ki koshish (jo exist karte honge wahi hatenge)
        for (int i = 11; i <= 60; i++) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            deviceRef.child(sdf.format(cal.getTime())).removeValue();
        }
    }
}
