package org.fieldsight.naxa.v3.project;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.bcss.collect.android.BuildConfig;
import org.bcss.collect.android.R;
import org.fieldsight.naxa.BackupActivity;
import org.fieldsight.naxa.common.Constant;
import org.fieldsight.naxa.common.DisposableManager;
import org.fieldsight.naxa.common.FieldSightUserSession;
import org.fieldsight.naxa.common.utilities.SnackBarUtils;
import org.fieldsight.naxa.helpers.FSInstancesDao;
import org.fieldsight.naxa.login.LoginActivity;
import org.fieldsight.naxa.login.model.Project;
import org.fieldsight.naxa.network.NetworkUtils;
import org.fieldsight.naxa.notificationslist.NotificationListActivity;
import org.fieldsight.naxa.preferences.SettingsActivity;
import org.fieldsight.naxa.project.ProjectListActivity;
import org.fieldsight.naxa.project.data.ProjectRepository;
import org.fieldsight.naxa.report.ReportActivity;
import org.fieldsight.naxa.v3.adapter.ProjectListAdapter;
import org.fieldsight.naxa.v3.network.LoadProjectCallback;
import org.fieldsight.naxa.v3.network.ProjectNameTuple;
import org.fieldsight.naxa.v3.network.SyncLocalSource3;
import org.fieldsight.naxa.v3.network.SyncServiceV3;
import org.fieldsight.naxa.v3.network.SyncStat;
import org.fieldsight.naxa.v3.network.Syncable;
import org.odk.collect.android.activities.CollectAbstractActivity;
import org.odk.collect.android.dto.Instance;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.utilities.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static org.fieldsight.naxa.common.Constant.FormDeploymentFrom.PROJECT;
import static org.odk.collect.android.application.Collect.allowClick;

public class ProjectListActivityV3 extends CollectAbstractActivity implements SyncingProjectAdapter.Callback {
    @BindView(R.id.rv_projectlist)
    RecyclerView rvProjectlist;

    @BindView(R.id.ll_nodata)
    LinearLayout llNodata;

    @BindView(R.id.tv_nodata)
    TextView tvNodata;

    @BindView(R.id.prgbar)
    ProgressBar prgbar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.tv_sync_project)
    TextView tvSyncProject;

    @BindView(R.id.cv_resync)
    CardView cvResync;

    @BindView(R.id.rv_projectlist_syncing)
    RecyclerView rvSyncing;

//    @BindView(R.id.tv_sync)
//    TextView tvSync;

    @BindView(R.id.tv_unsync)
    TextView tvUnsync;

    ProjectListAdapter unSyncedAdapter;


    RecyclerView.AdapterDataObserver observer;

//    @BindView(R.id.swipe_container)
//    SwipeRefreshLayout swipeRefreshLayout;

    boolean allSelected;
    LiveData<List<ProjectNameTuple>> projectIds;
    Observer<List<ProjectNameTuple>> projectObserver;
    boolean showSyncMenu = true;

    // flag to maintain the status of the syncing process is started or not
    boolean syncStarts = false;

    // Hashmap to track the syncing progress
    HashMap<String, List<Syncable>> syncableMap = new HashMap<>();

    // livedata for runnning live data observer
    LiveData<Integer> runningLiveData;

    // Observes the syncing or queued syncing project count
    Observer<Integer> runningLiveDataObserver;

    LiveData<List<SyncStat>> syncdata;
    Observer<List<SyncStat>> syncObserver;
    SyncingProjectAdapter syncAdapter;
    ArrayList<Project> toSyncList;

    // for syncing
    ArrayList<Project> syncProjectList = new ArrayList<>();
    // unsynced array list i.e. yet to sync
    List<Project> unSyncedprojectList = new ArrayList<>();
    Intent syncIntent;
    @BindView(R.id.nested_scroll_view)
    NestedScrollView nestedScrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_simple_recycler_with_nodata);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        setTitle("Projects");

        // adapter to manage the synced projects
        unSyncedAdapter = new ProjectListAdapter(unSyncedprojectList);
        // adapter to manage the syncing and synced projects
        syncAdapter = new SyncingProjectAdapter(syncProjectList, this);

        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
                super.onItemRangeChanged(positionStart, itemCount, payload);
                int selectedNum;
                for (selectedNum = 0; selectedNum < unSyncedprojectList.size(); selectedNum++) {
                    if (unSyncedprojectList.get(selectedNum).isChecked()) {
                        break;
                    }
                }
                Timber.d("project list counter is %d", selectedNum);
                // TODO check here, for tv sync not showing again
                if (selectedNum == unSyncedprojectList.size()) {
                    tvSyncProject.setVisibility(View.GONE);
                    allSelected = false;
//                    tvSync.setVisibility(View.GONE);
                    tvUnsync.setVisibility(View.GONE);
//                    invalidateOptionsMenu();
                } else if (!syncStarts) {
                    tvSyncProject.setVisibility(View.VISIBLE);
                    tvSyncProject.setBackgroundColor(getResources().getColor(R.color.secondaryColor));
                    tvSyncProject.setText("Sync Now");
                    tvUnsync.setVisibility(View.VISIBLE);
                }
            }
        };


        rvSyncing.setLayoutManager(new LinearLayoutManager(this));
        rvSyncing.setAdapter(syncAdapter);

        unSyncedAdapter.registerAdapterDataObserver(observer);
        rvProjectlist.setLayoutManager(new LinearLayoutManager(this));
        rvProjectlist.setAdapter(unSyncedAdapter);

        getDataFromServer();
        manageNodata(true);

//        tvSyncProject.setOnClickListener(v -> openDownloadAActivity());
        /**
         * regions and sites type = 0
         * forms type = 1
         * education materials type = 2
         */

        projectObserver = projectNameList -> {
            Timber.i("list live data = %d", projectNameList.size());

//            // check if project is completed or not
//            for(String key : syncableMap.keySet()) {
//                List<Syncable> syncableList = syncableMap.get(key);
//                boolean isSiteSynced = false, isFormSynced = false, isEducationMaterialSynced = false;
//                for(int i = 0; i < projectNameList.size(); i ++ ) {
//                   ProjectNameTuple projectNameTuple = projectNameList.get(i);
//                   if(!projectNameTuple.projectId.equals(key)) continue;
//                   // get the type from list
//
//                }
//            }

        };


        try {
            fixNullUrl();
        } catch (Exception e) {
            Timber.e(e);
        }


        syncObserver = new Observer<List<SyncStat>>() {
            @Override
            public void onChanged(List<SyncStat> syncStats) {
                Timber.i("sync stats size = %d", syncStats.size());
                // check if project is syncomplete or not
                // if sync complete, remove the downloading section from the item list
                // TODO check here how can we implement the form loading counter ??????????????????
                for (SyncStat stat : syncStats) {
                    String projectId = stat.getProjectId();
                    if (syncableMap.containsKey(projectId)) {
                        List<Syncable> syncableList = syncableMap.get(projectId);
                        Timber.i("ProjectListActivityv3, syncProjectlist size = %d", syncProjectList.size());
                        Syncable mSyncable = syncableList.get(Integer.parseInt(stat.getType()));
                        mSyncable.setStatus(stat.getStatus());
                        mSyncable.setProgress(stat.getProgress());
                        mSyncable.setTotal(stat.getTotal());
                        mSyncable.setCreatedDate(stat.getCreated_date());
                        syncableList.set(Integer.parseInt(stat.getType()), mSyncable);
                        syncableMap.put(projectId, syncableList);
                    }
                }
                syncAdapter.updateSyncMap(syncableMap);
            }
        };

        runningLiveDataObserver = count -> {
            Timber.i("SyncActivity ===============>>>>> syncing::  count = %d", count);
            if (count == 0) {
                Timber.i("SyncActivity ===============>>> enable called");

                // change all unsynced to synced
                for (int i = 0; i < syncProjectList.size(); i++) {
                    if (!syncProjectList.get(i).isSynced()) {
                        syncProjectList.get(i).setSynced(true);
                    }
                }
                syncAdapter.notifyDataSetChanged();
                unSyncedAdapter.disableAdapter(false);

                syncStarts = false;
                invalidateOptionsMenu();

                Timber.i("SyncAdapter ===============>>> remaining unsynced = %d", syncAdapter.getUnsyncedProject().size());

            }else {
                syncStarts = true;
            }
        };

        runningLiveData = SyncLocalSource3.getInstance().getCountByStatus(Constant.DownloadStatus.RUNNING, Constant.DownloadStatus.QUEUED);
        runningLiveData.observe(this, runningLiveDataObserver);
//        if (syncing) {
//            enableDisableAdapter(syncing);
//        }
    }

    // this class will manage the sync list to determine which should be synced
    private ArrayList<Syncable> createList() {
        // -1 refers here as never started
        return new ArrayList<Syncable>() {{
            add(0, new Syncable("Regions and sites", -1, 0, 0));
            add(1, new Syncable("Forms", -1, 0, 0));
            add(2, new Syncable("Materials", -1, 0, 0));
        }};
    }

    private void updateSyncableMap(List<Project> selectedProjectList) {
        if (syncableMap.size() > 0) syncableMap.clear();
        for (Project project : selectedProjectList) {
            syncableMap.put(project.getId(), createList());
        }
    }

    private ArrayList<String> getSelectedProjectIds(ArrayList<Project> selectedProjectList) {
        ArrayList<String> idList = new ArrayList<>();
        for (int i = 0; i < selectedProjectList.size(); i++) {
            idList.add(selectedProjectList.get(i).getId());
        }
        return idList;
    }

    private void startSyncing(ArrayList<Project> selectedProjectList) {

        ToastUtils.showLongToast("Data starts syncing");
        updateSyncableMap(selectedProjectList);

        Timber.i("ProjectListtActivityv3, syncable map = " + syncableMap.toString());

//        // clear synstat table before starting
//        String[] projectIds = new String[selectedProjectList.size()];
//        for(int i = 0; i < selectedProjectList.size(); i++) {
//            projectIds[i] = selectedProjectList.get(i).getId();
//        }
//        SyncLocalSource3.getInstance().deleteByIds(projectIds);

        syncIntent = new Intent(getApplicationContext(), SyncServiceV3.class);
        syncIntent.putStringArrayListExtra("projects", getSelectedProjectIds(selectedProjectList));
        syncIntent.putExtra("selection", syncableMap);
        startService(syncIntent);
        unSyncedAdapter.disableAdapter(true);
        syncStarts = true;
        invalidateOptionsMenu();
    }

    @OnClick(R.id.tv_sync_project)
    void addInSyncList() {
        if (NetworkUtils.isNetworkConnected()) {
            ArrayList<Project> toSyncList = manageSyncList();
            this.syncProjectList.addAll(0, toSyncList);
            syncAdapter.notifyDataSetChanged();
            tvUnsync.setVisibility(View.VISIBLE);
            startSyncing(toSyncList);
            // hide sync button when sync started
            tvSyncProject.setVisibility(View.GONE);
            nestedScrollView.scrollTo(0,0);
        } else {
            syncStarts = false;
            SnackBarUtils.showErrorFlashbar(this, getString(R.string.no_internet_body));
        }
    }

    private void fixNullUrl() throws Exception {
        FSInstancesDao instancesDao = new FSInstancesDao();
        List<Instance> instances = instancesDao.getBySiteId("");
        for (Instance instance : instances) {
            instance.setFieldSightSiteId("0");
            String[] path = instance.getSubmissionUri().split("/");
            String lastItem = path[path.length - 1];
            String fsFormId = path[path.length - 2];
            if (TextUtils.equals("null", lastItem)) {
                String where = InstanceProviderAPI.InstanceColumns.SUBMISSION_URI + "=?";

                String[] whereArgs = {instance.getSubmissionUri()};

                String fixedUrl = FSInstancesDao.generateSubmissionUrl(PROJECT, "0", fsFormId);

                ContentValues contentValues = new ContentValues();
                contentValues.put(InstanceProviderAPI.InstanceColumns.FS_SITE_ID, "0");
                contentValues.put(InstanceProviderAPI.InstanceColumns.SUBMISSION_URI, fixedUrl);
                instancesDao.updateInstance(contentValues, where, whereArgs);
                Timber.e("Fixed %s to %s", instance.getSubmissionUri(), fixedUrl);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.i("ProjectListActivityv3 :: anyProject checked = " + unSyncedAdapter.anyProjectSelectedForSync());
        if (tvSyncProject.getVisibility() == View.VISIBLE && !unSyncedAdapter.anyProjectSelectedForSync()) {
            tvSyncProject.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.cv_resync)
    void resyncProject() {
        if (NetworkUtils.isNetworkConnected()) {
            getDataFromServer();
            manageNodata(true);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_internet_body), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (observer != null) {
            unSyncedAdapter.unregisterAdapterDataObserver(observer);
        }
        if (projectIds != null && projectIds.hasObservers() && projectObserver != null) {
            projectIds.removeObserver(projectObserver);
        }

        // close all sync listening observers from live data
        if (syncdata != null && syncdata.hasObservers()) {
            syncdata.removeObserver(syncObserver);
        }
        if (runningLiveData != null && runningLiveData.hasObservers()) {
            runningLiveData.removeObserver(runningLiveDataObserver);
        }

    }

    void manageNodata(boolean loading) {
        if (syncAdapter.getItemCount() == 0 && unSyncedAdapter.getItemCount() == 0) {
            llNodata.setVisibility(View.VISIBLE);
            cvResync.setVisibility(loading ? View.GONE : View.VISIBLE);
        } else {
            llNodata.setVisibility(View.GONE);
        }
        prgbar.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvNodata.setText(loading ? "Loading projects ... " : "Error in syncing the project");
    }

    void refreshSyncStatus() {
        List<SyncStat> mSyncStatList = SyncLocalSource3.getInstance().getAllList();
        for (SyncStat stat : mSyncStatList) {
            String title = stat.getType().equals("0") ? "Sites and Regions" : stat.getType().equals("1") ? " forms" : "Education Materials";
            Syncable syncable = new Syncable(title, stat.getStatus());
            if (syncableMap.containsKey(stat.getProjectId())) {
                List<Syncable> syncableList = syncableMap.get(stat.getProjectId());
                syncableList.add(syncable);
                syncableMap.put(stat.getProjectId(), syncableList);
            } else {
                List<Syncable> mSyncableList = new ArrayList<>();
                mSyncableList.add(syncable);
                syncableMap.put(stat.getProjectId(), mSyncableList);
            }
        }
        syncAdapter.updateSyncMap(syncableMap);
        if (syncAdapter.getItemCount() > 0) {
            tvUnsync.setVisibility(View.VISIBLE);
        }

        // observer the syncing or sync progress
        syncdata = SyncLocalSource3.getInstance().getAll();
        syncdata.observe(this, syncObserver);

//        projectIds.observe(ProjectListActivityV3.this, projectObserver);
    }

    void getDataFromServer() {
        ProjectRepository.getInstance().getAll(new LoadProjectCallback() {
            @Override
            public void onProjectLoaded(List<Project> mProjectList, boolean fromOnline) {
                manageNodata(false);
                /** seprate sync and unsync data
                 check in syncstat table to findout which projects are synced or scheduled for sync already
                 separate yet to sync projects and populate syncing and yet to sync in different adapter
                 **/

                /**
                 *  get the project ids from sync stat table
                 *  Check if project ids is empty or not
                 *  if projectids is empty , none of the project are scheduled for the syncing
                 *
                 */
                // remove all cancelled projects
//                SyncLocalSource3.getInstance().removeCancelledProject();
                // update the value with syncstat
                List<SyncStat> mSyncStatList = SyncLocalSource3.getInstance().getAllList();

                HashMap<String, Integer> projectSyncalbeCount = new HashMap<>();

                // calculate the total count for each project id
                for (SyncStat mStat : mSyncStatList) {
                    if (projectSyncalbeCount.containsKey(mStat.getProjectId())) {
                        int count = projectSyncalbeCount.get(mStat.getProjectId());
                        projectSyncalbeCount.put(mStat.getProjectId(), count + 1);
                    } else {
                        projectSyncalbeCount.put(mStat.getProjectId(), 1);
                    }
                }
                Timber.i("ProjectListActivityv3, sync stat by project = %s", projectSyncalbeCount.toString());
                // clean syncstat , i.e. failed case, cancelled case

                // get project ids which list size is less than 3


                List<String> unCompleteProjects = new ArrayList<>();
                for (String key : projectSyncalbeCount.keySet()) {
                    if (projectSyncalbeCount.get(key) < 3) {
                        unCompleteProjects.add(key);
                    }
                }
                Timber.i("ProjectListActivityv3, uncomplete project size = %d", unCompleteProjects.size());

                // delete the uncomplete project list
                String[] idsArray = unCompleteProjects.toArray(new String[unCompleteProjects.size()]);
                SyncLocalSource3.getInstance().deleteByIds(idsArray);

                Timber.i("getDataFromServer :: ===========>>>>>>>> sync project ids = %d ", mSyncStatList.size());

                // get syncstat list again
                mSyncStatList = SyncLocalSource3.getInstance().getAllList();

                if (mSyncStatList.size() == 0) {
                    unSyncedprojectList.addAll(mProjectList);
                } else {
                    // separate the list
                    for (int i = 0; i < mProjectList.size(); i++) {
                        int j;
                        boolean found = false;
                        for (j = 0; j < mSyncStatList.size(); j++) {
                            if (mProjectList.get(i).getId().equals(mSyncStatList.get(j).getProjectId())) {
                                found = true;
                                break;
                            }
                        }

                        Timber.i("getDataFromServer :: ========>>>>>> found = " + found);
                        if (found) {
                            syncProjectList.add(mProjectList.get(i));
                        } else {
                            unSyncedprojectList.add(mProjectList.get(i));
                        }
                    }
                }

                // check if the project is synced or not if the project is from online
                Timber.i(" getDataFromServer :: ===========>>>>>> syncProjectList Size = %d, unSyncProjectList size = %d", unSyncedprojectList.size(), syncProjectList.size());
                unSyncedAdapter.notifyDataSetChanged();
                if (syncProjectList.size() > 0) {
                    syncAdapter.notifyDataSetChanged();
                }

                refreshSyncStatus();

                manageNodata(false);
            }

            @Override
            public void onDataNotAvailable() {
                Timber.d("data not available");
                manageNodata(false);
            }
        });
    }

    //    Clear the sync PROJECT list and add the selected projects
    ArrayList<Project> manageSyncList() {
        ArrayList<Project> checkedProjectList = new ArrayList<>();
        ArrayList<Project> unCheckedProjectList = new ArrayList<>();

        for (int i = 0; i < unSyncedprojectList.size(); i++) {
            Project project = unSyncedprojectList.get(i);
            if (project.isChecked()) {
                checkedProjectList.add(project);
            } else {
                unCheckedProjectList.add(project);
            }
        }

        this.unSyncedprojectList.clear();
        this.unSyncedprojectList.addAll(unCheckedProjectList);
        unSyncedAdapter.notifyDataSetChanged();

        Timber.i("manageSyncList ==========>>>>>>>> checkedProjectList size = %d ", checkedProjectList.size());
        return checkedProjectList;
    }

//    void openDownloadAActivity() {
    // changing the list as syncing and unsyncing

//        ArrayList<Project> syncProjectList = manageSyncList();
//        if (syncProjectList.size() > 0) {
//            Intent intent = new Intent(this, SyncActivity.class);
//            Bundle bundle = new Bundle();
//            bundle.putParcelableArrayList("projects", syncProjectList);
//            bundle.putBoolean("auto", true);
//            intent.putExtra("params", bundle);
//            startActivity(intent);
//        } else {
//            ToastUtils.showShortToastInMiddle("Please select at least one projects");
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu_fieldsight, menu);
        menu.findItem(R.id.action_refresh).getIcon().setColorFilter(getResources().getColor(R.color.primaryColor), PorterDuff.Mode.SRC_IN);
        menu.findItem(R.id.action_notificaiton).getIcon().setColorFilter(getResources().getColor(R.color.primaryColor), PorterDuff.Mode.SRC_IN);

        if (!BuildConfig.BUILD_TYPE.equals("release")) {
            menu.findItem(R.id.action_server_change).setVisible(true);
            menu.findItem(R.id.action_server_change).getIcon().setColorFilter(getResources().getColor(R.color.primaryColor), PorterDuff.Mode.SRC_IN);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        menu.findItem(R.id.action_refresh).setVisible(showSyncMenu);
        if (syncStarts) {
            menu.findItem(R.id.action_refresh).setVisible(true);
        }else {
            menu.findItem(R.id.action_refresh).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_backup:
                startActivity(new Intent(this, BackupActivity.class));
                return true;
            case R.id.action_refresh:
//                check all the PROJECT and make auto true
//                allSelected = !allSelected;
//                for (Project project : projectList) {
////                    if (!PROJECT.isSynced()) {
//                    project.setChecked(allSelected);
////                    } else {
////                        PROJECT.setChecked(false);
////                    }
//                }
//                adapter.toggleAllSelected(allSelected);
//                adapter.notifyDataSetChanged();
                if (syncStarts) {
                    cancelAllSync();
                } else {
                    ToastUtils.showLongToast("There is no any pending project syncing in queue to cancel");
                }
                break;
            case R.id.action_notificaiton:
                NotificationListActivity.start(this);
                break;
                case R.id.action_server_change:
                    if (allowClick(getClass().getName())) {
                        startActivity(new Intent(ProjectListActivityV3.this, org.fieldsight.naxa.common.SettingsActivity.class));
                    }
                break;
            case R.id.action_logout:
                FieldSightUserSession.showLogoutDialog(this);
                break;
            case R.id.action_setting:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_submit_report:
                startActivity(new Intent(this, ReportActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    boolean exit;

    @Override
    public void finish() {
        if (allSelected) {
//            allSelected = false;
//            for (Project project : projectList) {
//                project.setChecked(allSelected);
//            }
////            adapter.toggleAllSelected(allSelected);
//            adapter.notifyDataSetChanged();
//            invalidateOptionsMenu();
        } else {
            // exit the app in double back pressed
            if (exit) {
                super.finish();
            } else {
                Toast.makeText(getApplicationContext(), "Please double tap to exit", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exit = false;
                    }
                }, 2000);
                exit = true;
            }
        }
    }

    private void cancelAllSync() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel sync process");
        builder.setMessage("Project is syncing, Canceling the syncing will stop syncing this project.")
                .setPositiveButton("Cancel Sync", (dialog, which) -> {
                    if (syncIntent != null) {
                        DisposableManager.dispose();
                        stopService(syncIntent);
                        Timber.i("Service closed down");

                        // delete project form syncstat table which is not completed
                        List<String> syncingIds = new ArrayList<>();
                        for (String key : syncableMap.keySet()) {
                            boolean sitesSynced = syncableMap.get(key).get(0).getStatus() == Constant.DownloadStatus.COMPLETED;
                            boolean formsSynnced = syncableMap.get(key).get(1).getStatus() == Constant.DownloadStatus.COMPLETED;
                            boolean educationMaterialSynced = syncableMap.get(key).get(2).getStatus() == Constant.DownloadStatus.COMPLETED;
                            Timber.i(" ProjectListActivityv3, projectId = " + key + " sitesSynced = " + sitesSynced + " formsSynced = " + formsSynnced + " educationMaterialSynced = " + educationMaterialSynced);
                            if (sitesSynced && formsSynnced && educationMaterialSynced) continue;
                            syncingIds.add(key);
                        }
                        Timber.i("syncing key size = %d", syncingIds.size());
                        if (syncingIds.size() > 0) {
                            String[] ids = syncingIds.toArray(new String[syncingIds.size()]);
                            SyncLocalSource3.getInstance().deleteByIds(ids);
                            List<Project> projectList = syncAdapter.popItemByIds(ids);
                            unSyncedAdapter.push(0, projectList.toArray(new Project[projectList.size()]));
                        }
                    }
                    Timber.i("cancel clicked");
                    ToastUtils.showLongToast("Project sync cancelled by user");
                }).setNegativeButton("Close", null).create().show();
    }

    @Override
    public void syncedProjectClicked(Project project) {
        ProjectDashboardActivity.start(this, project);
    }

    @Override
    public void onCancelClicked(int pos) {
//        Project project = ((SyncingProjectAdapter) rvSyncing.getAdapter()).popItem(pos);
//        project.setChecked(false);
//        SyncLocalSource3.getInstance().setProjectCancelled(project.getId());
//        unSyncedAdapter.push(pos, project);
//        // remove this from sync stats
//        ToastUtils.showLongToast("Project sync cancelled by user");
    }

    @Override
    public void retryClicked(int pos) {
        Timber.i("retry clicked");
        if(!syncStarts) {
            if (NetworkUtils.isNetworkConnected()) {
                tvUnsync.setVisibility(View.VISIBLE);
                tvSyncProject.setVisibility(View.GONE);
                ArrayList<Project> retryList = ((SyncingProjectAdapter) rvSyncing.getAdapter()).getbyPosition(pos);
                startSyncing(retryList);
                // hide sync button when sync started
            } else {
                SnackBarUtils.showErrorFlashbar(this, getString(R.string.no_internet_body));
            }
        } else {
            ToastUtils.showLongToast("Please wait until sync completes");
        }
    }
}


