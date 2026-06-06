package com.example.rssreader;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AIConfigActivity extends AppCompatActivity {

    private EditText editApiUrl;
    private EditText editApiKey;
    private EditText editModel;
    private Spinner spinnerProvider;
    private Button btnSave;
    private Button btnTestAI;

    private AIConfigManager configManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_config);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI 配置");
        }

        configManager = new AIConfigManager(this);

        initViews();
        loadConfig();
        setupListeners();
    }

    private void initViews() {
        editApiUrl = findViewById(R.id.editApiUrl);
        editApiKey = findViewById(R.id.editApiKey);
        editModel = findViewById(R.id.editModel);
        spinnerProvider = findViewById(R.id.spinnerProvider);
        btnSave = findViewById(R.id.btnSave);
        btnTestAI = findViewById(R.id.btnTestAI);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ai_providers, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvider.setAdapter(adapter);
    }

    private void loadConfig() {
        editApiUrl.setText(configManager.getApiUrl());
        editApiKey.setText(configManager.getApiKey());
        editModel.setText(configManager.getModel());

        String provider = configManager.getProvider();
        if ("openai".equals(provider)) {
            spinnerProvider.setSelection(0);
        } else if ("claude".equals(provider)) {
            spinnerProvider.setSelection(1);
        } else if ("deepseek".equals(provider)) {
            spinnerProvider.setSelection(2);
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveConfig());
        btnTestAI.setOnClickListener(v -> testConnection());
    }

    private void saveConfig() {
        if (!saveConfigFromInputs()) {
            return;
        }
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean saveConfigFromInputs() {
        String apiUrl = editApiUrl.getText().toString().trim();
        String apiKey = editApiKey.getText().toString().trim();
        String model = editModel.getText().toString().trim();
        String provider = spinnerProvider.getSelectedItem().toString().toLowerCase();

        if (apiUrl.isEmpty()) {
            Toast.makeText(this, "请输入 AI API 地址", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请输入 AI API Key", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (model.isEmpty()) {
            Toast.makeText(this, "请输入模型名", Toast.LENGTH_SHORT).show();
            return false;
        }

        configManager.setApiUrl(apiUrl);
        configManager.setApiKey(apiKey);
        configManager.setModel(model);
        configManager.setProvider(provider);
        return true;
    }

    private void testConnection() {
        if (!saveConfigFromInputs()) {
            return;
        }

        Toast.makeText(this, "测试连接中...", Toast.LENGTH_SHORT).show();

        String testText = "This is a test article.";
        AISummaryService.summarize(this, testText, new AISummaryService.SummaryCallback() {
            @Override
            public void onSuccess(String summary) {
                runOnUiThread(() -> Toast.makeText(AIConfigActivity.this,
                        "连接成功！\n总结：" + summary,
                        Toast.LENGTH_LONG).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(AIConfigActivity.this,
                        "连接失败：" + error,
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
