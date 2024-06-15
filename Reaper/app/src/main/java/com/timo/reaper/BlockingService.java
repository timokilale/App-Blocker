package com.timo.reaper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

public class BlockingService extends AccessibilityService {

    private static final String CHANNEL_ID = "BlockingServiceChannel";
    private Set<String> blockedPackages = new HashSet<>();
    private FirebaseFirestore firestore;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = String.valueOf(event.getPackageName());
            if (blockedPackages.contains(packageName)) {
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // Handle interrupt events
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);

        startForegroundService();
        initializeFirestore();
    }

    private void initializeFirestore() {
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("Apps")
                .whereEqualTo("blocked", true)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }

                        blockedPackages.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            blockedPackages.add(document.getId());
                        }
                    }
                });
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Blocking Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Reaper Blocking Service")
                .setContentText("Blocking specified apps")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);
    }
}
