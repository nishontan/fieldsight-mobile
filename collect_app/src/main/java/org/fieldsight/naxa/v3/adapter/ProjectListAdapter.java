package org.fieldsight.naxa.v3.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.bcss.collect.android.R;
import org.fieldsight.naxa.login.model.Project;
import org.fieldsight.naxa.v3.network.ProjectNameTuple;
import org.fieldsight.naxa.v3.project.ProjectDashboardActivity;

import java.util.ArrayList;
import java.util.List;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectViewHolder> {
    private final List<Project> projectList;
    boolean disable = false;
    public ProjectListAdapter(List<Project> projectList) {
        this.projectList = projectList;
    }

//    public void toggleAllSelected(boolean allSelected) {
//        this.allTrue = allSelected;
//    }

//    public void toggleAllSelectedAndNotify(boolean allSelected) {
//        this.allTrue = allSelected;
//        notifyDataSetChanged();
//    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ProjectViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.project_expand, viewGroup, false)) {
            @Override
            void checkBoxChanged(int index, boolean isChecked) {
                projectList.get(index).setChecked(isChecked);
                notifyDataSetChanged();
            }

            @Override
            void itemClicked(int index) {
                Project project = projectList.get(index);
//                if (project.isSynced()) {
//                    ProjectDashboardActivity.start(viewGroup.getContext(), projectList.get(index));
//                    return;
//                }
//                boolean isSyncRunning = Collect.selectedProjectList != null && Collect.selectedProjectList.size() > 0
//                        && Collect.syncableMap != null && Collect.syncableMap.size() > 0;
//                Timber.i("ProjectListAdapter, isSyncRunning : " + isSyncRunning);
//                if (isSyncRunning) {
//                    viewGroup.getContext().startActivity(new Intent(viewGroup.getContext(), SyncActivity.class));
//                    return;
//                } else {
                    project.setChecked(!project.isChecked());
                    notifyItemChanged(index);
//                }
            }
        };
    }

    public void notifyProjectisSynced(List<ProjectNameTuple> projecttuple) {
//        for (int i = 0; i < projectList.size(); i++) {
//            for (int j = 0; j < projecttuple.size(); j++) {
//                if (projectList.get(i).getId().equals(projecttuple.get(j).projectId)) {
//                    int status = projecttuple.get(j).status;
//                    if (status == Constant.DownloadStatus.RUNNING) {
//                        projectList.get(i).setStatusMessage("Syncing project");
//                    } else if (status == Constant.DownloadStatus.COMPLETED) {
//                        projectList.get(i).setSynced(true);
//                        projectList.get(i).setSyncedDate(projecttuple.get(j).createdDate);
//                        projectList.get(i).setStatusMessage("Synced On " + DateTimeUtils.getFormattedDate("yyyy-MM-dd, HH:mm", projectList.get(i).getSyncedDate()));
//                    } else if (status == Constant.DownloadStatus.FAILED) {
//                        projectList.get(i).setSynced(false);
//                        projectList.get(i).setStatusMessage("Sync failed");
//                    } else {
//                        projectList.get(i).setStatusMessage("");
//                    }
//                }
//            }
//        }
//        notifyDataSetChanged();
    }

    public void push(int index, Project... projects) {
        List<Project> mProjectList = new ArrayList<>();
        for(Project project : projects) {
            mProjectList.add(project);
        }
        this.projectList.addAll(index, mProjectList);
        notifyItemChanged(index);
    }


    public boolean anyProjectSelectedForSync() {
        boolean found = false;
        for (Project project : this.projectList) {
            if (!project.isSynced()) {
                found = true;
                break;
            }
        }
        return found;
    }

    public void clearAndUpdate(List<Project> projects) {
        projectList.clear();
        projectList.addAll(projects);
        notifyDataSetChanged();
    }


    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder projectViewHolder, int i) {
        projectViewHolder.bindView(projectList.get(i), disable);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public void disableAdapter(boolean disable) {
        this.disable = disable;
        notifyDataSetChanged();
    }
}
