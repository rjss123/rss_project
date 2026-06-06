package com.example.rssreader;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TranslationService {

    private static final String TAG = "TranslationService";

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }

    // 使用 LibreTranslate 免费 API
    public static void translate(String text, TranslationCallback callback) {
        new TranslateTask(text, callback).execute();
    }

    private static class TranslateTask extends AsyncTask<Void, Void, String> {
        private String text;
        private TranslationCallback callback;
        private String errorMessage;

        TranslateTask(String text, TranslationCallback callback) {
            this.text = text;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 使用 MyMemory 翻译 API (免费，无需密钥)
                String encodedText = URLEncoder.encode(text, "UTF-8");
                String urlString = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=en|zh";

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // 解析 JSON 响应
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject responseData = jsonResponse.getJSONObject("responseData");
                    String translatedText = responseData.getString("translatedText");

                    return translatedText;
                } else {
                    errorMessage = "HTTP 错误: " + responseCode;
                    return null;
                }

            } catch (Exception e) {
                Log.e(TAG, "翻译失败: " + e.getMessage(), e);
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && callback != null) {
                callback.onSuccess(result);
            } else if (callback != null) {
                callback.onError(errorMessage != null ? errorMessage : "翻译失败");
            }
        }
    }
}
