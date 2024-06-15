package com.timo.reaper;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class BlockedAppsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BlockedAppsAdapter adapter;
    private List<String> blockedApps;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_apps);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        blockedApps = new ArrayList<>();
        adapter = new BlockedAppsAdapter(blockedApps);
        recyclerView.setAdapter(adapter);

        loadBlockedApps();
    }

    private void loadBlockedApps() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("Apps")
                .whereEqualTo("blocked", true)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }

                        blockedApps.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            blockedApps.add(document.getId());
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
