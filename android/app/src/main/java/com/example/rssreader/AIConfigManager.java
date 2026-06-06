package com.example.rssreader;

import android.content.Context;
import android.content.SharedPreferences;

public class AIConfigManager {
    private static final String PREFS_NAME = "ai_config";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_MODEL = "model";

    private SharedPreferences prefs;

    public AIConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Getters
    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public String getApiUrl() {
        return prefs.getString(KEY_API_URL, "https://api.deepseek.com");
    }

    public String getProvider() {
        return prefs.getString(KEY_PROVIDER, "deepseek");
    }

    public String getModel() {
        return prefs.getString(KEY_MODEL, "deepseek-v4-flash");
    }

    // Setters
    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public void setApiUrl(String apiUrl) {
        prefs.edit().putString(KEY_API_URL, apiUrl).apply();
    }

    public void setProvider(String provider) {
        prefs.edit().putString(KEY_PROVIDER, provider).apply();
    }

    public void setModel(String model) {
        prefs.edit().putString(KEY_MODEL, model).apply();
    }

    // 检查是否已配置
    public boolean isConfigured() {
        return !getApiKey().isEmpty();
    }
}
