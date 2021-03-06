package org.fieldsight.naxa.notificationslist;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;


import org.fieldsight.naxa.common.database.BaseDaoFieldSight;
import org.fieldsight.naxa.data.FieldSightNotification;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Maybe;


@Dao
public abstract class FieldSightNotificationDAO implements BaseDaoFieldSight<FieldSightNotification> {



    @Query("SELECT * FROM fieldsightnotification ORDER BY receivedDateTimeInMillis DESC")
    public abstract LiveData<List<FieldSightNotification>> getAll();


    @Query("DELETE FROM fieldsightnotification")
    public abstract void deleteAll();

    @Transaction
    public void updateAll(ArrayList<FieldSightNotification> items) {
        deleteAll();
        insert(items);
    }

    @Query("SELECT COUNT(notificationType) FROM fieldsightnotification " +
            "WHERE notificationType in (:strings) " +
            "and (siteId =:siteId or projectId =:projectId)  and isRead =:read ")
    public abstract LiveData<Integer> notificationCount(Boolean read, String siteId, String projectId, String... strings);

    @Query("SELECT COUNT(notificationType) FROM fieldsightnotification WHERE notificationType in (:notificationTypes) and isRead =:read ")
    public abstract Maybe<Integer> countForNotificationType(Boolean read, String... notificationTypes);


    @Query("UPDATE fieldsightnotification SET isRead=:read WHERE notificationType in (:notificationTypes)")
    public abstract void applyReadToNotificationType(Boolean read, String... notificationTypes);

    @Query("SELECT " +
            "(SELECT COUNT(DISTINCT projectId) FROM fieldsightnotification WHERE notificationType =:assign and isRead =:read ) " +
            " - " +
            "(SELECT COUNT(DISTINCT projectId) FROM fieldsightnotification WHERE notificationType =:assign AND projectId in(:projectIds) and isRead =:read )")
    public abstract LiveData<Integer> countNonExistentProjectInNotification(Boolean read, String assign, String... projectIds);


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertOrIgnore(FieldSightNotification... items);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertOrIgnore(ArrayList<FieldSightNotification> items);
        /*
    *
 SELECT ( SELECT count(DISTINCT projectId) FROM fieldsightnotification WHERE notificationType = "Assign Site") -
 ( SELECT count(DISTINCT projectId) FROM fieldsightnotification WHERE notificationType = "Assign Site" AND projectId in(183))
     */
}
