package com.example.rssreader.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ArticleEntity.class, FeedEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ArticleDao articleDao();
    public abstract FeedDao feedDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "rss_reader_database"
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
}
