package com.example.rssreader.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feeds")
public class FeedEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int serverId;  // 服务器上的 ID
    private String title;
    private String url;
    private String description;
    private String createdAt;
    private String updatedAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getServerId() { return serverId; }
    public void setServerId(int serverId) { this.serverId = serverId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
