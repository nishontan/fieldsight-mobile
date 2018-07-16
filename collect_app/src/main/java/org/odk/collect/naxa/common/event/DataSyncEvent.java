package org.odk.collect.naxa.common.event;

import org.odk.collect.naxa.onboarding.DownloadProgress;

import static org.odk.collect.naxa.common.event.DataSyncEvent.EventStatus.EVENT_UPDATE;

/**
 * Created by nishon on 2/8/18.
 */

public class DataSyncEvent {

    private String event;
    private String status;
    private int uid;
    private DownloadProgress downloadProgress;


    public DataSyncEvent(int uid, String status) {
        this.uid = uid;
        this.status = status;
    }

    public DataSyncEvent(String event, String status) {
        this.event = event;
        this.status = status;
    }


    public DataSyncEvent(int uid, DownloadProgress downloadProgress) {
        this.status = EVENT_UPDATE;
        this.uid = uid;
        this.downloadProgress = downloadProgress;
    }

    public DownloadProgress getDownloadProgress() {
        return downloadProgress;
    }

    public String getEvent() {
        return event;
    }

    public String getStatus() {
        return status;
    }

    public int getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return "DataSyncEvent{" +
                "event='" + event + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public static final class EventStatus {
        public static final String EVENT_START = "start";
        public static final String EVENT_END = "end";
        public static final String EVENT_ERROR = "error";
        public static final String EVENT_UPDATE = "update";
    }

    public static final class EventType {
        public static final String GENERAL_FORM_DEPLOYED = "general_form_deployed";
        public static final String SCHEDULE_FORM_DEPLOYED = "schedule_form_deployed";
        public static final String STAGED_FORM_DEPLOYED = "staged_form_deployed";
        public static final String ME_API_HIT = "me_api_hit";
        public static final String ANY_DATA_SYNC = "any_api_hit";
    }

}
