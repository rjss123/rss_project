package com.example.rssreader;

import android.app.Application;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class RssApplication extends Application {

    private static final String TAG = "RssApplication";
    private static final String WORK_NAME = "rss_periodic_sync";

    @Override
    public void onCreate() {
        super.onCreate();
        scheduleRssSync();
    }

    /**
     * 调度定时 RSS 同步任务
     * - 最小间隔 30 分钟（避免频繁请求）
     * - 仅在联网时执行
     * - 不弹通知，静默更新
     */
    private void scheduleRssSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(
                RssSyncWorker.class,
                30, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .addTag("rss_sync")
                .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP, // 已有任务则复用，不重复创建
                        syncWork
                );

        Log.d(TAG, "RSS periodic sync scheduled: every 30 minutes");
    }
}
