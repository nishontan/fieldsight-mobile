package org.fieldsight.naxa.v3;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import org.fieldsight.collect.android.R;
import org.fieldsight.naxa.BackupActivity;
import org.fieldsight.naxa.BaseActivity;
import org.fieldsight.naxa.FSInstanceChooserList;
import org.fieldsight.naxa.FSInstanceUploaderListActivity;
import org.fieldsight.naxa.common.FieldSightUserSession;
import org.fieldsight.naxa.common.ViewUtils;
import org.fieldsight.naxa.login.model.Project;
import org.fieldsight.naxa.login.model.User;
import org.fieldsight.naxa.network.NetworkUtils;
import org.fieldsight.naxa.notificationslist.NotificationListActivity;
import org.fieldsight.naxa.preferences.SettingsActivity;
import org.fieldsight.naxa.profile.UserActivity;
import org.fieldsight.naxa.project.TermsLabels;
import org.fieldsight.naxa.project.data.ProjectRepository;
import org.fieldsight.naxa.report.ReportActivity;
import org.fieldsight.naxa.site.CreateSiteActivity;
import org.fieldsight.naxa.site.ProjectDashboardActivity;
import org.fieldsight.naxa.v3.adapter.ProjectListAdapter;
import org.fieldsight.naxa.v3.network.LoadProjectCallback;
import org.fieldsight.naxa.v3.network.ProjectNameTuple;
import org.fieldsight.naxa.v3.network.SyncActivity;
import org.fieldsight.naxa.v3.network.SyncLocalSourcev3;
import org.odk.collect.android.activities.CollectAbstractActivity;
import org.odk.collect.android.activities.FileManagerTabs;
import org.odk.collect.android.utilities.ApplicationConstants;
import org.odk.collect.android.utilities.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ProjectListActivityV3 extends BaseActivity {
    @BindView(R.id.rv_projectlist)
    RecyclerView rv_projectlist;

    @BindView(R.id.ll_nodata)
    LinearLayout ll_nodata;

    @BindView(R.id.tv_nodata)
    TextView tv_nodata;

    @BindView(R.id.prgbar)
    ProgressBar prgbar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.tv_sync_project)
    TextView tv_sync_project;

    @BindView(R.id.cv_resync)
    CardView cv_resync;

    ProjectListAdapter adapter = null;
    List<Project> projectList = new ArrayList<>();
    boolean auto = false;
    RecyclerView.AdapterDataObserver observer;
    boolean allSelected = false;
    LiveData<List<ProjectNameTuple>> projectIds;
    Observer<List<ProjectNameTuple>> projectObserver = null;
    boolean showSyncMenu = true;


    @BindView(R.id.activity_dashboard_drawer_layout)
    public DrawerLayout drawerLayout;

    @BindView(R.id.activity_dashboard_navigation_view)
    public NavigationView navigationView;


    private FrameLayout navigationHeader;
    private ActionBarDrawerToggle drawerToggle;
    TermsLabels tl = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_simple_recycler_with_nodata);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        setTitle("Projects");
        adapter = new ProjectListAdapter(projectList, allSelected);
        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                int selected = 0;
                for (int i = 0; i < projectList.size(); i++) {
                    if (projectList.get(i).isChecked())
                        selected++;
                }
                Timber.d("project list counter is %d", selected);
                if (selected > 0) {
                    tv_sync_project.setVisibility(View.VISIBLE);
                    tv_sync_project.setBackgroundColor(getResources().getColor(R.color.secondaryColor));
                    tv_sync_project.setText(String.format(Locale.getDefault(), "Sync %d projects", selected));
                } else {
                    tv_sync_project.setVisibility(View.GONE);
                    allSelected = false;
                    invalidateOptionsMenu();
                }

            }
        };

        adapter.registerAdapterDataObserver(observer);
        rv_projectlist.setLayoutManager(new LinearLayoutManager(this));
        rv_projectlist.setAdapter(adapter);
        getDataFromServer();
        manageNodata(true);
        tv_sync_project.setOnClickListener(v -> openDownloadAActivity());
        projectObserver = projectNameList -> {
            Timber.i("list live data = %d", projectNameList.size());
            adapter.notifyProjectisSynced(projectNameList);
            showSyncMenu = projectNameList.size() == 0 || projectNameList.size() < adapter.getItemCount();
            invalidateOptionsMenu();
        };
        projectIds = SyncLocalSourcev3.getInstance().getAllSiteSyncingProject();
        setupNavigation();
        setupNavigationHeader();
    }


    @Override
    public void onBackClicked(boolean isHome) {
        toggleNavDrawer();
    }

    private void setupNavigationHeader() {
        try {
            User user = FieldSightUserSession.getUser();
            ((TextView) navigationHeader.findViewById(R.id.tv_user_name)).setText(user.getFullName());
            ((TextView) navigationHeader.findViewById(R.id.tv_email)).setText(user.getEmail());
            if (tl != null && !TextUtils.isEmpty(tl.site_supervisor)) {
                Timber.i("ProjectDashboardActivity, data:: sitesv = %s", tl.site_supervisor);
                ((TextView) navigationHeader.findViewById(R.id.tv_user_post)).setText(tl.site_supervisor);
            }

            ImageView ivProfilePicture = navigationHeader.findViewById(R.id.image_profile);

            ViewUtils.loadRemoteImage(this, user.getProfilepic())
                    .circleCrop()
                    .into(ivProfilePicture);


            navigationHeader.setOnClickListener(v -> {
                toggleNavDrawer();
                new Handler()
                        .postDelayed(() -> {
                            UserActivity.start(ProjectListActivityV3.this);
                        }, 250);
            });
        } catch (IllegalArgumentException e) {
            Timber.e(e);
        }
    }

    private void toggleNavDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }


    private void setupNavigation() {
        navigationHeader = (FrameLayout) navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                toggleNavDrawer();
                final int selectedItemId = item.getItemId();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handleNavDrawerClicks(selectedItemId);
                    }
                }, 250);


                return false;
            }
        });


        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(drawerToggle);
    }

    private void handleNavDrawerClicks(int id) {
        switch (id) {
            case R.id.nav_create_offline_site:

                break;
            case R.id.nav_delete_saved_form:

                startActivity(new Intent(getApplicationContext(), FileManagerTabs.class));
                break;
            case R.id.nav_edit_saved_form:

                Intent i = new Intent(getApplicationContext(), FSInstanceChooserList.class);
                i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE, ApplicationConstants.FormModes.EDIT_SAVED);
                startActivity(i);
                break;
            case R.id.nav_send_final_form:

                startActivity(new Intent(getApplicationContext(), FSInstanceUploaderListActivity.class));

                break;
            case R.id.nav_view_finalized_offline_site:

                break;
            case R.id.nav_view_site_dashboard:

                break;
            case R.id.nav_backup:
                startActivity(new Intent(this, BackupActivity.class));
                return;
            case R.id.nav_setting:
                startActivity(new Intent(this, org.fieldsight.naxa.preferences.SettingsActivity.class));
                break;
        }
    }


    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.i("ProjectListActivityv3 :: anyProject checked = %s", adapter.anyProjectSelectedForSync());
        if (tv_sync_project.getVisibility() == View.VISIBLE && !adapter.anyProjectSelectedForSync()) {
            tv_sync_project.setVisibility(View.GONE);
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
        if (observer != null)
            adapter.unregisterAdapterDataObserver(observer);
        if (projectIds != null && projectIds.hasObservers() && projectObserver != null)
            projectIds.removeObserver(projectObserver);
    }

    void manageNodata(boolean loading) {
        if (adapter.getItemCount() == 0) {
            ll_nodata.setVisibility(View.VISIBLE);
            cv_resync.setVisibility(loading ? View.GONE : View.VISIBLE);
        } else {
            ll_nodata.setVisibility(View.GONE);
        }
        prgbar.setVisibility(loading ? View.VISIBLE : View.GONE);
        tv_nodata.setText(loading ? "Loading data ... " : "Error in syncing the project");
    }

    void getDataFromServer() {
        ProjectRepository.getInstance().getAll(new LoadProjectCallback() {
            @Override
            public void onProjectLoaded(List<Project> projects) {
                projectList.addAll(projects);
                adapter.notifyDataSetChanged();
                manageNodata(false);
                Timber.e("data found with %d size", projects.size());
                projectIds.observe(ProjectListActivityV3.this, projectObserver);
            }

            @Override
            public void onDataNotAvailable() {
                Timber.d("data not available");
                manageNodata(false);
            }
        });


    }

    //    Clear the sync project list and add the selected projects
    ArrayList<Project> manageSyncList() {
        ArrayList<Project> syncProjectList = new ArrayList<>();
        for (Project project : projectList) {
            if (project.isChecked()) {
                syncProjectList.add(project);
            }
        }
        return syncProjectList;
    }

    void openDownloadAActivity() {
        ArrayList<Project> syncProjectList = manageSyncList();
        if (syncProjectList.size() > 0) {
            Intent intent = new Intent(this, SyncActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("projects", syncProjectList);
            bundle.putBoolean("auto", true);
            intent.putExtra("params", bundle);
            startActivity(intent);
        } else {
            ToastUtils.showShortToastInMiddle("Please select at least one projects");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu_fieldsight, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        menu.findItem(R.id.action_refresh).setVisible(showSyncMenu);
        if (showSyncMenu) {
            menu.findItem(R.id.action_refresh).setIcon(allSelected ?
                    R.drawable.ic_cancel_white_24dp :
                    R.drawable.ic_action_sync
            );
            menu.findItem(R.id.action_refresh).setTitle(allSelected ? "Cancel" : "sync");
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
//                check all the project and make auto true
                allSelected = !allSelected;
                for (Project project : projectList) {
//                    if (!project.isSynced()) {
                    project.setChecked(allSelected);
//                    } else {
//                        project.setChecked(false);
//                    }
                }
                adapter.toggleAllSelected(allSelected);
                adapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                break;
            case R.id.action_notificaiton:
                NotificationListActivity.start(this);
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


}


