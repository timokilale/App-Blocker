package com.timo.javaside;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class PermissionRequestActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Request the necessary permissions
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.QUERY_ALL_PACKAGES},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, inform the Service
                sendPermissionResult(true);
            } else {
                // Permission denied, inform the Service
                sendPermissionResult(false);
            }
        }
        // Finish the activity
        finish();
    }

    private void sendPermissionResult(boolean granted) {
        // Send the permission result back to the Service
        Intent intent = new Intent("PermissionResult");
        intent.putExtra("permissionResult", granted);
        sendBroadcast(intent);
    }
}
