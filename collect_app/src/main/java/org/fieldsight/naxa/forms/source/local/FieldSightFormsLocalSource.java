package org.fieldsight.naxa.forms.source.local;

import androidx.lifecycle.LiveData;

import org.fieldsight.naxa.common.BaseLocalDataSourceRX;
import org.fieldsight.naxa.common.Constant;
import org.fieldsight.naxa.common.FieldSightDatabase;
import org.odk.collect.android.application.Collect;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;

public class FieldSightFormsLocalSource implements BaseLocalDataSourceRX<FieldSightForm> {

    private static FieldSightFormsLocalSource INSTANCE;
    private FieldSightFormDAO dao;

    private FieldSightFormsLocalSource() {
        FieldSightDatabase database = FieldSightDatabase.getDatabase(Collect.getInstance());//todo inject context
        this.dao = database.getFieldSightFormDAO();
    }

    public static FieldSightFormsLocalSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FieldSightFormsLocalSource();
        }
        return INSTANCE;
    }

    public LiveData<List<FieldSightForm>> getGeneralForms(String projectId, String siteId) {
        return dao.getFormByType(Constant.FormType.GENERAl, projectId, siteId);
    }

    public LiveData<List<FieldSightForm>> getSurveyForms(String projectId, String siteId) {
        return dao.getFormByType(Constant.FormType.SURVEY, projectId, siteId);
    }

    public LiveData<List<FieldSightForm>> getScheduledForms(String projectId, String siteId) {
        return dao.getFormByType(Constant.FormType.SCHEDULE, projectId, siteId);
    }

    public LiveData<List<FieldSightForm>> getStagedForms(String projectId, String siteId) {
        return dao.getFormByType(Constant.FormType.STAGED, projectId, siteId);
    }

    public void saveForms(FieldSightForm... fieldSightForm) {
        dao.insert(fieldSightForm);
    }

    @Override
    public LiveData<List<FieldSightForm>> getAll() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Completable save(FieldSightForm... items) {
        return Completable.fromAction(() -> dao.insert(items));
    }

    @Override
    public void save(ArrayList<FieldSightForm> items) {
        dao.insert(items);
    }


    @Override
    public void updateAll(ArrayList<FieldSightForm> items) {

    }
}