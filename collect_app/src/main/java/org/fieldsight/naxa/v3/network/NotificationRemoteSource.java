package org.fieldsight.naxa.v3.network;

import org.fieldsight.naxa.network.ServiceGenerator;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class NotificationRemoteSource {

    private static NotificationRemoteSource notificationRemoteSource;

    public synchronized static NotificationRemoteSource getInstance() {
        if (notificationRemoteSource == null) {
            notificationRemoteSource = new NotificationRemoteSource();
        }
        return notificationRemoteSource;
    }

    public Single<ResponseBody> getNotifications(String epochTime, String type) {
        return ServiceGenerator.getRxClient().create(ApiV3Interface.class)
                .getNotification(getHasMap(epochTime, type))
                .subscribeOn(Schedulers.io());
    }

    private Map<String, String> getHasMap(String epochTime, String type) {
        HashMap<String, String> requestParams = new HashMap<>();
        requestParams.put("last_updated", epochTime);
        requestParams.put("type", type);
        return requestParams;
    }
}