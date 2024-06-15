package com.timo.reaper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BlockedAppsAdapter extends RecyclerView.Adapter<BlockedAppsAdapter.BlockedAppViewHolder> {

    private List<String> blockedApps;

    public BlockedAppsAdapter(List<String> blockedApps) {
        this.blockedApps = blockedApps;
    }

    @NonNull
     public BlockedAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blocked_app, parent, false);
        return new BlockedAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockedAppViewHolder holder, int position) {
        holder.bind(blockedApps.get(position));
    }

    @Override
    public int getItemCount() {
        return blockedApps.size();
    }

    static class BlockedAppViewHolder extends RecyclerView.ViewHolder {

        private TextView appNameTextView;

        public BlockedAppViewHolder(@NonNull View itemView) {
            super(itemView);
            appNameTextView = itemView.findViewById(R.id.app_name_text_view);
        }

        public void bind(String appName) {
            appNameTextView.setText(appName);
        }
    }
}
