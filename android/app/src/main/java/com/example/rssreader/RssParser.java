package com.example.rssreader;

import android.util.Log;
import com.example.rssreader.database.ArticleEntity;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.xml.sax.InputSource;

/**
 * RSS/Atom/RDF 解析器，委托给 Rome 库。
 * 支持 RSS 0.90-2.0、Atom 0.3-1.0、RDF。
 */
public class RssParser {

    private static final String TAG = "RssParser";

    /**
     * 从原始 XML 字符串解析 RSS/Atom Feed。
     *
     * @param xmlContent XML 内容
     * @param feedId     数据库中的 Feed ID
     * @return 解析出的文章列表
     */
    public static List<ArticleEntity> parse(String xmlContent, int feedId) {
        List<ArticleEntity> articles = new ArrayList<>();

        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new InputSource(new StringReader(xmlContent)));

            List<SyndEntry> entries = feed.getEntries();
            if (entries == null || entries.isEmpty()) {
                Log.w(TAG, "Feed returned empty entries");
                return articles;
            }

            Log.d(TAG, "Feed has " + entries.size() + " entries");

            for (int i = 0; i < entries.size(); i++) {
                SyndEntry entry = entries.get(i);
                try {
                    ArticleEntity article = mapEntry(entry, feedId, i);
                    articles.add(article);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to map entry " + i, e);
                }
            }

            Log.d(TAG, "Successfully parsed " + articles.size() + " articles");

        } catch (Exception e) {
            Log.e(TAG, "Error parsing RSS: " + e.getMessage(), e);
        }

        return articles;
    }

    // ---------- 字段映射 ----------

    private static ArticleEntity mapEntry(SyndEntry entry, int feedId, int index) {
        ArticleEntity article = new ArticleEntity();
        article.setServerId(index);
        article.setFeedId(feedId);

        // 标题
        String title = nullToEmpty(entry.getTitle());
        if (title.isEmpty()) title = "无标题";
        article.setTitle(title);

        // 链接
        article.setLink(nullToEmpty(entry.getLink()));

        // 作者
        article.setAuthor(nullToEmpty(entry.getAuthor()));

        // 描述
        String description = nullToEmpty(getDescriptionValue(entry));
        article.setDescription(description);

        // 正文：优先 contents（完整正文），其次 description
        String content = nullToEmpty(getContentsValue(entry));
        if (!content.isEmpty()) {
            article.setContent(content);
        } else if (!description.isEmpty()) {
            article.setContent(description);
        } else {
            article.setContent("该 RSS 源未提供文章内容");
        }

        // 发布时间（毫秒时间戳，保证 ORDER BY 数字 = 时间序）
        Date pubDate = entry.getPublishedDate();
        if (pubDate == null) pubDate = entry.getUpdatedDate();
        article.setPublished(pubDate != null ? pubDate.getTime() : 0L);

        // 状态
        article.setRead(false);
        article.setFavorited(false);
        article.setCreatedAt(String.valueOf(System.currentTimeMillis()));

        Log.d(TAG, "Parsed: " + title + ", Content length: "
                + (article.getContent() != null ? article.getContent().length() : 0));

        return article;
    }

    /**
     * 获取描述（纯文本），Rome 对 RSS description 和 Atom summary 统一处理
     */
    private static String getDescriptionValue(SyndEntry entry) {
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue();
        }
        return null;
    }

    /**
     * 获取正文（HTML/纯文本），Rome 对 content:encoded 和 Atom content 统一处理
     */
    private static String getContentsValue(SyndEntry entry) {
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            return entry.getContents().get(0).getValue();
        }
        return null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
