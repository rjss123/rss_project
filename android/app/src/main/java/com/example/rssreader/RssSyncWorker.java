package com.example.rssreader;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * WorkManager 后台定时同步 RSS 文章。
 * - 遍历所有已订阅 Feed，获取最新 RSS 内容
 * - 增量存储：按 link 去重，只插入新文章
 * - 不弹通知，静默执行
 */
public class RssSyncWorker extends Worker {

    private static final String TAG = "RssSyncWorker";

    public RssSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "RSS sync started");
        RssRepository repo = new RssRepository(getApplicationContext());

        try {
            SyncResult result = repo.syncFeedsSilently();

            Log.d(TAG, "RSS sync done — updated: " + result.newArticleCount
                    + " articles from " + result.feedCount + " feeds"
                    + (result.failedCount > 0 ? " (" + result.failedCount + " failed)" : ""));

            // 即使部分源失败也认为成功，下次会重试
            if (result.failedCount > 0 && result.failedCount == result.feedCount) {
                // 所有源都失败了，可能是网络问题，延迟重试
                return Result.retry();
            }
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "RSS sync failed: " + e.getMessage(), e);
            // 临时错误（网络波动等）自动重试
            return Result.retry();
        }
    }

    /** 同步结果 */
    static class SyncResult {
        int feedCount;
        int newArticleCount;
        int failedCount;

        SyncResult(int feedCount, int newArticleCount, int failedCount) {
            this.feedCount = feedCount;
            this.newArticleCount = newArticleCount;
            this.failedCount = failedCount;
        }
    }
}
