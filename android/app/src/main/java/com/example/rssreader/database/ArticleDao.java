package com.example.rssreader.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ArticleEntity article);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ArticleEntity> articles);

    @Update
    void update(ArticleEntity article);

    @Delete
    void delete(ArticleEntity article);

    @Query("SELECT * FROM articles ORDER BY published DESC")
    List<ArticleEntity> getAllArticles();

    @Query("SELECT * FROM articles WHERE isFavorited = 1 ORDER BY createdAt DESC")
    List<ArticleEntity> getFavoriteArticles();

    @Query("SELECT * FROM articles WHERE isRead = 0 ORDER BY published DESC")
    List<ArticleEntity> getUnreadArticles();

    @Query("SELECT * FROM articles WHERE feedId = :feedId ORDER BY published DESC")
    List<ArticleEntity> getArticlesByFeed(int feedId);

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY published DESC")
    List<ArticleEntity> searchArticles(String query);

    @Query("SELECT * FROM articles WHERE serverId = :serverId LIMIT 1")
    ArticleEntity getArticleByServerId(int serverId);

    @Query("SELECT * FROM articles WHERE link = :link LIMIT 1")
    ArticleEntity getArticleByLink(String link);

    @Query("UPDATE articles SET isFavorited = :isFavorited WHERE id = :id")
    void updateFavoriteStatus(int id, boolean isFavorited);

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :id")
    void updateReadStatus(int id, boolean isRead);

    @Query("DELETE FROM articles WHERE isFavorited = 0")
    void deleteNonFavorites();

    @Query("DELETE FROM articles")
    void deleteAll();
}
