package com.timo.javaside;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class BlockedAppService extends Service {
    private static final String TAG = "BlockedAppService";
    private static final String CHANNEL_ID = "BlockedAppServiceChannel";
    private static final int NOTIFICATION_ID = 2;
    private ListenerRegistration listenerRegistration;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create notification channel and start service in foreground
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Blocked App Service")
                .setContentText("Monitoring blocked apps...")
                .setSmallIcon(R.drawable.ic_notification) // icon
                .build();
        startForeground(NOTIFICATION_ID, notification);

        // Firestore listener registration
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        listenerRegistration = db.collection("Apps")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        // Update PDF with the new data
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String appName = doc.getId();
                            Boolean blocked = doc.getBoolean("blocked");

                            if (blocked != null) {
                                editor.putBoolean(appName, blocked);
                            }
                        }

                        editor.apply();
                        updatePdf();
                    }
                });
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "BlockedAppService";
            String description = "Channel for Blocked App Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updatePdf() {
        SharedPreferences prefs = getSharedPreferences("pdflocation", MODE_PRIVATE);
        String uriString = prefs.getString("uri", null);

        if (uriString != null) {
            Uri pdfUri = Uri.parse(uriString);
            MyActivity.createPdf(this, pdfUri);
        } else {
            Log.w(TAG, "PDF URI is null.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
