package com.timo.javaside;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyActivity extends AppCompatActivity {
    private static final String TAG = "MyActivity";
    private static final int WRITE_REQUEST_CODE = 1;
    private static final int USAGE_STATS_PERMISSION_CODE = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for usage stats permission
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            startMyService();
        } else {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            try {
                startActivityForResult(intent, USAGE_STATS_PERMISSION_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Unable to open settings. Please grant permission manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
                if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // Get the URI of the chosen save location
                Uri uri = data.getData();

                // Create a SharedPreferences object called "pdflocation"
                SharedPreferences sharedPreferences = getSharedPreferences("pdflocation", MODE_PRIVATE);

                // Use an editor to edit the SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Store the URI in the SharedPreferences under the key "uri"
                editor.putString("uri", uri.toString());

                // Apply the changes
                editor.apply();

               z createPdf(this, uri); //createPdf(this);
            }
        }
    }


    private void startMyService() {
        Intent accessibilityServiceIntent = new Intent(this, MyAccessibilityService.class);
        startService(accessibilityServiceIntent);

        Intent blockedAppServiceIntent = new Intent(this, BlockedAppService.class);
        startService(blockedAppServiceIntent);

        // Always request user to create a new document (PDF)
        Intent createDocumentIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT); //Context.openFileOutput(String name, int mode)
        createDocumentIntent.addCategory(Intent.CATEGORY_OPENABLE);
        createDocumentIntent.setType("application/pdf");
        createDocumentIntent.putExtra(Intent.EXTRA_TITLE, "Blacklist.pdf");
        startActivityForResult(createDocumentIntent, WRITE_REQUEST_CODE);
    }


    public static void createPdf(Context context, Uri uri) {
        try {
            ContentResolver contentResolver = context.getContentResolver(); //
            OutputStream outputStream = contentResolver.openOutputStream(uri); //context.openFileOutput("Blacklist.pdf", Context.MODE_PRIVATE);

            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document pdfLayout = new Document(pdfDocument);

            // Retrieve package names from AppPrefs SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
            Map<String, ?> appsMap = prefs.getAll();
            List<String> packageNames = new ArrayList<>();

            // Filter package names where the blocked value is true
            for (Map.Entry<String, ?> entry : appsMap.entrySet()) {
                if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                    packageNames.add(entry.getKey());
                }
            }

            // Add each package name to the PDF
            if (packageNames.isEmpty()) {
                Log.w(TAG, "No blocked apps found.");
            } else {
                for (String packageName : packageNames) {
                    pdfLayout.add(new Paragraph("Package: " + packageName));
                }
            }

            pdfLayout.close();
            writer.close();
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error writing to PDF: " + e.getMessage(), e);
            Toast.makeText(context, "Error writing to PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
