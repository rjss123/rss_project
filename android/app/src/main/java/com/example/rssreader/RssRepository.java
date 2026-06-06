package com.example.rssreader;

import android.content.Context;
import android.util.Log;
import com.example.rssreader.database.AppDatabase;
import com.example.rssreader.database.ArticleDao;
import com.example.rssreader.database.ArticleEntity;
import com.example.rssreader.database.FeedDao;
import com.example.rssreader.database.FeedEntity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RssRepository {

    private static final String TAG = "RssRepository";
    private ArticleDao articleDao;
    private FeedDao feedDao;
    private ExecutorService executorService;
    private OkHttpClient httpClient;

    public interface OnArticlesLoadedListener {
        void onSuccess(List<ArticleEntity> articles);
        void onError(String error);
    }

    public interface OnFeedsLoadedListener {
        void onSuccess(List<FeedEntity> feeds);
        void onError(String error);
    }

    public interface OnFeedOperationListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public RssRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        articleDao = database.articleDao();
        feedDao = database.feedDao();
        executorService = Executors.newCachedThreadPool();
        httpClient = new OkHttpClient();
    }

    public void getAllArticles(OnArticlesLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<ArticleEntity> articles = articleDao.getAllArticles();
                listener.onSuccess(articles);
            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void searchArticles(String query, OnArticlesLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<ArticleEntity> articles = articleDao.searchArticles(query);
                listener.onSuccess(articles);
            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void getFavoriteArticles(OnArticlesLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<ArticleEntity> articles = articleDao.getFavoriteArticles();
                listener.onSuccess(articles);
            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void getUnreadArticles(OnArticlesLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<ArticleEntity> articles = articleDao.getUnreadArticles();
                listener.onSuccess(articles);
            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void getAllFeeds(OnFeedsLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<FeedEntity> feeds = feedDao.getAllFeeds();
                listener.onSuccess(feeds);
            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void addFeed(String url, OnFeedOperationListener listener) {
        executorService.execute(() -> {
            try {
                FeedEntity feed = new FeedEntity();
                feed.setUrl(url);
                feed.setTitle(extractFeedTitle(url));
                feed.setCreatedAt(String.valueOf(System.currentTimeMillis()));
                feedDao.insert(feed);

                fetchAndParseFeed(url, feed.getId(), new OnArticlesLoadedListener() {
                    @Override
                    public void onSuccess(List<ArticleEntity> articles) {
                        listener.onSuccess("订阅添加成功，获取到 " + articles.size() + " 篇文章");
                    }

                    @Override
                    public void onError(String error) {
                        listener.onError("获取内容失败: " + error);
                    }
                });

            } catch (Exception e) {
                listener.onError("添加失败: " + e.getMessage());
            }
        });
    }

    public void refreshFeed(FeedEntity feed, OnFeedOperationListener listener) {
        fetchAndParseFeed(feed.getUrl(), feed.getId(), new OnArticlesLoadedListener() {
            @Override
            public void onSuccess(List<ArticleEntity> articles) {
                listener.onSuccess("刷新成功，获取到 " + articles.size() + " 篇文章");
            }

            @Override
            public void onError(String error) {
                listener.onError("刷新失败: " + error);
            }
        });
    }

    public void deleteFeed(FeedEntity feed, OnFeedOperationListener listener) {
        executorService.execute(() -> {
            try {
                feedDao.delete(feed);
                listener.onSuccess("删除成功");
            } catch (Exception e) {
                listener.onError("删除失败: " + e.getMessage());
            }
        });
    }

    private void fetchAndParseFeed(String url, int feedId, OnArticlesLoadedListener listener) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Fetching RSS from: " + url);
                Request request = new Request.Builder().url(url).build();
                Response response = httpClient.newCall(request).execute();

                if (!response.isSuccessful()) {
                    listener.onError("HTTP " + response.code());
                    return;
                }

                String xmlContent = response.body().string();
                List<ArticleEntity> articles = RssParser.parse(xmlContent, feedId);

                // 去重：只插入不存在的文章（根据链接判断）
                int newCount = 0;
                for (ArticleEntity article : articles) {
                    try {
                        ArticleEntity existing = articleDao.getArticleByLink(article.getLink());
                        if (existing == null) {
                            articleDao.insert(article);
                            newCount++;
                            Log.d(TAG, "Inserted new article: " + article.getTitle());
                        } else {
                            Log.d(TAG, "Skipped duplicate: " + article.getTitle());
                        }
                    } catch (Exception e) {
                        // 忽略重复插入错误
                        Log.e(TAG, "Insert error (probably duplicate): " + e.getMessage());
                    }
                }

                Log.d(TAG, "New articles: " + newCount + " / Total: " + articles.size());
                listener.onSuccess(articles);
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
                listener.onError(e.getMessage());
            }
        });
    }

    private String extractFeedTitle(String url) {
        try {
            String title = url;
            if (title.contains("://")) title = title.split("://")[1];
            if (title.contains("/")) title = title.split("/")[0];
            return title;
        } catch (Exception e) {
            return url;
        }
    }

    public void toggleFavorite(int id, boolean isFavorited) {
        executorService.execute(() -> articleDao.updateFavoriteStatus(id, isFavorited));
    }

    public void markAsRead(int id) {
        executorService.execute(() -> articleDao.updateReadStatus(id, true));
    }
}
