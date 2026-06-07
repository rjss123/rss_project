package com.example.rssreader;

import android.os.AsyncTask;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebContentExtractor {

    private static final String TAG = "WebContentExtractor";

    public interface ExtractionCallback {
        void onSuccess(String extractedContent);
        void onError(String error);
    }

    /**
     * 从 URL 提取网页正文
     */
    public static void extractContent(String url, ExtractionCallback callback) {
        new ExtractTask(url, callback).execute();
    }

    private static class ExtractTask extends AsyncTask<Void, Void, String> {
        private String url;
        private ExtractionCallback callback;
        private String errorMessage;

        ExtractTask(String url, ExtractionCallback callback) {
            this.url = url;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                Log.d(TAG, "Fetching content from: " + url);

                // 使用 Jsoup 抓取网页
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();

                // 提取正文内容
                String content = extractMainContent(doc);

                if (content == null || content.trim().isEmpty()) {
                    errorMessage = "无法提取网页正文";
                    return null;
                }

                Log.d(TAG, "Extracted content length: " + content.length());
                return content;

            } catch (Exception e) {
                Log.e(TAG, "Error extracting content: " + e.getMessage(), e);
                errorMessage = "网页抓取失败: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && callback != null) {
                callback.onSuccess(result);
            } else if (callback != null) {
                callback.onError(errorMessage != null ? errorMessage : "提取失败");
            }
        }
    }

    /**
     * 智能提取网页主要内容
     */
    public static String extractMainContent(Document doc) {
        StringBuilder content = new StringBuilder();

        // 移除不需要的元素
        doc.select("script, style, nav, header, footer, aside, .ad, .ads, .advertisement, .social-share, .comment, .comments").remove();

        // 尝试多种选择器提取正文（按优先级）
        Element mainContent = null;

        // 1. 常见的文章容器
        mainContent = doc.selectFirst("article");
        if (mainContent == null) {
            mainContent = doc.selectFirst("main");
        }
        if (mainContent == null) {
            mainContent = doc.selectFirst(".post-content, .article-content, .entry-content, .post-body, .article-body, .article__content, .post__content");
        }
        if (mainContent == null) {
            mainContent = doc.selectFirst("[role=main]");
        }
        if (mainContent == null) {
            mainContent = doc.selectFirst("#content, #main, .content, .main, .post, .article");
        }

        // 2. 如果找到主容器，提取文本
        if (mainContent != null) {
            // 提取标题
            Element title = mainContent.selectFirst("h1, h2");
            if (title != null) {
                content.append(title.text()).append("\n\n");
            }

            // 提取段落
            Elements paragraphs = mainContent.select("p");
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 20) {  // 过滤太短的段落
                    content.append(text).append("\n\n");
                }
            }
        } else {
            // 3. 降级方案：提取所有段落
            Elements paragraphs = doc.select("p");
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 50) {
                    content.append(text).append("\n\n");
                }
            }
        }

        // 限制长度（前 5000 字符）
        String result = content.toString().trim();
        if (result.length() > 5000) {
            result = result.substring(0, 5000) + "...";
        }

        return result;
    }
}
