package com.example.rssreader;

import android.content.Context;
import android.util.Log;
import com.example.rssreader.database.AppDatabase;
import com.example.rssreader.database.ArticleDao;
import com.example.rssreader.database.ArticleEntity;
import com.example.rssreader.database.FeedDao;
import com.example.rssreader.database.FeedEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RssRepository {

    private static final String TAG = "RssRepository";
    private static final String NO_CONTENT_TEXT = "该 RSS 源未提供文章内容";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final Pattern FEED_URL_PATTERN = Pattern.compile("(?i)(https?:)?//[^\\s\\\"'<>]+(?:rss|atom|feed|xml)[^\\s\\\"'<>]*");
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

    public interface OnFeedScanListener {
        void onSuccess(List<String> feedUrls);
        void onError(String error);
    }

    public RssRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        articleDao = database.articleDao();
        feedDao = database.feedDao();
        executorService = Executors.newCachedThreadPool();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .build();
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

    public void scanFeedUrls(String url, OnFeedScanListener listener) {
        executorService.execute(() -> {
            try {
                List<String> feedUrls = scanFeedUrls(normalizeUrl(url), false);
                if (feedUrls.isEmpty()) {
                    Set<String> candidates = collectFeedUrlCandidates(normalizeUrl(url));
                    addKnownFeedCandidates(normalizeUrl(url), candidates);
                    listener.onError("未扫描到 RSS/Atom 候选地址，已扫描 " + candidates.size() + " 个地址: " + summarizeCandidates(candidates));
                } else {
                    listener.onSuccess(feedUrls);
                }
            } catch (Exception e) {
                listener.onError("扫描失败: " + e.getMessage());
            }
        });
    }

    public void addFeed(String url, OnFeedOperationListener listener) {
        executorService.execute(() -> {
            try {
                String feedUrl = normalizeUrl(url);
                if (!isValidFeedUrl(feedUrl)) {
                    feedUrl = resolveFeedUrl(feedUrl);
                }
                FeedEntity feed = new FeedEntity();
                feed.setUrl(feedUrl);
                feed.setTitle(extractFeedTitle(feedUrl));
                feed.setCreatedAt(String.valueOf(System.currentTimeMillis()));
                long feedId = feedDao.insert(feed);
                feed.setId((int) feedId);

                fetchAndParseFeed(feedUrl, feed.getId(), new OnArticlesLoadedListener() {
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

    public void importKnownFeeds(OnFeedOperationListener listener) {
        executorService.execute(() -> {
            try {
                removeUnavailableKnownFeeds();
                int importedCount = 0;
                for (KnownFeedSource source : KnownFeedSource.getAll()) {
                    String feedUrl = normalizeUrl(source.getUrl());
                    FeedEntity existing = feedDao.getFeedByUrl(feedUrl);
                    if (existing == null && feedUrl.startsWith("https://")) {
                        existing = feedDao.getFeedByUrl("http://" + feedUrl.substring(8));
                    }
                    if (existing != null) {
                        existing.setUrl(feedUrl);
                        existing.setTitle(source.getTitle());
                        existing.setDescription(source.getCategory());
                        feedDao.update(existing);
                        continue;
                    }

                    FeedEntity feed = new FeedEntity();
                    feed.setUrl(feedUrl);
                    feed.setTitle(source.getTitle());
                    feed.setDescription(source.getCategory());
                    feed.setCreatedAt(String.valueOf(System.currentTimeMillis()));
                    feedDao.insert(feed);
                    importedCount++;
                }
                if (importedCount == 0) {
                    repairKnownFeedSubscriptions();
                }
                listener.onSuccess("已导入 " + importedCount + " 个已知 RSS 源");
            } catch (Exception e) {
                listener.onError("导入失败: " + e.getMessage());
            }
        });
    }

    private void removeUnavailableKnownFeeds() {
        for (String url : KnownFeedSource.getRemovedUrls()) {
            feedDao.deleteByUrl(normalizeUrl(url));
        }
    }

    private void repairKnownFeedSubscriptions() {
        for (KnownFeedSource source : KnownFeedSource.getAll()) {
            String normalizedUrl = normalizeUrl(source.getUrl());
            FeedEntity existing = feedDao.getFeedByUrl(normalizedUrl);
            if (existing != null) {
                existing.setTitle(source.getTitle());
                existing.setDescription(source.getCategory());
                feedDao.update(existing);
            }
        }
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

    public void refreshAllFeeds(OnFeedOperationListener listener) {
        executorService.execute(() -> {
            try {
                RssSyncWorker.SyncResult result = syncFeedsSilently();
                if (result.failedCount > 0) {
                    listener.onSuccess("刷新完成，获取到 " + result.newArticleCount + " 篇文章，" + result.failedCount + " 个源失败");
                } else {
                    listener.onSuccess("刷新成功，获取到 " + result.newArticleCount + " 篇文章");
                }
            } catch (Exception e) {
                listener.onError("刷新失败: " + e.getMessage());
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

    /**
     * 静默同步所有 Feed（供 WorkManager 后台任务调用）。
     * 增量存储：按 link 去重，只插入新文章，不弹通知。
     * 注意：此方法是同步阻塞的，必须在后台线程调用。
     */
    public RssSyncWorker.SyncResult syncFeedsSilently() {
        List<FeedEntity> feeds = feedDao.getAllFeeds();
        int newArticleCount = 0;
        int failedCount = 0;

        for (FeedEntity feed : feeds) {
            if (isMalformedFeedUrl(feed.getUrl())) {
                feedDao.delete(feed);
                failedCount++;
                Log.e(TAG, "Removed malformed feed: " + feed.getUrl());
                continue;
            }
            try {
                newArticleCount += fetchAndSaveFeed(feed.getUrl(), feed.getId()).size();
            } catch (Exception e) {
                failedCount++;
                Log.e(TAG, "Sync failed for: " + feed.getUrl() + " — " + e.getMessage());
            }
        }

        return new RssSyncWorker.SyncResult(feeds.size(), newArticleCount, failedCount);
    }

    private void fetchAndParseFeed(String url, int feedId, OnArticlesLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<ArticleEntity> articles = fetchAndSaveFeed(url, feedId);
                listener.onSuccess(articles);
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
                listener.onError(e.getMessage());
            }
        });
    }

    private List<ArticleEntity> fetchAndSaveFeed(String url, int feedId) throws Exception {
        Log.d(TAG, "Fetching RSS from: " + url);
        String xmlContent = fetchString(url);
        List<ArticleEntity> articles = RssParser.parse(xmlContent, feedId);

        int newCount = 0;
        for (ArticleEntity article : articles) {
            try {
                ArticleEntity existing = articleDao.getArticleByLink(article.getLink());
                if (existing == null) {
                    long id = articleDao.insert(article);
                    article.setId(Math.toIntExact(id));
                    newCount++;

                    // 自动爬取网页补全内容（国外 RSS 常只有链接无正文）
                    if (shouldBackfillContent(article.getContent())) {
                        backfillArticleContent(article);
                    }
                    Log.d(TAG, "Inserted new article: " + article.getTitle());
                } else {
                    if (shouldReplaceContent(existing.getContent(), article.getContent())) {
                        existing.setContent(article.getContent());
                        articleDao.update(existing);
                        Log.d(TAG, "Updated existing article from feed: " + existing.getTitle());
                    }
                    Log.d(TAG, "Skipped duplicate: " + article.getTitle());
                }
            } catch (Exception e) {
                Log.e(TAG, "Insert/update error: " + e.getMessage());
            }
        }

        Log.d(TAG, "New articles: " + newCount + " / Total: " + articles.size());
        return articles;
    }

    private String resolveFeedUrl(String inputUrl) throws Exception {
        List<String> feedUrls = scanFeedUrls(inputUrl, true);
        if (!feedUrls.isEmpty()) {
            return feedUrls.get(0);
        }

        Set<String> candidates = collectFeedUrlCandidates(inputUrl);
        addKnownFeedCandidates(inputUrl, candidates);
        throw new Exception("未扫描到可用 RSS/Atom 订阅地址，已尝试 " + candidates.size() + " 个候选地址: " + summarizeCandidates(candidates));
    }

    private List<String> scanFeedUrls(String inputUrl, boolean onlyValid) {
        Set<String> candidates = collectFeedUrlCandidates(inputUrl);
        addKnownFeedCandidates(inputUrl, candidates);
        Log.e(TAG, "RSS scan candidates count: " + candidates.size());
        List<String> feedUrls = new ArrayList<>();
        for (String candidateUrl : candidates) {
            Log.e(TAG, "RSS scan candidate: " + candidateUrl);
            if (!looksLikeFeedUrl(candidateUrl)) {
                continue;
            }
            if (!onlyValid || isValidFeedUrl(candidateUrl)) {
                Log.e(TAG, "RSS scan matched: " + candidateUrl);
                feedUrls.add(candidateUrl);
            }
        }
        Log.e(TAG, "RSS scan matched count: " + feedUrls.size());
        return feedUrls;
    }

    private Set<String> collectFeedUrlCandidates(String inputUrl) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, inputUrl);
        addUrlVariants(candidates, inputUrl);
        addCommonFeedCandidates(candidates, inputUrl);

        Set<String> pagesToScan = new LinkedHashSet<>();
        addCandidate(pagesToScan, inputUrl);
        addUrlVariants(pagesToScan, inputUrl);
        String homeUrl = getHomeUrl(inputUrl);
        if (homeUrl != null) {
            addCandidate(pagesToScan, homeUrl);
            addUrlVariants(pagesToScan, homeUrl);
        }

        for (String pageUrl : pagesToScan) {
            addCommonFeedCandidates(candidates, pageUrl);
            collectFeedUrlsFromHtml(pageUrl, candidates);
        }
        return candidates;
    }

    private void addKnownFeedCandidates(String inputUrl, Set<String> candidates) {
        String key = inputUrl == null ? "" : inputUrl.toLowerCase(Locale.ROOT);
        String normalizedHost = normalizeHost(key);
        for (KnownFeedSource source : KnownFeedSource.getAll()) {
            String searchable = (source.getUrl() + " " + source.getTitle() + " " + source.getAliases()).toLowerCase(Locale.ROOT);
            if (key.isEmpty()
                    || searchable.contains(key)
                    || (!normalizedHost.isEmpty() && searchable.contains(normalizedHost))
                    || containsAlias(key, source.getAliases())) {
                addCandidate(candidates, source.getUrl());
            }
        }
    }

    private String normalizeHost(String url) {
        HttpUrl parsedUrl = HttpUrl.parse(url);
        if (parsedUrl == null) {
            return url.replace("https://", "").replace("http://", "").replace("www.", "").replace("/", "");
        }
        String host = parsedUrl.host();
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private boolean containsAlias(String key, String aliases) {
        for (String alias : aliases.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!alias.isEmpty() && key.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private void collectFeedUrlsFromHtml(String pageUrl, Set<String> candidates) {
        try {
            Document doc = Jsoup.connect(pageUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();
            collectFeedUrlsFromText(doc.html(), candidates);
            candidates.addAll(collectFeedCandidates(doc, pageUrl));
        } catch (Exception e) {
            Log.d(TAG, "Feed discovery from HTML failed: " + e.getMessage());
        }
    }

    private Set<String> collectFeedCandidates(Document doc, String pageUrl) {
        Set<String> candidates = new LinkedHashSet<>();

        Elements feedLinks = doc.select("link[href], a[href]");
        for (Element link : feedLinks) {
            String rel = link.attr("rel").toLowerCase(Locale.ROOT);
            String type = link.attr("type").toLowerCase(Locale.ROOT);
            String title = link.attr("title").toLowerCase(Locale.ROOT);
            String text = link.text().toLowerCase(Locale.ROOT);
            String hrefValue = link.attr("href").toLowerCase(Locale.ROOT);
            String href = link.absUrl("href");
            if (rel.contains("alternate")
                    || type.contains("rss") || type.contains("atom") || type.contains("xml")
                    || title.contains("rss") || title.contains("atom") || title.contains("feed") || title.contains("订阅")
                    || text.contains("rss") || text.contains("atom") || text.contains("订阅") || text.contains("feed")
                    || hrefValue.contains("rss") || hrefValue.contains("atom") || hrefValue.contains("feed")
                    || hrefValue.contains("xml") || hrefValue.contains("/wp-json/")) {
                addCandidate(candidates, href);
                addUrlVariants(candidates, href);
            }
        }

        Elements scripts = doc.select("script[src]");
        for (Element script : scripts) {
            String srcValue = script.attr("src").toLowerCase(Locale.ROOT);
            if (srcValue.contains("rss") || srcValue.contains("atom") || srcValue.contains("feed")) {
                String src = script.absUrl("src");
                addCandidate(candidates, src);
                addUrlVariants(candidates, src);
            }
        }

        addCommonFeedCandidates(candidates, pageUrl);
        return candidates;
    }

    private void addCandidate(Set<String> candidates, String url) {
        if (url != null && !url.trim().isEmpty()) {
            String candidate = cleanCandidateUrl(url.trim());
            if (candidate == null) {
                return;
            }
            if (candidate.startsWith("//")) {
                candidate = "https:" + candidate;
            }
            if (!isMalformedFeedUrl(candidate)) {
                candidates.add(candidate);
            }
        }
    }

    private String cleanCandidateUrl(String url) {
        int firstWhitespace = -1;
        for (int i = 0; i < url.length(); i++) {
            if (Character.isWhitespace(url.charAt(i))) {
                firstWhitespace = i;
                break;
            }
        }
        if (firstWhitespace >= 0) {
            url = url.substring(0, firstWhitespace);
        }
        url = url.replace("\\", "").replace("\"", "").replace("'", "");
        return url.isEmpty() ? null : url;
    }

    private boolean isMalformedFeedUrl(String url) {
        HttpUrl parsedUrl = HttpUrl.parse(url);
        if (parsedUrl == null) {
            return true;
        }
        String host = parsedUrl.host();
        String path = parsedUrl.encodedPath().toLowerCase(Locale.ROOT);
        return host.startsWith("ww.")
                || host.startsWith("w.")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".gif")
                || path.endsWith(".webp");
    }

    private void addUrlVariants(Set<String> candidates, String url) {
        HttpUrl parsedUrl = HttpUrl.parse(url);
        if (parsedUrl == null || isMalformedFeedUrl(url)) {
            return;
        }

        if ("https".equals(parsedUrl.scheme())) {
            candidates.add(parsedUrl.newBuilder().scheme("http").build().toString());
        } else if ("http".equals(parsedUrl.scheme())) {
            candidates.add(parsedUrl.newBuilder().scheme("https").build().toString());
        }

        String host = parsedUrl.host();
        if (host.startsWith("www.")) {
            candidates.add(parsedUrl.newBuilder().host(host.substring(4)).build().toString());
        } else {
            candidates.add(parsedUrl.newBuilder().host("www." + host).build().toString());
        }
    }

    private void collectFeedUrlsFromText(String html, Set<String> candidates) {
        Matcher matcher = FEED_URL_PATTERN.matcher(html);
        while (matcher.find()) {
            String url = matcher.group();
            addCandidate(candidates, url);
            addUrlVariants(candidates, url.startsWith("//") ? "https:" + url : url);
        }
    }

    private String summarizeCandidates(Set<String> candidates) {
        StringBuilder summary = new StringBuilder();
        int count = 0;
        for (String candidate : candidates) {
            if (count > 0) {
                summary.append(", ");
            }
            summary.append(candidate);
            count++;
            if (count >= 8) {
                break;
            }
        }
        if (candidates.size() > count) {
            summary.append(" ...");
        }
        return summary.toString();
    }

    private void addCommonFeedCandidates(Set<String> candidates, String pageUrl) {
        HttpUrl parsedUrl = HttpUrl.parse(pageUrl);
        if (parsedUrl == null) {
            return;
        }

        String[] rootPaths = {
                "feed", "atom", "rss.xml", "atom.xml", "feed.xml", "index.xml",
                "feed/", "atom/", "feed.php", "rss.php", "atom.php",
                "api/rss", "api/feed", "rss/index.xml", "feed/index.xml",
                "feeds/posts/default", "feeds/posts/default?alt=rss", "feeds/posts/default?alt=atom",
                "?feed=rss", "?feed=rss2", "?feed=atom", "?format=rss", "?format=feed", "?output=rss"
        };
        for (String path : rootPaths) {
            addPathCandidate(candidates, parsedUrl, path, true);
        }

        String encodedPath = parsedUrl.encodedPath();
        if (encodedPath != null && encodedPath.length() > 1) {
            String basePath = encodedPath.endsWith("/") ? encodedPath : encodedPath + "/";
            String[] nestedPaths = {"feed", "feed/", "atom", "rss.xml", "atom.xml", "feed.xml", "index.xml"};
            for (String path : nestedPaths) {
                addPathCandidate(candidates, parsedUrl, basePath + path, false);
            }
        }
    }

    private void addPathCandidate(Set<String> candidates, HttpUrl baseUrl, String path, boolean rootPath) {
        HttpUrl.Builder builder = baseUrl.newBuilder().fragment(null);
        if (path.startsWith("?")) {
            builder.encodedPath(rootPath ? "/" : baseUrl.encodedPath()).encodedQuery(path.substring(1));
        } else if (path.contains("?")) {
            String[] parts = path.split("\\?", 2);
            builder.encodedPath((rootPath ? "/" : "") + parts[0]).encodedQuery(parts[1]);
        } else {
            builder.encodedPath((rootPath ? "/" : "") + path).query(null);
        }
        candidates.add(builder.build().toString());
    }

    private String getHomeUrl(String url) {
        HttpUrl parsedUrl = HttpUrl.parse(url);
        if (parsedUrl == null) {
            return null;
        }
        return parsedUrl.newBuilder()
                .encodedPath("/")
                .query(null)
                .fragment(null)
                .build()
                .toString();
    }

    private boolean isValidFeedUrl(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return false;
            }
            String content = fetchString(url);
            return isFeedContent(content) || !RssParser.parse(content, 0).isEmpty();
        } catch (Exception e) {
            Log.d(TAG, "Feed candidate invalid: " + url);
            return false;
        }
    }

    private boolean looksLikeFeedUrl(String url) {
        if (url == null) {
            return false;
        }
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        return lowerUrl.contains("rss")
                || lowerUrl.contains("atom")
                || lowerUrl.contains("feed")
                || lowerUrl.contains("xml")
                || lowerUrl.contains("wp-json");
    }

    private void backfillArticleContent(ArticleEntity article) {
        if (!shouldBackfillContent(article.getContent())) {
            return;
        }

        executorService.execute(() -> {
            String extractedContent = extractArticleContent(article.getLink());
            if (shouldReplaceContent(article.getContent(), extractedContent)) {
                article.setContent(extractedContent);
                articleDao.update(article);
            }
        });
    }

    private String extractArticleContent(String articleUrl) {
        if (articleUrl == null || articleUrl.trim().isEmpty()) {
            return null;
        }

        try {
            Document doc = Jsoup.connect(articleUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();
            return WebContentExtractor.extractMainContent(doc);
        } catch (Exception e) {
            Log.d(TAG, "Article content extraction failed: " + e.getMessage());
            return null;
        }
    }

    private boolean shouldBackfillContent(String content) {
        if (content == null) {
            return true;
        }

        String text = Jsoup.parse(content).text().trim();
        return text.isEmpty()
                || NO_CONTENT_TEXT.equals(text)
                || "该 RSS 源未提供文章正文内容".equals(text)
                || "暂无内容".equals(text)
                || text.length() < 300;
    }

    private boolean shouldReplaceContent(String oldContent, String newContent) {
        if (newContent == null) {
            return false;
        }

        String newText = Jsoup.parse(newContent).text().trim();
        if (newText.length() < 120 || NO_CONTENT_TEXT.equals(newText)) {
            return false;
        }

        String oldText = oldContent == null ? "" : Jsoup.parse(oldContent).text().trim();
        return oldText.isEmpty()
                || NO_CONTENT_TEXT.equals(oldText)
                || "该 RSS 源未提供文章正文内容".equals(oldText)
                || "暂无内容".equals(oldText)
                || newText.length() > oldText.length() * 2;
    }

    private boolean isFeedContent(String content) {
        if (content == null) {
            return false;
        }

        String trimmed = content.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("<?xml") && (trimmed.contains("<rss") || trimmed.contains("<feed") || trimmed.contains("<rdf:rdf"))
                || trimmed.startsWith("<rss")
                || trimmed.startsWith("<feed")
                || trimmed.startsWith("<rdf:rdf");
    }

    private String fetchString(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("HTTP " + response.code());
            }
            if (response.body() == null) {
                throw new Exception("响应内容为空");
            }
            return response.body().string();
        }
    }

    private String normalizeUrl(String url) {
        String normalizedUrl = url.trim();
        if (normalizedUrl.startsWith("//")) {
            return "https:" + normalizedUrl;
        }
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            normalizedUrl = "https://" + normalizedUrl;
        }
        return normalizedUrl;
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

    /** 同步获取所有文章（供后台线程调用） */
    public List<ArticleEntity> getAllArticlesSync() {
        return articleDao.getAllArticles();
    }

    /** 同步获取所有订阅（供后台线程调用） */
    public List<FeedEntity> getAllFeedsSync() {
        return feedDao.getAllFeeds();
    }

    /** 同步已知源分类：匹配 KnownFeedSource，为已有订阅补充分类标签（后台异步） */
    public void syncFeedCategories() {
        executorService.execute(() -> {
            List<FeedEntity> feeds = feedDao.getAllFeeds();
            for (FeedEntity feed : feeds) {
                for (KnownFeedSource source : KnownFeedSource.getAll()) {
                    if (source.getUrl().equals(feed.getUrl())) {
                        String oldDesc = feed.getDescription();
                        if (oldDesc == null || !oldDesc.equals(source.getCategory())) {
                            feed.setDescription(source.getCategory());
                            feedDao.update(feed);
                            Log.d(TAG, "Synced category for " + feed.getTitle() + " → " + source.getCategory());
                        }
                        break;
                    }
                }
            }
        });
    }
}
