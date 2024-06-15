package com.timo.javaside;

import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.annotation.NonNull;


public class UploadWorker extends Worker {
    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Call your method to upload app data
        MyAccessibilityService service = new MyAccessibilityService();
        service.uploadAppData();

        // Indicate whether the task finished successfully with the Result
        return Result.success();
    }
}

