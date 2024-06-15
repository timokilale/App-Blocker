package com.timo.javaside;

import android.accessibilityservice.AccessibilityService;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private FirebaseFirestore db = null;
    private static final String CHANNEL_ID = "MyAccessibilityServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private static final int USAGE_STATS_PERMISSION_CODE = 1002;

    private final List<String> documentIds = new ArrayList<>();
    private static final int NUM_THREADS = 5;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // handles accessibility events
    }

    @Override
    public void onInterrupt() {
        //  handles accessibility events
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        checkUsageStatsPermission();

        // Create a notification channel and start the service in the foreground
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("My Accessibility Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.ic_notification) // icon
                .build();
        startForeground(NOTIFICATION_ID, notification);

        // Initialize Firestore and other components
        db = FirebaseFirestore.getInstance();

        // Schedule the task to upload app data every 24 hours
        PeriodicWorkRequest uploadWorkRequest =
                new PeriodicWorkRequest.Builder(UploadWorker.class, 24, TimeUnit.HOURS)
                        .build();
        WorkManager.getInstance(this).enqueue(uploadWorkRequest);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MyAccessibilityService";
            String description = "Channel for My Accessibility Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void checkUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            initialize();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Usage Stats Permission");
            builder.setMessage("This app needs access to usage stats to function properly.");
            builder.setPositiveButton("Grant Permission", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MyAccessibilityService.this, "Unable to open settings. Please grant permission manually.", Toast.LENGTH_LONG).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Handle user cancellation
                }
            });
            builder.show();
        }
    }

    private void initialize() {
        // Initialize Firestore and other components
        db = FirebaseFirestore.getInstance();

        // Call the method to upload app data if document IDs are not already loaded
        if (documentIds.isEmpty()) {
            loadDocumentIds();
            if (documentIds.isEmpty()) {
                uploadAppData();
            }
        }

        // Add a listener for each document
        for (String documentId : documentIds) {
            db.collection("Apps").document(documentId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (snapshot != null && snapshot.exists()) {
                            boolean blocked = Boolean.TRUE.equals(snapshot.getBoolean("blocked"));
                            int timeLimit = snapshot.getLong("time_limit").intValue();
                            if (blocked) {
                                String packageName = snapshot.getString("package");

                                // Save packageName and blocked to SharedPreferences
                                SharedPreferences prefs = getSharedPreferences("blocked_apps", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(packageName, true);
                                editor.apply();

                                prefs = getSharedPreferences("pdf_info", MODE_PRIVATE);
                                editor = prefs.edit();
                                editor.putBoolean(packageName, true);
                                editor.apply();

                                // Trigger PDF update
                                Intent createDocumentIntent = new Intent(MyAccessibilityService.this, MyActivity.class);
                                createDocumentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(createDocumentIntent);
                            }
                        }
                    });
        }
    }

    private void loadDocumentIds() {
        SharedPreferences preferences = getSharedPreferences("documentIds", MODE_PRIVATE);
        String documentIdsString = preferences.getString("documentIds", "");
        if (!documentIdsString.isEmpty()) {
            documentIdsString = documentIdsString.substring(1, documentIdsString.length() - 1); // Remove brackets
            String[] idsArray = documentIdsString.split(", ");
            documentIds.addAll(Arrays.asList(idsArray));
        }
    }

    private long getAppUsageTime(String packageName) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return 0;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long start = calendar.getTimeInMillis();
        long end = System.currentTimeMillis();

        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (stats == null) {
            return 0;
        }

        for (UsageStats usageStats : stats) {
            if (usageStats.getPackageName().equals(packageName)) {
                return usageStats.getTotalTimeInForeground();
            }
        }
        return 0;
    }

    private String formatUsageTime(long usageTime) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(usageTime);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(usageTime) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public void uploadAppData() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        executor.submit(() -> {
            try {
                PackageManager packageManager = getPackageManager();
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);

                for (ResolveInfo resolveInfo : apps) {
                    String appName = resolveInfo.loadLabel(packageManager).toString();
                    String packageName = resolveInfo.activityInfo.packageName;

                    // Get app usage time
                    long usageTime = getAppUsageTime(packageName);

                    // Format usage time
                    String formattedUsageTime = formatUsageTime(usageTime);

                    // Create app data map
                    Map<String, Object> appData = new HashMap<>();
                    appData.put("appName", appName);
                    appData.put("package", packageName);
                    appData.put("usageTime", formattedUsageTime);
                    appData.put("blocked", true); // Default to false
                    appData.put("time_limit", 0); //

                    // Add app data to Firestore
                    db.collection("Apps").document(packageName).set(appData)
                            .addOnSuccessListener(aVoid -> {
                                // Save package name to SharedPreferences
                                SharedPreferences preferences = getSharedPreferences("documentIds", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString(packageName, packageName);
                                editor.apply();

                                Log.d(TAG, "App data added with ID: " + packageName);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error adding app data", e));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading app data", e);
            } finally {
                // Shutdown the executor service
                executor.shutdown();
            }
        });
    }

}

