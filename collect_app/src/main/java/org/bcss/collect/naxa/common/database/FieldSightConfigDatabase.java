package org.bcss.collect.naxa.common.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import org.bcss.collect.android.application.Collect;
import org.bcss.collect.naxa.sync.DownloadableItem;
import org.bcss.collect.naxa.sync.DownloadableItemDAO;

import java.io.File;

@Database(entities =
        {
                SiteOveride.class,
                ProjectFilter.class,
                DownloadableItem.class,
                SiteUploadHistory.class

        },
        version = 8)

public abstract class FieldSightConfigDatabase extends RoomDatabase {

    private static FieldSightConfigDatabase INSTANCE;

    public abstract SiteOverideDAO getSiteOverideDAO();

    public abstract ProjectFilterDAO getProjectFilterDAO();

    public abstract DownloadableItemDAO getSyncDao();

    public abstract SiteUploadHistoryDAO getSiteUploadHistoryDao();

    private static final String DB_PATH = Collect.METADATA_PATH + File.separator + "fieldsight_cofig";

    public static FieldSightConfigDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (FieldSightConfigDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            FieldSightConfigDatabase.class, DB_PATH)
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }


}
