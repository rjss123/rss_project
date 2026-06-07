package com.example.rssreader.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.rssreader.AIConfigManager;
import com.example.rssreader.R;
import com.example.rssreader.RssRepository;
import com.example.rssreader.database.ArticleEntity;
import com.example.rssreader.database.FeedEntity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DailySummaryFragment extends Fragment {

    private static final String HINT_NOT_CONFIGURED = "请先在「订阅」页面点击 AI 配置按钮，设置 API Key。";

    private TextView summaryText;
    private TextView statusText;
    private TextView errorText;
    private ProgressBar progressBar;
    private Button btnRefresh;
    private SwipeRefreshLayout swipeRefresh;
    private RssRepository repository;
    private AIConfigManager aiConfig;
    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new RssRepository(requireContext());
        aiConfig = new AIConfigManager(requireContext());
        executor = Executors.newSingleThreadExecutor();

        summaryText = view.findViewById(R.id.summaryText);
        statusText = view.findViewById(R.id.statusText);
        errorText = view.findViewById(R.id.errorText);
        progressBar = view.findViewById(R.id.progressBar);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        btnRefresh.setOnClickListener(v -> generateSummary());
        swipeRefresh.setOnRefreshListener(this::generateSummary);

        // 检查 AI 配置
        if (!aiConfig.isConfigured()) {
            summaryText.setText(HINT_NOT_CONFIGURED);
            statusText.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.GONE);
            return;
        }

        generateSummary();
    }

    private void generateSummary() {
        showLoading(true);

        executor.execute(() -> {
            try {
                // 1. 获取今天发布的科技类文章
                List<ArticleEntity> todayArticles = getTodayTechArticles();
                if (todayArticles.isEmpty()) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        summaryText.setText("今天还没有科技类文章更新，先去刷新订阅吧。");
                    });
                    return;
                }

                // 2. 构建摘要文本
                StringBuilder feedText = new StringBuilder();
                for (int i = 0; i < Math.min(todayArticles.size(), 30); i++) {
                    ArticleEntity a = todayArticles.get(i);
                    feedText.append("[").append(i + 1).append("] ");
                    feedText.append(a.getTitle());
                    String desc = stripHtml(a.getDescription());
                    if (desc != null && desc.length() > 5) {
                        feedText.append(" —— ");
                        feedText.append(desc.substring(0, Math.min(desc.length(), 200)));
                    }
                    feedText.append("\n\n");
                }

                // 3. 调用 AI 生成日报
                String summary = callAIForDigest(feedText.toString(), todayArticles.size());

                mainHandler.post(() -> {
                    showLoading(false);
                    errorText.setVisibility(View.GONE);
                    summaryText.setText(summary);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    errorText.setText("生成失败: " + e.getMessage());
                    errorText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private List<ArticleEntity> getTodayTechArticles() {
        // 获取所有文章和订阅，筛选今天发布的科技类
        List<ArticleEntity> allArticles = repository.getAllArticlesSync();
        List<FeedEntity> allFeeds = repository.getAllFeedsSync();

        // 建立 feedId → 分类映射
        java.util.Map<Integer, String> feedCategory = new java.util.HashMap<>();
        for (FeedEntity f : allFeeds) {
            String desc = f.getDescription();
            feedCategory.put(f.getId(), desc != null ? desc : "");
        }

        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        long todayMillis = todayStart.getTimeInMillis();

        List<ArticleEntity> todayList = new ArrayList<>();
        for (ArticleEntity a : allArticles) {
            // 只看科技分类
            String cat = feedCategory.get(a.getFeedId());
            if (cat == null || !cat.equals("科技")) continue;

            if (a.getPublished() >= todayMillis) {
                todayList.add(a);
            }
        }
        return todayList;
    }

    private String callAIForDigest(String articleText, int count) throws Exception {
        String apiKey = aiConfig.getApiKey();
        String model = aiConfig.getModel();
        String endpoint = normalizeEndpoint(aiConfig.getApiUrl());

        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("temperature", 0.5);
        payload.put("max_tokens", 1500);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "你是一名资深科技编辑。请根据以下今日科技资讯，撰写一份简洁的科技日报。" +
                        "要求：1) 用中文输出 2) 按重要性排列 3) 每条用「•」开头 4) 各条控制在 2-3 句话 " +
                        "5) 最后加一段「今日趋势」总结（50 字内）。"));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "以下是今天采集的 " + count + " 条科技资讯标题和摘要，请生成今日科技日报：\n\n" + articleText));
        payload.put("messages", messages);

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(90000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        if (code >= 200 && code < 300) {
            JSONObject json = new JSONObject(response.toString());
            return json.getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content").trim();
        }
        throw new Exception("API " + code + ": " + response);
    }

    private String normalizeEndpoint(String apiUrl) {
        String url = apiUrl == null || apiUrl.trim().isEmpty()
                ? "https://api.deepseek.com" : apiUrl.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) return url;
        if (url.endsWith("/v1")) return url + "/chat/completions";
        return url + "/chat/completions";
    }

    private void showLoading(boolean loading) {
        swipeRefresh.setRefreshing(false);
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            statusText.setVisibility(View.VISIBLE);
            btnRefresh.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            statusText.setVisibility(View.GONE);
            btnRefresh.setEnabled(true);
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 用户可能刚配置好 API Key，重新检查
        if (aiConfig != null && aiConfig.isConfigured()) {
            btnRefresh.setVisibility(View.VISIBLE);
            if (summaryText.getText().toString().isEmpty()
                    || HINT_NOT_CONFIGURED.equals(summaryText.getText().toString())) {
                generateSummary();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}
