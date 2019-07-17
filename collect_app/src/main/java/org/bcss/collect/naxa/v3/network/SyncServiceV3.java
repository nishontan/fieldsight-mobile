package org.bcss.collect.naxa.v3.network;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.bcss.collect.android.logic.FormDetails;
import org.bcss.collect.naxa.common.Constant;
import org.bcss.collect.naxa.common.DisposableManager;
import org.bcss.collect.naxa.common.ODKFormRemoteSource;
import org.bcss.collect.naxa.common.rx.RetrofitException;
import org.bcss.collect.naxa.educational.EducationalMaterialsRemoteSource;
import org.bcss.collect.naxa.generalforms.data.GeneralForm;
import org.bcss.collect.naxa.generalforms.data.GeneralFormRemoteSource;
import org.bcss.collect.naxa.login.model.Project;
import org.bcss.collect.naxa.login.model.Site;
import org.bcss.collect.naxa.network.APIEndpoint;
import org.bcss.collect.naxa.scheduled.data.ScheduleForm;
import org.bcss.collect.naxa.scheduled.data.ScheduledFormsRemoteSource;
import org.bcss.collect.naxa.site.db.SiteLocalSource;
import org.bcss.collect.naxa.site.db.SiteRemoteSource;
import org.bcss.collect.naxa.stages.data.Stage;
import org.bcss.collect.naxa.stages.data.StageRemoteSource;
import org.odk.collect.android.activities.FormDownloadList;
import org.odk.collect.android.utilities.ApplicationConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.bcss.collect.naxa.ResponseUtils.isListOfType;

public class SyncServiceV3 extends IntentService {
    /***
     *
     *
     * @Author: Yubaraj Poudel
     * @Since : 14/05/2019
     */

    ArrayList<Project> selectedProject;
    HashMap<String, List<Syncable>> selectedMap = null;
    private List<String> failedSiteUrls = new ArrayList<>();
    private ArrayList<Disposable> syncDisposable = new ArrayList<>();

    public SyncServiceV3() {
        super("SyncserviceV3");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String sentAction = intent.getAction();
        if (TextUtils.equals(sentAction, Constant.SERVICE.STOP_SYNC)) {
            cancelAllTask();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            selectedProject = Objects.requireNonNull(intent).getParcelableArrayListExtra("projects");
            Timber.i("SyncServiceV3 slectedProject size = %d", selectedProject.size());

            selectedMap = (HashMap<String, List<Syncable>>) intent.getSerializableExtra("selection");
            for (String key : selectedMap.keySet()) {
                Timber.i(readaableSyncParams(key, selectedMap.get(key)));
            }

            //Start syncing sites
            Disposable sitesObservable = downloadByRegionObservable(selectedProject, selectedMap)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<Object>() {
                        @Override
                        public void onNext(Object o) {
                            //unused
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e);
                        }

                        @Override
                        public void onComplete() {
                            //unused
                        }
                    });

            DisposableManager.add(sitesObservable);

            Disposable projectEduMatObservable = Observable.just(selectedProject)
                    .flatMapIterable((Function<ArrayList<Project>, Iterable<Project>>) projects -> projects)
                    .filter(project -> selectedMap.get(project.getId()).get(2).sync)
                    .flatMap(new Function<Project, Observable<String>>() {
                        @Override
                        public Observable<String> apply(Project project) throws Exception {

                            return EducationalMaterialsRemoteSource.getInstance()
                                    .getByProjectId(project.getId())
                                    .toObservable()
                                    .doOnDispose(new Action() {
                                        @Override
                                        public void run() throws Exception {
                                            markAsFailed(project.getId(), 2, "");
                                        }
                                    })
                                    .doOnSubscribe(new Consumer<Disposable>() {
                                        @Override
                                        public void accept(Disposable disposable) throws Exception {
                                            markAsRunning(project.getId(), 2);
                                        }
                                    })
                                    .onErrorReturn(throwable -> {
                                        String url = getFailedFormUrl(throwable)[0];
                                        markAsFailed(project.getId(), 2, url);
                                        return "error";
                                    })
                                    .doOnNext(o -> {
                                        boolean hasErrorBeenThrown = TextUtils.equals(o, "error");
                                        if (!hasErrorBeenThrown) {//error has been thrown
                                            markAsCompleted(project.getId(), 2);
                                        }
                                    });
                        }
                    })

                    .subscribe(projectId -> {
                        //unused
                    }, Timber::e);


            DisposableManager.add(projectEduMatObservable);
            HashMap<String, FormDetails> failedForms = new HashMap<>();


//            Observable.just(selectedProject)
//                    .flatMapIterable((Function<ArrayList<Project>, Iterable<Project>>) projects -> projects)
//                    .filter(project -> selectedMap.get(project.getId()).get(1).sync)
//                    .flatMap(new Function<Project, Observable<Pair>>() {
//                        @Override
//                        public Observable<Pair> apply(Project project) throws Exception {
//                            Observable<Pair> odkForms = ODKFormRemoteSource.getInstance().getFormListByProject(project);
//                            return odkForms;
//                        }
//                    })
//                    .doOnNext(new Consumer<Pair>() {
//                        @Override
//                        public void accept(Pair pair) throws Exception {
//                            Intent toFormDownloadList = new Intent(SyncServiceV3.this, FormDownloadList.class);
//                            ArrayList<String> forms = (ArrayList<String>) pair.first;
//                            toFormDownloadList.putStringArrayListExtra(ApplicationConstants.BundleKeys.FORM_IDS,forms);
//                            toFormDownloadList.putExtra(ApplicationConstants.BundleKeys.URL, pair.second.toString());
//                            startActivity(toFormDownloadList);
//                        }
//                    })
//                    .subscribeOn(Schedulers.io())
//                    .subscribe(new DisposableObserver<Pair>() {
//                        @Override
//                        public void onNext(Pair pair) {
//                            Timber.i(pair.toString());
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            Timber.e(e);
//                        }
//
//                        @Override
//                        public void onComplete() {
//
//                        }
//                    });
//
//
//            if(true)return;
            Disposable formsDownloadObservable = Observable.just(selectedProject)
                    .flatMapIterable((Function<ArrayList<Project>, Iterable<Project>>) projects -> projects)
                    .filter(project -> selectedMap.get(project.getId()).get(1).sync)
                    .flatMap(new Function<Project, Observable<List<?>>>() {
                        @Override
                        public Observable<List<?>> apply(Project project) {

                            Observable<ArrayList<GeneralForm>> generalForms = GeneralFormRemoteSource.getInstance().fetchGeneralFormByProjectId(project.getId()).toObservable();
                            Observable<ArrayList<ScheduleForm>> scheduledForms = ScheduledFormsRemoteSource.getInstance().fetchFormByProjectId(project.getId()).toObservable();
                            Observable<ArrayList<Stage>> stagedForms = StageRemoteSource.getInstance().fetchByProjectId(project.getId()).toObservable();

                            Observable<List<ArrayList<FormDetails>>> odkForms = SyncLocalSourcev3
                                    .getInstance()
                                    .getFailedUrls(project.getId(), 1)
                                    .subscribeOn(Schedulers.io())
                                    .flatMapObservable((Function<SyncStat, ObservableSource<List<ArrayList<FormDetails>>>>) syncStat -> {
                                        List<String> failedFormsUrls = new Gson().fromJson(syncStat.getFailedUrl(), new TypeToken<List<String>>() {
                                        }.getType());

                                        boolean isValidList = syncStat.getFailedUrl() != null &&
                                                syncStat.getFailedUrl().contains("[") &&
                                                failedFormsUrls.size() > 0;

                                        if (isValidList) {
                                            return ODKFormRemoteSource.getInstance().getByProjectId(project, failedFormsUrls);
                                        } else {
                                            return ODKFormRemoteSource.getInstance().getByProjectId(project);
                                        }
                                    });


                            return Observable.concat(odkForms, generalForms, scheduledForms, stagedForms)
                                    .doOnNext(new Consumer<List<? extends Object>>() {
                                        @SuppressLint("CheckResult")
                                        @Override
                                        public void accept(List<?> objects) throws Exception {

                                            if (isListOfType(objects, ArrayList.class)) {
                                                saveFailedFormUrls(objects);
                                            }


                                        }

                                        private void saveFailedFormUrls(List<?> objects) {
                                            List<ArrayList<FormDetails>> formDetailsPerProject = (List<ArrayList<FormDetails>>) objects;
                                            for (ArrayList<FormDetails> formDetailsList : formDetailsPerProject) {
                                                ArrayList<String> failed = new ArrayList<>();
                                                for (FormDetails formDetails : formDetailsList) {
                                                    failed.add(formDetails.getDownloadUrl());
                                                }

                                                markAsFailed(project.getId(), 1, failed.toString());
                                            }
                                        }
                                    })
                                    .doOnSubscribe(disposable -> markAsRunning(project.getId(), 1))
                                    .doOnDispose(() -> markAsFailed(project.getId(), 1, ""))
                                    .onErrorReturn(new Function<Throwable, List<? extends Object>>() {
                                        @Override
                                        public List<? extends Object> apply(Throwable throwable) throws Exception {
                                            Timber.e(throwable);
                                            String urls = new ArrayList<String>() {
                                                {
                                                    add(APIEndpoint.BASE_URL + APIEndpoint.ASSIGNED_FORM_LIST_PROJECT.concat(project.getId()));
                                                    add(APIEndpoint.BASE_URL + APIEndpoint.ASSIGNED_FORM_LIST_SITE.concat(project.getId()));
                                                }
                                            }.toString();

                                            markAsFailed(project.getId(), 1, urls);
                                            return new ArrayList<>();
                                        }
                                    });
                        }
                    })
                    .subscribe(project -> {
                        //unused
                    }, Timber::e);


            DisposableManager.add(formsDownloadObservable);

        } catch (NullPointerException e) {
            Timber.e(e);
        }
    }


    private void cancelAllTask() {
        for (Disposable disposable : syncDisposable) {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }
    }


    private Function<Project, Observable<Object>> getSitesObservable() {
        return new Function<Project, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Project project) throws Exception {
                Observable<Object> regionSitesObservable = Observable.just(project.getRegionList())
                        .flatMapIterable((Function<List<Region>, Iterable<Region>>) regions -> regions)
                        .flatMapSingle(new Function<Region, SingleSource<SiteResponse>>() {
                            @Override
                            public SingleSource<SiteResponse> apply(Region region) {
                                return SiteRemoteSource.getInstance().getSitesByRegionId(region.getId())
                                        .doOnSuccess(saveSites());
                            }
                        })
                        .concatMap(new Function<SiteResponse, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(SiteResponse siteResponse) throws Exception {
                                if (siteResponse.getNext() == null) {
                                    return Observable.just(siteResponse);
                                }

                                return Observable.just(siteResponse)
                                        .concatWith(getSitesByUrl(siteResponse.getNext()));

                            }
                        });


                Observable<Object> projectObservable = Observable.just(project)
                        .filter(Project::getHasClusteredSites)
                        .flatMapSingle((Function<Project, SingleSource<SiteResponse>>) project1 -> SiteRemoteSource.getInstance().getSitesByProjectId(project1.getId()).doOnSuccess(saveSites()))
                        .concatMap(new Function<SiteResponse, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(SiteResponse siteResponse) {
                                if (siteResponse.getNext() == null) {
                                    return Observable.just(siteResponse);
                                }
                                return Observable.just(siteResponse)
                                        .concatWith(getSitesByUrl(siteResponse.getNext()));

                            }
                        });


                return Observable.concat(projectObservable, regionSitesObservable)
                        .doOnSubscribe(disposable -> markAsRunning(project.getId(), 0))
                        .onErrorReturn(throwable -> {
                            String url = getFailedFormUrl(throwable)[0];
                            failedSiteUrls.add(url);
                            return project.getId();
                        })
                        .doOnNext(o -> {
                            boolean hasErrorBeenThrown = o instanceof String;
                            if (!hasErrorBeenThrown) {//error has been thrown
                                markAsCompleted(project.getId(), 0);
                            }

                            if (failedSiteUrls.size() > 0) {
                                markAsFailed(project.getId(), 0, failedSiteUrls.toString());
                                failedSiteUrls.clear();
                            }
                        }).doOnDispose(new Action() {
                            @Override
                            public void run() throws Exception {
                                markAsFailed(project.getId(), 0, "");
                            }
                        });
            }
        };

    }


    private String[] getFailedFormUrl(Throwable throwable) {
        String failedUrl = "";
        String projectId = "";
        if (throwable instanceof RetrofitException) {
            RetrofitException retrofitException = (RetrofitException) throwable;
            String msg = retrofitException.getMessage();
            failedUrl = retrofitException.getUrl();
            projectId = retrofitException.getProjectId();

        }

        return new String[]{failedUrl, projectId};
    }

    private void markAsFailed(String projectId, int type, String failedUrl, boolean shouldRetry) {
        saveState(projectId, type, failedUrl, false, Constant.DownloadStatus.FAILED);
        Timber.e("Download stopped %s for project %s", failedUrl, projectId);
    }

    private void markAsFailed(String projectId, int type, String failedUrl) {
        markAsFailed(projectId, type, failedUrl, false);
    }


    private void markAsRunning(String projectId, int type) {
        saveState(projectId, type, "", true, Constant.DownloadStatus.RUNNING);
    }


    private void markAsCompleted(String projectId, int type) {
        saveState(projectId, type, "", false, Constant.DownloadStatus.COMPLETED);
        Timber.e("Download completed for project %s", projectId);
    }

    private void saveState(String projectId, int type, String failedUrl, boolean started, int status) {
        Timber.d("saving for for %d stopped at %s for %s", type, failedUrl, projectId);
        if (selectedMap != null && selectedMap.containsKey(projectId)) {
//            selectedMap.get(projectId).get(type).completed = true;
            SyncStat syncStat = new SyncStat(projectId, type + "", failedUrl, started, status, System.currentTimeMillis());
            SyncLocalSourcev3.getInstance().save(syncStat);
        }
    }

    private Observable<Object> downloadByRegionObservable(ArrayList<Project> selectedProject, HashMap<String, List<Syncable>> selectedMap) {
        return Observable.just(selectedProject)
                .flatMapIterable((Function<ArrayList<Project>, Iterable<Project>>) projects -> projects)
                .filter(project -> selectedMap.get(project.getId()).get(0).sync)
                .flatMap(getSitesObservable());


    }


    private Consumer<? super SiteResponse> saveSites() {
        return (Consumer<SiteResponse>) siteResponse -> {
            Timber.i("Saving %d sites", siteResponse.getResult().size());

            SiteLocalSource.getInstance().save((ArrayList<Site>) siteResponse.getResult());
        };
    }

    private ObservableSource<? extends SiteResponse> getSitesByUrl(String url) {
        return SiteRemoteSource.getInstance().getSitesByURL(url)
                .toObservable()
                .filter(new Predicate<SiteResponse>() {
                    @Override
                    public boolean test(SiteResponse siteResponse) {
                        return siteResponse.getNext() != null;
                    }
                })
                .doOnNext(saveSites())
                .flatMap(new Function<SiteResponse, ObservableSource<? extends SiteResponse>>() {
                    @Override
                    public ObservableSource<? extends SiteResponse> apply(SiteResponse siteResponse) {
                        return getSitesByUrl(siteResponse.getNext());
                    }
                });
    }


    private String readaableSyncParams(String projectName, List<Syncable> list) {
        String logString = "";
        for (Syncable syncable : list) {
            logString += "\n title = " + syncable.getTitle() + ", sync = " + syncable.getSync();
        }
        return String.format("%s \n params = %s", projectName, logString);
    }
}

