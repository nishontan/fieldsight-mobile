package org.fieldsight.naxa.preferences;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.evernote.android.job.JobManager;
import com.google.android.gms.analytics.HitBuilders;

import org.bcss.collect.android.BuildConfig;
import org.bcss.collect.android.R;
import org.fieldsight.naxa.common.FieldSightUserSession;
import org.fieldsight.naxa.jobs.DailyNotificationJob;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.AdminSharedPreferences;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.tasks.ServerPollingJob;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

import static org.fieldsight.naxa.preferences.SettingsKeys.KEY_APP_URL;
import static org.fieldsight.naxa.preferences.SettingsKeys.KEY_NOTIFICATION_TIME_DAILY;
import static org.fieldsight.naxa.preferences.SettingsKeys.KEY_NOTIFICATION_TIME_MONTHLY;
import static org.fieldsight.naxa.preferences.SettingsKeys.KEY_NOTIFICATION_TIME_WEEKLY;
import static org.odk.collect.android.preferences.AdminKeys.ALLOW_OTHER_WAYS_OF_EDITING_FORM;


public class ScheduledNotificationSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {


//    private CustomTimePickerDialog customTimePickerDialog;
    private final String[] weeks = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private final String[] months = {"Beginning of the month", "Middle of the month", "End of the month"};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setupSharedPreferences();
        setupNotificationToggle();
        setupUpdateButton();
//        setupTimePicker();
        initListPref(GeneralKeys.KEY_IMAGE_SIZE);

    }

    private void init() {
        addPreferencesFromResource(R.xml.fieldsight_preferences);
        findPreference(KEY_NOTIFICATION_TIME_DAILY).setOnPreferenceClickListener(this);
        findPreference(KEY_NOTIFICATION_TIME_WEEKLY).setOnPreferenceClickListener(this);
        findPreference(KEY_NOTIFICATION_TIME_MONTHLY).setOnPreferenceClickListener(this);

    }

    private void setupTimePicker() {
//        String[] dailyTime = ((String) SettingsSharedPreferences.getInstance().get(KEY_NOTIFICATION_TIME_DAILY)).split(":");
//        customTimePickerDialog = new CustomTimePickerDialog(getActivity(), (view, hourOfDay, minute) -> {
//            String time = String.format(Locale.getDefault(), "%d:%d", hourOfDay, minute);
//            SettingsSharedPreferences.getInstance().save(KEY_NOTIFICATION_TIME_DAILY, time);
//        }, Integer.valueOf(dailyTime[0]), Integer.valueOf(dailyTime[1]));
    }


    private void setupUpdateButton() {
        Preference preference = findPreference(KEY_APP_URL);
        preference.setOnPreferenceClickListener(this);
        String title = getString(R.string.app_name).concat(": ").concat(BuildConfig.VERSION_NAME);
        preference.setTitle(title);
        preference.setSummary(FieldSightUserSession.getServerUrl(Collect.getInstance()));
    }


    private void setupNotificationToggle() {
        SwitchPreference switchPreferenceDaily = (SwitchPreference) findPreference(SettingsKeys.KEY_NOTIFICATION_SWITCH_DAILY);
        SwitchPreference switchPreferenceWeek = (SwitchPreference) findPreference(SettingsKeys.KEY_NOTIFICATION_SWITCH_WEEKLY);
        SwitchPreference switchPreferenceMonth = (SwitchPreference) findPreference(SettingsKeys.KEY_NOTIFICATION_SWITCH_MONTHLY);

        switchPreferenceDaily.setSummaryOff(getString(R.string.msg_no_longer_notifcation_receiced));
        switchPreferenceWeek.setSummaryOff(getString(R.string.msg_no_longer_notifcation_receiced));
        switchPreferenceMonth.setSummaryOff(getString(R.string.msg_no_longer_notifcation_receiced));

        switchPreferenceDaily.setSummaryOn(getString(R.string.msg_will_receive_notifications));
        switchPreferenceWeek.setSummaryOn(getString(R.string.msg_will_receive_notifications));
        switchPreferenceMonth.setSummaryOn(getString(R.string.msg_will_receive_notifications));

    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case KEY_NOTIFICATION_TIME_DAILY:
//                customTimePickerDialog.show();
                break;
            case KEY_NOTIFICATION_TIME_WEEKLY:
                showWeekPickerDialog();
                break;
            case KEY_NOTIFICATION_TIME_MONTHLY:
                showMonthlyPickerDialog();
                break;
            case KEY_APP_URL:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(FieldSightUserSession.getServerUrl(Collect.getInstance()))));
                break;
        }
        return false;
    }

    private void showWeekPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.msg_choose_a_day);

        builder.setItems(weeks, (dialog, which) -> SettingsSharedPreferences.getInstance().save(KEY_NOTIFICATION_TIME_WEEKLY, which));
        builder.show();
    }

    private void showMonthlyPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.msg_choose_month_option);

        builder.setItems(months, (dialog, which) -> SettingsSharedPreferences.getInstance().save(KEY_NOTIFICATION_TIME_MONTHLY, which));
        builder.show();
    }

    private void setupSharedPreferences() {
        SettingsSharedPreferences.getInstance().register(this);

        String time = String.valueOf(SettingsSharedPreferences.getInstance().get(KEY_NOTIFICATION_TIME_DAILY));
        findPreference(KEY_NOTIFICATION_TIME_DAILY).setSummary(formatTime(time));

        int weekIndex = Integer.parseInt(String.valueOf(SettingsSharedPreferences.getInstance().get(KEY_NOTIFICATION_TIME_WEEKLY)));
        findPreference(KEY_NOTIFICATION_TIME_WEEKLY).setSummary(weeks[weekIndex]);

        int monthIndex = Integer.parseInt(String.valueOf(SettingsSharedPreferences.getInstance().get(KEY_NOTIFICATION_TIME_MONTHLY)));
        findPreference(KEY_NOTIFICATION_TIME_MONTHLY).setSummary(months[monthIndex]);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case KEY_NOTIFICATION_TIME_DAILY:
                String time = String.valueOf(SettingsSharedPreferences.getInstance().get(KEY_NOTIFICATION_TIME_DAILY));
                findPreference(KEY_NOTIFICATION_TIME_DAILY).setSummary(formatTime(time));
                break;
            case KEY_NOTIFICATION_TIME_WEEKLY:
                Integer weekIndex = (Integer) SettingsSharedPreferences.getInstance().get(KEY_NOTIFICATION_TIME_WEEKLY);
                findPreference(KEY_NOTIFICATION_TIME_WEEKLY).setSummary(weeks[weekIndex]);
                break;
            case KEY_NOTIFICATION_TIME_MONTHLY:
                Integer monthIndex = (Integer) SettingsSharedPreferences.getInstance().get(KEY_NOTIFICATION_TIME_MONTHLY);
                findPreference(KEY_NOTIFICATION_TIME_WEEKLY).setSummary(months[monthIndex]);
                break;
        }

        if (isOneItemActivated()) {
            DailyNotificationJob.schedule();
        } else {
            JobManager.instance().cancelAllForTag(DailyNotificationJob.TAG);
        }
    }

    private boolean isOneItemActivated() {
        boolean isDailyActivated = (boolean) SettingsSharedPreferences.getInstance().get(SettingsKeys.KEY_NOTIFICATION_SWITCH_DAILY);
        boolean isWeeklyActivated = (boolean) SettingsSharedPreferences.getInstance().get(SettingsKeys.KEY_NOTIFICATION_SWITCH_WEEKLY);
        boolean isMonthlyActivated = (boolean) SettingsSharedPreferences.getInstance().get(SettingsKeys.KEY_NOTIFICATION_SWITCH_MONTHLY);

        return isDailyActivated || isWeeklyActivated || isMonthlyActivated;

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        SettingsSharedPreferences.getInstance().unregister(this);
    }

    private String formatTime(String time) {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("H:mm", Locale.getDefault());
            final Date dateObj = sdf.parse(time);
            return new SimpleDateFormat("K:mm a", Locale.getDefault()).format(dateObj);
        } catch (ParseException e) {
            Timber.e(e);
        }

        return time;
    }

    private void initListPref(String key) {
        final ListPreference pref = (ListPreference) findPreference(key);

        if (pref != null) {
            pref.setSummary(pref.getEntry());
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                int index = ((ListPreference) preference).findIndexOfValue(newValue.toString());
                CharSequence entry = ((ListPreference) preference).getEntries()[index];
                preference.setSummary(entry);

                if (key.equals(GeneralKeys.KEY_PERIODIC_FORM_UPDATES_CHECK)) {
                    ServerPollingJob.schedulePeriodicJob((String) newValue);

                    Collect.getInstance().getDefaultTracker()
                            .send(new HitBuilders.EventBuilder()
                                    .setCategory("PreferenceChange")
                                    .setAction("Periodic form updates check")
                                    .setLabel((String) newValue)
                                    .build());

                    if (newValue.equals(getString(R.string.never_value))) {
                        Preference automaticUpdatePreference = findPreference(GeneralKeys.KEY_AUTOMATIC_UPDATE);
                        if (automaticUpdatePreference != null) {
                            automaticUpdatePreference.setEnabled(false);
                        }
                    }
                    getActivity().recreate();
                }
                return true;
            });
            if (key.equals(GeneralKeys.KEY_CONSTRAINT_BEHAVIOR)) {
                pref.setEnabled((Boolean) AdminSharedPreferences.getInstance().get(ALLOW_OTHER_WAYS_OF_EDITING_FORM));
            }
        }
    }

}