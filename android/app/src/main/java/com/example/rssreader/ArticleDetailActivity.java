package com.example.rssreader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ArticleDetailActivity extends AppCompatActivity {

    private TextView titleText;
    private TextView metaText;
    private WebView contentWebView;
    private Button btnTranslate;
    private Button btnAISummary;
    private CardView translationCard;
    private CardView summaryCard;
    private TextView translatedText;
    private TextView summaryText;
    private FloatingActionButton fabFavorite;
    private FloatingActionButton fabOpenBrowser;

    private String articleTitle;
    private String articleAuthor;
    private String articlePublished;
    private String articleContent;
    private String articleLink;
    private int articleId;
    private boolean isFavorited = false;
    private boolean isTranslated = false;
    private boolean isSummarized = false;
    private RssRepository repository;
    private AIConfigManager aiConfigManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        repository = new RssRepository(this);
        aiConfigManager = new AIConfigManager(this);

        // 获取传递的数据
        Intent intent = getIntent();
        articleTitle = intent.getStringExtra("title");
        articleAuthor = intent.getStringExtra("author");
        articlePublished = intent.getStringExtra("published");
        articleContent = intent.getStringExtra("content");
        articleLink = intent.getStringExtra("link");
        articleId = intent.getIntExtra("articleId", 0);
        isFavorited = intent.getBooleanExtra("isFavorited", false);

        initViews();
        displayArticle();
    }

    private void initViews() {
        titleText = findViewById(R.id.articleTitle);
        metaText = findViewById(R.id.articleMeta);
        contentWebView = findViewById(R.id.articleContent);
        btnTranslate = findViewById(R.id.btnTranslate);
        btnAISummary = findViewById(R.id.btnAISummary);
        translationCard = findViewById(R.id.translationCard);
        summaryCard = findViewById(R.id.summaryCard);
        translatedText = findViewById(R.id.translatedText);
        summaryText = findViewById(R.id.summaryText);
        fabFavorite = findViewById(R.id.fabFavorite);
        fabOpenBrowser = findViewById(R.id.fabOpenBrowser);

        // 配置 WebView
        WebSettings webSettings = contentWebView.getSettings();
        webSettings.setJavaScriptEnabled(false);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

        // 收藏按钮
        updateFavoriteIcon(isFavorited);
        fabFavorite.setOnClickListener(v -> {
            isFavorited = !isFavorited;
            updateFavoriteIcon(isFavorited);

            // 保存到数据库
            repository.toggleFavorite(articleId, isFavorited);

            Toast.makeText(this, isFavorited ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
        });

        // 翻译按钮
        btnTranslate.setOnClickListener(v -> {
            if (isTranslated) {
                // 隐藏翻译
                translationCard.setVisibility(View.GONE);
                btnTranslate.setText("翻译全文");
                isTranslated = false;
            } else {
                // 显示翻译
                translateArticle();
            }
        });

        // AI 总结按钮
        btnAISummary.setOnClickListener(v -> {
            if (isSummarized) {
                summaryCard.setVisibility(View.GONE);
                isSummarized = false;
                btnAISummary.setText("🤖 AI 总结");
            } else {
                generateAISummary();
            }
        });

        // FAB 按钮点击 - 在浏览器打开原文
        fabOpenBrowser.setOnClickListener(v -> {
            if (articleLink != null && !articleLink.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleLink));
                startActivity(browserIntent);
            }
        });
    }

    private void displayArticle() {
        // 标题
        titleText.setText(articleTitle != null ? articleTitle : "无标题");

        // 元信息（作者 + 时间）
        StringBuilder meta = new StringBuilder();
        if (articleAuthor != null && !articleAuthor.isEmpty()) {
            meta.append("作者: ").append(articleAuthor);
        }
        if (articlePublished != null && !articlePublished.isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(formatDate(articlePublished));
        }
        metaText.setText(meta.toString());

        // 内容
        String htmlContent = buildHtmlContent(articleContent);
        contentWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    private void translateArticle() {
        btnTranslate.setEnabled(false);
        btnTranslate.setText("翻译中...");

        // 组合标题和内容一起翻译
        StringBuilder textToTranslate = new StringBuilder();

        // 添加标题
        if (articleTitle != null && !articleTitle.isEmpty()) {
            textToTranslate.append(articleTitle).append("\n\n");
        }

        // 添加内容（提取纯文本）
        String contentText = stripHtml(articleContent);
        if (contentText != null && !contentText.isEmpty()) {
            textToTranslate.append(contentText);
        }

        TranslationService.translate(textToTranslate.toString(), new TranslationService.TranslationCallback() {
            @Override
            public void onSuccess(String translated) {
                runOnUiThread(() -> {
                    btnTranslate.setEnabled(true);
                    btnTranslate.setText("隐藏译文");
                    translatedText.setText(translated);
                    translationCard.setVisibility(View.VISIBLE);
                    isTranslated = true;
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnTranslate.setEnabled(true);
                    btnTranslate.setText("翻译全文");
                    Toast.makeText(ArticleDetailActivity.this, "翻译失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        // 简单去除 HTML 标签
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&amp;", "&")
                   .trim();
    }

    private String buildHtmlContent(String content) {
        if (content == null || content.isEmpty()) {
            content = "<p>暂无内容</p>";
        }

        // 构建完整的 HTML
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { " +
                "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "  font-size: 16px; " +
                "  line-height: 1.6; " +
                "  color: #333; " +
                "  padding: 0; " +
                "  margin: 0; " +
                "} " +
                "img { max-width: 100%; height: auto; } " +
                "a { color: #2196F3; text-decoration: none; } " +
                "p { margin-bottom: 1em; } " +
                "pre { background: #f5f5f5; padding: 10px; overflow-x: auto; } " +
                "code { background: #f5f5f5; padding: 2px 5px; } " +
                "</style>" +
                "</head>" +
                "<body>" +
                content +
                "</body>" +
                "</html>";
    }

    private String formatDate(String dateString) {
        try {
            return dateString.substring(0, Math.min(dateString.length(), 19)).replace("T", " ");
        } catch (Exception e) {
            return dateString;
        }
    }

    private void updateFavoriteIcon(boolean isFavorited) {
        if (fabFavorite != null) {
            fabFavorite.setImageResource(isFavorited ?
                android.R.drawable.star_big_on : android.R.drawable.star_big_off);
        }
    }

    private void generateAISummary() {
        // 检查是否配置了 AI
        if (!aiConfigManager.isConfigured()) {
            Toast.makeText(this, "请先在订阅管理页面配置 AI", Toast.LENGTH_LONG).show();
            return;
        }

        btnAISummary.setEnabled(false);
        btnAISummary.setText("正在生成总结...");

        // 如果有正文内容，直接总结
        if (articleContent != null && !articleContent.isEmpty()
                && !articleContent.equals("该 RSS 源未提供文章正文内容")) {
            AISummaryService.summarize(this, articleContent, summaryCallback);
        } else {
            // 没有正文，先抓取网页
            Toast.makeText(this, "正在抓取网页...", Toast.LENGTH_SHORT).show();
            WebContentExtractor.extractContent(articleLink, new WebContentExtractor.ExtractionCallback() {
                @Override
                public void onSuccess(String extractedContent) {
                    runOnUiThread(() -> {
                        Toast.makeText(ArticleDetailActivity.this, "正在生成 AI 总结...", Toast.LENGTH_SHORT).show();
                        AISummaryService.summarize(ArticleDetailActivity.this, extractedContent, summaryCallback);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnAISummary.setEnabled(true);
                        btnAISummary.setText(" AI 总结");
                        Toast.makeText(ArticleDetailActivity.this, "网页抓取失败: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private final AISummaryService.SummaryCallback summaryCallback = new AISummaryService.SummaryCallback() {
        @Override
        public void onSuccess(String summary) {
            runOnUiThread(() -> {
                btnAISummary.setEnabled(true);
                btnAISummary.setText("隐藏总结");
                summaryText.setText(summary);
                summaryCard.setVisibility(View.VISIBLE);
                isSummarized = true;
            });
        }

        @Override
        public void onError(String error) {
            runOnUiThread(() -> {
                btnAISummary.setEnabled(true);
                btnAISummary.setText(" AI 总结");
                Toast.makeText(ArticleDetailActivity.this, "AI 总结失败: " + error, Toast.LENGTH_LONG).show();
            });
        }
    };

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
