package com.example.rssreader;

import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import com.example.rssreader.database.ArticleEntity;

public class RssParser {

    private static final String TAG = "RssParser";

    public static List<ArticleEntity> parse(String xmlContent, int feedId) {
        List<ArticleEntity> articles = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            doc.getDocumentElement().normalize();

            // 尝试解析 RSS 2.0 格式
            NodeList itemList = doc.getElementsByTagName("item");

            // 如果没有找到 item，尝试 Atom 格式
            if (itemList.getLength() == 0) {
                itemList = doc.getElementsByTagName("entry");
            }

            Log.d(TAG, "Found " + itemList.getLength() + " items in feed");

            for (int i = 0; i < itemList.getLength(); i++) {
                Node node = itemList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    ArticleEntity article = parseItem(element, feedId, i);
                    if (article != null) {
                        articles.add(article);
                    }
                }
            }

            Log.d(TAG, "Successfully parsed " + articles.size() + " articles");

        } catch (Exception e) {
            Log.e(TAG, "Error parsing RSS: " + e.getMessage(), e);
        }

        return articles;
    }

    private static ArticleEntity parseItem(Element element, int feedId, int index) {
        try {
            ArticleEntity article = new ArticleEntity();
            article.setServerId(index);
            article.setFeedId(feedId);

            // ========== 标题 ==========
            String title = getElementValue(element, "title");
            if (title == null || title.isEmpty()) {
                title = "无标题";
            }
            article.setTitle(title);

            // ========== 链接 ==========
            String link = getElementValue(element, "link");
            if (link == null || link.isEmpty()) {
                NodeList linkNodes = element.getElementsByTagName("link");
                if (linkNodes.getLength() > 0) {
                    Element linkElement = (Element) linkNodes.item(0);
                    link = linkElement.getAttribute("href");
                }
            }
            article.setLink(link);

            // ========== 正文内容（关键部分）==========
            String fullContent = extractContent(element);

            // 如果没有正文，使用描述
            String description = getElementValue(element, "description");
            if (description == null || description.isEmpty()) {
                description = getElementValue(element, "summary");
            }
            article.setDescription(description);

            // 设置正文（优先完整内容）
            if (fullContent != null && !fullContent.isEmpty()) {
                article.setContent(fullContent);
            } else if (description != null && !description.isEmpty()) {
                article.setContent(description);
            } else {
                article.setContent("该 RSS 源未提供文章内容");
            }

            // ========== 作者 ==========
            String author = extractAuthor(element);
            article.setAuthor(author);

            // ========== 发布时间 ==========
            String pubDate = extractPublishDate(element);
            article.setPublished(pubDate);

            // 默认未读、未收藏
            article.setRead(false);
            article.setFavorited(false);
            article.setCreatedAt(String.valueOf(System.currentTimeMillis()));

            Log.d(TAG, "Parsed: " + title +
                ", Content length: " + (article.getContent() != null ? article.getContent().length() : 0));

            return article;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing item: " + e.getMessage());
            return null;
        }
    }

    /**
     * 提取完整内容 - 尝试多种标签
     */
    private static String extractContent(Element element) {
        // 1. content:encoded (WordPress, 最常见的完整内容)
        String content = getElementValueNS(element, "http://purl.org/rss/1.0/modules/content/", "encoded");
        if (content != null && !content.isEmpty()) {
            Log.d(TAG, "Found content:encoded");
            return content;
        }

        // 2. content:encoded (不带命名空间)
        content = getElementValue(element, "content:encoded");
        if (content != null && !content.isEmpty()) {
            Log.d(TAG, "Found content:encoded (no NS)");
            return content;
        }

        // 3. content 标签 (Atom)
        NodeList contentNodes = element.getElementsByTagName("content");
        if (contentNodes.getLength() > 0) {
            Element contentElement = (Element) contentNodes.item(0);
            String type = contentElement.getAttribute("type");
            content = contentElement.getTextContent();

            if (content != null && !content.isEmpty()) {
                Log.d(TAG, "Found content tag, type: " + type);
                return content;
            }
        }

        // 4. media:description
        content = getElementValueNS(element, "http://search.yahoo.com/mrss/", "description");
        if (content != null && !content.isEmpty()) {
            Log.d(TAG, "Found media:description");
            return content;
        }

        // 5. description (可能包含 HTML)
        content = getElementValue(element, "description");
        if (content != null && content.contains("<")) {
            // description 包含 HTML，可能是完整内容
            Log.d(TAG, "Found HTML in description");
            return content;
        }

        Log.d(TAG, "No full content found");
        return null;
    }

    /**
     * 提取作者信息
     */
    private static String extractAuthor(Element element) {
        // 1. author 标签
        String author = getElementValue(element, "author");
        if (author != null && !author.isEmpty()) {
            return author;
        }

        // 2. dc:creator
        author = getElementValue(element, "dc:creator");
        if (author != null && !author.isEmpty()) {
            return author;
        }

        // 3. Atom author/name
        NodeList authorNodes = element.getElementsByTagName("author");
        if (authorNodes.getLength() > 0) {
            Element authorElement = (Element) authorNodes.item(0);
            author = getElementValue(authorElement, "name");
            if (author != null && !author.isEmpty()) {
                return author;
            }
        }

        return null;
    }

    /**
     * 提取发布时间
     */
    private static String extractPublishDate(Element element) {
        // 1. pubDate (RSS 2.0)
        String date = getElementValue(element, "pubDate");
        if (date != null && !date.isEmpty()) {
            return date;
        }

        // 2. published (Atom)
        date = getElementValue(element, "published");
        if (date != null && !date.isEmpty()) {
            return date;
        }

        // 3. updated (Atom)
        date = getElementValue(element, "updated");
        if (date != null && !date.isEmpty()) {
            return date;
        }

        // 4. dc:date
        date = getElementValue(element, "dc:date");
        if (date != null && !date.isEmpty()) {
            return date;
        }

        return null;
    }

    /**
     * 获取元素值（普通标签）
     */
    private static String getElementValue(Element parent, String tagName) {
        try {
            NodeList nodeList = parent.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                if (node != null && node.getTextContent() != null) {
                    return node.getTextContent().trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting element value for " + tagName);
        }
        return null;
    }

    /**
     * 获取元素值（带命名空间）
     */
    private static String getElementValueNS(Element parent, String namespace, String localName) {
        try {
            NodeList nodeList = parent.getElementsByTagNameNS(namespace, localName);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                if (node != null && node.getTextContent() != null) {
                    return node.getTextContent().trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting element value for " + namespace + ":" + localName);
        }
        return null;
    }
}
