package com.example.rssreader.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface FeedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FeedEntity feed);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FeedEntity> feeds);

    @Update
    void update(FeedEntity feed);

    @Delete
    void delete(FeedEntity feed);

    @Query("SELECT * FROM feeds ORDER BY title ASC")
    List<FeedEntity> getAllFeeds();

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    FeedEntity getFeedByUrl(String url);

    @Query("SELECT * FROM feeds WHERE serverId = :serverId LIMIT 1")
    FeedEntity getFeedByServerId(int serverId);

    @Query("DELETE FROM feeds WHERE url = :url")
    void deleteByUrl(String url);

    @Query("DELETE FROM feeds")
    void deleteAll();
}
