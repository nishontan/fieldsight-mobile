package org.bcss.collect.naxa.common;

import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;

import com.evernote.android.job.DailyJob;
import com.evernote.android.job.JobManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.bcss.collect.android.R;
import org.bcss.collect.android.application.Collect;
import org.bcss.collect.android.listeners.DeleteFormsListener;
import org.bcss.collect.android.listeners.DeleteInstancesListener;
import org.bcss.collect.android.logic.PropertyManager;
import org.bcss.collect.android.provider.FormsProviderAPI;
import org.bcss.collect.android.provider.InstanceProviderAPI;
import org.bcss.collect.naxa.common.database.FieldSightConfigDatabase;
import org.bcss.collect.naxa.common.exception.FirebaseTokenException;
import org.bcss.collect.naxa.common.utilities.FlashBarUtils;
import org.bcss.collect.naxa.firebase.FCMParameter;
import org.bcss.collect.naxa.jobs.DailyNotificationJob;
import org.bcss.collect.naxa.login.LoginActivity;
import org.bcss.collect.naxa.login.model.Site;
import org.bcss.collect.naxa.login.model.User;
import org.bcss.collect.naxa.network.APIEndpoint;
import org.bcss.collect.naxa.network.ApiInterface;
import org.bcss.collect.naxa.network.ServiceGenerator;
import org.bcss.collect.naxa.site.db.SiteLocalSource;
import org.bcss.collect.naxa.sync.SyncRepository;
import org.odk.collect.android.activities.CollectAbstractActivity;
import org.odk.collect.android.dao.FormsDao;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.tasks.DeleteFormsTask;
import org.odk.collect.android.tasks.DeleteInstancesTask;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;
import timber.log.Timber;

public class FieldSightUserSession {

    public static void login() {

    }

    public static String getAuthToken() {
        return SharedPreferenceUtils.getFromPrefs(Collect.getInstance(), Constant.PrefKey.token, "");
    }

    public static void saveAuthToken(String token) {
        SharedPreferenceUtils.saveToPrefs(Collect.getInstance(), Constant.PrefKey.token, "Token " + token);
    }

    public static FCMParameter getFCMParameter(String username, String token, boolean deviceStatus) {
        String deviceId = new PropertyManager(Collect.getInstance()).getSingularProperty(PropertyManager.PROPMGR_DEVICE_ID);
        return new FCMParameter(deviceId, token, username, String.valueOf(deviceStatus));
    }

    public static MutableLiveData<String[]> getLogoutMessage() {
        MutableLiveData<String[]> message = new MutableLiveData<>();
        Context context = Collect.getInstance();
        message.setValue(new String[]{
                "",
                "",
                ""
        });


        SiteLocalSource.getInstance().getAllByStatus(Constant.SiteStatus.IS_OFFLINE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<Site>>() {
                    @Override
                    public void onSuccess(List<Site> sites) {
                        String msg;
                        int unsentFormCount = new InstancesDao().getUnsentInstancesCursor().getCount();
                        int offlineSitesNumber = sites.size();

                        if (offlineSitesNumber == 0 && unsentFormCount == 0) {
                            msg = context.getString(R.string.logout_message_none);
                        } else if (offlineSitesNumber == 0) {
                            msg = context.getString(R.string.logout_message_only_filled_forms, unsentFormCount);
                        } else if (unsentFormCount == 0) {
                            msg = context.getString(R.string.logout_message_only_offline_sites, offlineSitesNumber);
                        } else {
                            msg = context.getString(R.string.logout_message_all, offlineSitesNumber, unsentFormCount);
                        }


                        message.setValue(new String[]{
                                msg,
                                String.valueOf(unsentFormCount),
                                String.valueOf(offlineSitesNumber)
                        });
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });

        return message;
    }

    private static String getLogoutMessage(int offlineSitesNumber, int unsentFormCount) {


        String msg;
        Context context = Collect.getInstance();

        if (offlineSitesNumber == 0 && unsentFormCount == 0) {
            msg = context.getString(R.string.logout_message_none);
        } else if (offlineSitesNumber == 0) {
            msg = context.getString(R.string.logout_message_only_filled_forms, unsentFormCount);
        } else if (unsentFormCount == 0) {
            msg = context.getString(R.string.logout_message_only_offline_sites, offlineSitesNumber);
        } else {
            msg = context.getString(R.string.logout_message_all, offlineSitesNumber, unsentFormCount);
        }

        return msg;
    }


    static class DeleteFcm extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                FirebaseInstanceId.getInstance().deleteInstanceId();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static void showLogoutDialog(Activity context) {

        ((CollectAbstractActivity) context).showProgress();

        SiteLocalSource.getInstance()
                .getAllByStatus(Constant.SiteStatus.IS_OFFLINE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<Site>>() {
                    @Override
                    public void onSuccess(List<Site> sites) {
                        int unsentFormCount = new InstancesDao().getUnsentInstancesCursor().getCount();
                        int offlineSitesNumber = sites.size();

                        boolean isSafeToLogout = (unsentFormCount + offlineSitesNumber) == 0;
                        if (isSafeToLogout) {

                            logout(context, new OnLogoutListener() {
                                @Override
                                public void logoutTaskSuccess() {
//                                    tokenDeleteThread.start();
                                    new DeleteFcm().execute();
                                    ((CollectAbstractActivity) context).hideProgress();
                                    Intent intent = new Intent(context, LoginActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    context.startActivity(intent);
                                }

                                @Override
                                public void logoutTaskFailed(String message) {
                                    Timber.e(message);
                                    ((CollectAbstractActivity) context).hideProgress();
                                    FlashBarUtils.showFlashbar(context, "Logout failed ");
                                }

                                @Override
                                public void taskComplete() {
                                    ((CollectAbstractActivity) context).hideProgress();
                                }
                            });
                        } else {
                            ((CollectAbstractActivity) context).hideProgress();
                            DialogFactory.createMessageDialog(context,
                                    context.getString(R.string.msg_stop_logout),
                                    getLogoutMessage(offlineSitesNumber, unsentFormCount))
                                    .show();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                        ((CollectAbstractActivity) context).hideProgress();
                        DialogFactory.createMessageDialog(context,
                                context.getString(R.string.msg_stop_logout),
                                context.getString(R.string.dialog_unexpected_error_title))
                                .show();
                    }
                });


    }

    static Thread tokenDeleteThread = new Thread(() -> {
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException e) {
            e.printStackTrace();
        }
    });


    public static void stopLogoutDialog(Context context) {

        DialogFactory.createMessageDialog(context, "Can't logout", "An active internet connection required").show();
    }


    private static void removeFormsAndInstances(Context context, DeleteFormsListener listener) {
        DeleteInstancesTask deleteInstancesTask = new DeleteInstancesTask();
        deleteInstancesTask.setContentResolver(context.getContentResolver());
        deleteInstancesTask.setDeleteListener(new DeleteInstancesListener() {
            @Override
            public void deleteComplete(int deletedInstances) {
                DeleteFormsTask deleteFormsTask = new DeleteFormsTask();
                deleteFormsTask.setContentResolver(context.getContentResolver());
                deleteFormsTask.setDeleteListener(listener);
                deleteFormsTask.execute(getAllFormsIds());
            }

            @Override
            public void progressUpdate(int progress, int total) {
                Timber.i("Deleting %s out of %s instances", progress, total);
            }
        });


        deleteInstancesTask.execute(getAllInstancedsIds());

    }


    public interface OnLogoutListener {
        void logoutTaskSuccess();

        void logoutTaskFailed(String message);

        void taskComplete();
    }

    private static void logout(Context context, OnLogoutListener logoutListener) {

        Completable purgeDatabase = Completable.fromAction(() -> {
            FieldSightDatabase.getDatabase(context).clearAllTables();
            FieldSightConfigDatabase.getDatabase(context).clearAllTables();
        });


        Completable purgeSharedPref = Completable.fromAction(new Action() {
            @Override
            public void run() {
                SharedPreferenceUtils.deleteAll(context);
            }
        });

        try {
            JobManager.instance().cancelAll();
        } catch (Exception e) {
            Timber.e(e);
        }


        Observable.concat(purgeSharedPref.toObservable(), purgeDatabase.toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Object>() {
                    @Override
                    public void onNext(Object voidResponse) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                        logoutListener.logoutTaskFailed(e.getMessage());
                        logoutListener.taskComplete();
                    }

                    @Override
                    public void onComplete() {
                        removeFormsAndInstances(context, deletedForms -> {
                            ServiceGenerator.clearInstance();
                            SyncRepository.instance = null;
                            logoutListener.logoutTaskSuccess();
                            logoutListener.taskComplete();
                        });
                    }
                });


    }

    public static void setUser(User user) {
        if (user != null) {
            String userString = GSONInstance.getInstance().toJson(user);
            SharedPreferenceUtils.saveToPrefs(Collect.getInstance(), SharedPreferenceUtils.PREF_KEY.USER, userString);
        }
    }


    @NonNull
    public static User getUser() throws IllegalArgumentException {
        String userString = getUserString();
        if (userString == null || userString.length() == 0) {
            throw new IllegalArgumentException("User information is missing from cache");
        }
        return GSONInstance.getInstance().fromJson(userString, User.class);
    }

    private static String getUserString() {
        return SharedPreferenceUtils.getFromPrefs(Collect.getInstance().getApplicationContext(), SharedPreferenceUtils.PREF_KEY.USER, "");
    }

    private static Long[] getAllFormsIds() {
        Cursor results = null;
        ArrayList<Long> ids = new ArrayList<>();
        try {
            results = new FormsDao().getFormsCursor();
            if (results.getCount() > 0) {
                results.moveToPosition(-1);
                while (results.moveToNext()) {
                    String id = results.getString(results
                            .getColumnIndex(FormsProviderAPI.FormsColumns._ID));

                    ids.add(Long.valueOf(id));
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();//should never happen
        } finally {
            if (results != null) {
                results.close();
            }
        }

        Long[] longs = new Long[ids.size()];
        return ids.toArray(longs);
    }

    private static Long[] getAllInstancedsIds() {
        Cursor results = null;
        ArrayList<Long> ids = new ArrayList<>();
        try {
            results = new InstancesDao().getInstanceCursor();
            if (results.getCount() > 0) {
                results.moveToPosition(-1);
                while (results.moveToNext()) {
                    String id = results.getString(results
                            .getColumnIndex(InstanceProviderAPI.InstanceColumns._ID));

                    ids.add(Long.valueOf(id));
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();//should never happen
        } finally {
            if (results != null) {
                results.close();
            }
        }

        Long[] longs = new Long[ids.size()];
        return ids.toArray(longs);
    }


    public static boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && token.trim().length() > 0;
    }

    public static String getServerUrl(Context context) {
        return SharedPreferenceUtils.getFromPrefs(context, Constant.KEY_BASE_URL, APIEndpoint.BASE_URL);
    }

    public static void setServerUrl(Context context, String newUrl) {
        SharedPreferenceUtils.saveToPrefs(context, Constant.KEY_BASE_URL, newUrl);
    }
}

