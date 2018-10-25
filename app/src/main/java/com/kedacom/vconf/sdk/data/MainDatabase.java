package com.kedacom.vconf.sdk.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Member.class}, version = 1)
public abstract class MainDatabase extends RoomDatabase {
    private static MainDatabase instance;
    public abstract MembersDao membersDao();

    public static synchronized MainDatabase getInstance(Context context){
        if (null == instance){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    MainDatabase.class, "main.db")
                    .build();
        }

        return instance;
    }
}
