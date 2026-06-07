package com.example.rssreader.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "articles", indices = {@Index(value = {"link"}, unique = true)})
public class ArticleEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int serverId;  // 服务器上的 ID
    private int feedId;
    private String title;
    private String link;
    private String description;
    private long published; // 毫秒时间戳，用于数字排序
    private String author;
    private String content;
    private boolean isFavorited;
    private boolean isRead;
    private String createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getServerId() { return serverId; }
    public void setServerId(int serverId) { this.serverId = serverId; }

    public int getFeedId() { return feedId; }
    public void setFeedId(int feedId) { this.feedId = feedId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getPublished() { return published; }
    public void setPublished(long published) { this.published = published; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isFavorited() { return isFavorited; }
    public void setFavorited(boolean favorited) { isFavorited = favorited; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
