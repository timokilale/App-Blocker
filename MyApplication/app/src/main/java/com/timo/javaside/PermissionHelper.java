package com.timo.javaside;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

public class PermissionHelper {

    public static void requestUsageStatsPermission(Context context) {
        // Display a toast message explaining the need for the permission
        Toast.makeText(context, "To track app usage, please grant usage access permission.", Toast.LENGTH_LONG).show();

        // Open the settings screen for the app with usage access settings
        openUsageAccessSettings(context);
    }

    private static void openUsageAccessSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Handle if the settings screen is not found
            Toast.makeText(context, "Unable to open settings. Please grant permission manually.", Toast.LENGTH_LONG).show();
        }
    }
}

