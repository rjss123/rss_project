package com.example.rssreader;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AISummaryService {

    private static final String TAG = "AISummaryService";

    public interface SummaryCallback {
        void onSuccess(String summary);
        void onError(String error);
    }

    public static void summarize(Context context, String content, SummaryCallback callback) {
        new SummarizeTask(context.getApplicationContext(), content, callback).execute();
    }

    private static class SummarizeTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String content;
        private final SummaryCallback callback;
        private String errorMessage;

        SummarizeTask(Context context, String content, SummaryCallback callback) {
            this.context = context;
            this.content = content == null ? "" : content;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                AIConfigManager configManager = new AIConfigManager(context);
                String apiKey = configManager.getApiKey();
                String model = configManager.getModel();
                String endpoint = normalizeEndpoint(configManager.getApiUrl());

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    errorMessage = "请先配置 DeepSeek API Key";
                    return null;
                }

                String textToSummarize = content.trim();
                if (textToSummarize.length() > 6000) {
                    textToSummarize = textToSummarize.substring(0, 6000) + "...";
                }

                JSONObject payload = new JSONObject();
                payload.put("model", model);
                payload.put("temperature", 0.3);
                payload.put("max_tokens", 500);

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", "你是一个专业的 RSS 文章总结助手。请用中文总结文章核心内容，结构清晰，控制在 200 字以内。"));
                messages.put(new JSONObject()
                        .put("role", "user")
                        .put("content", "请总结以下文章：\n\n" + textToSummarize));
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
                        errorMessage = "AI 没有返回总结";
                        return null;
                    }
                    return choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();
                }

                errorMessage = "DeepSeek API 错误: HTTP " + responseCode + " - " + response;
                return null;
            } catch (Exception e) {
                Log.e(TAG, "AI 总结失败: " + e.getMessage(), e);
                errorMessage = "AI 总结失败: " + e.getMessage();
                return null;
            }
        }

        private String normalizeEndpoint(String apiUrl) {
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

        @Override
        protected void onPostExecute(String result) {
            if (result != null && callback != null) {
                callback.onSuccess(result);
            } else if (callback != null) {
                callback.onError(errorMessage != null ? errorMessage : "总结失败");
            }
        }
    }
}
