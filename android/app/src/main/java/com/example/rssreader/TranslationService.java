package com.example.rssreader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 翻译服务，使用 DeepSeek / OpenAI 兼容 API。
 * 复用 AIConfigManager 的 API Key / Endpoint / Model 配置。
 */
public class TranslationService {

    private static final String TAG = "TranslationService";
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }

    /**
     * 使用 AI 将文本翻译为中文。
     *
     * @param context  用于读取 AIConfigManager 配置
     * @param text     待翻译文本
     * @param callback 结果回调（主线程）
     */
    public static void translate(Context context, String text, TranslationCallback callback) {
        executor.execute(() -> {
            try {
                AIConfigManager configManager = new AIConfigManager(context.getApplicationContext());
                String apiKey = configManager.getApiKey();
                String model = configManager.getModel();
                String endpoint = normalizeEndpoint(configManager.getApiUrl());

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    postError(callback, "请先配置 AI API Key");
                    return;
                }

                String textToTranslate = text == null ? "" : text.trim();
                if (textToTranslate.isEmpty()) {
                    postSuccess(callback, "");
                    return;
                }

                // 截断过长文本
                final int maxChars = 8000;
                if (textToTranslate.length() > maxChars) {
                    textToTranslate = textToTranslate.substring(0, maxChars) + "...";
                }

                JSONObject payload = new JSONObject();
                payload.put("model", model);
                payload.put("temperature", 0.1);
                payload.put("max_tokens", Math.min(textToTranslate.length() + 500, 4096));

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", "你是一名专业翻译。请将以下文本翻译为中文。如果原文已是中文则保持不变。只输出译文，不附加解释。"));
                messages.put(new JSONObject()
                        .put("role", "user")
                        .put("content", textToTranslate));
                payload.put("messages", messages);

                URL url = new URL(endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream(),
                        StandardCharsets.UTF_8));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() == 0) {
                        postError(callback, "AI 没有返回翻译结果");
                        return;
                    }
                    String translated = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();
                    postSuccess(callback, translated);
                } else {
                    postError(callback, "API 错误: HTTP " + responseCode + " - " + response);
                }
            } catch (Exception e) {
                Log.e(TAG, "AI 翻译失败: " + e.getMessage(), e);
                postError(callback, "翻译失败: " + e.getMessage());
            }
        });
    }

    private static String normalizeEndpoint(String apiUrl) {
        String url = apiUrl == null || apiUrl.trim().isEmpty()
                ? "https://api.deepseek.com"
                : apiUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/chat/completions")) {
            return url;
        }
        if (url.endsWith("/v1")) {
            return url + "/chat/completions";
        }
        return url + "/chat/completions";
    }

    private static void postSuccess(TranslationCallback callback, String result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(result));
        }
    }

    private static void postError(TranslationCallback callback, String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }
}
