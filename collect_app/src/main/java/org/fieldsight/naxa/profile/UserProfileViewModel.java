//package org.fieldsight.naxa.profile;
//
//import androidx.lifecycle.MutableLiveData;
//import androidx.lifecycle.ViewModel;
//
//import org.odk.collect.android.application.Collect;
//import org.fieldsight.naxa.common.FieldSightUserSession;
//import org.fieldsight.naxa.login.model.User;
//
//import java.io.File;
//
//import io.reactivex.Observable;
//
//
//public class UserProfileViewModel extends ViewModel {
//
//    private final UserProfileRepository userProfileRepository;
//    private final MutableLiveData<User> user = new MutableLiveData<>();
//    private final MutableLiveData<Boolean> syncLiveData = new MutableLiveData<>();
//    private final MutableLiveData<Boolean> editProfile = new MutableLiveData<>();
//    private final MutableLiveData<Boolean> progressBar = new MutableLiveData<>();
//
//    public UserProfileViewModel() {
//        this.userProfileRepository = new UserProfileRepository();
//        getUser().setValue(get());
//    }
//
//    public void save() {
//        userProfileRepository.save(getUser().getValue());
//    }
//
//    private User get() {
//        return FieldSightUserSession.getUser();
//    }
//
//    public Observable<User> upload() {
//        return userProfileRepository.upload(get());
//    }
//
//    public MutableLiveData<Boolean> getEditProfile() {
//        return editProfile;
//    }
//
//    public void setEditProfile(Boolean value) {
//        editProfile.setValue(value);
//    }
//
//    public MutableLiveData<User> getUser() {
//        return user;
//    }
//
//    public void setUser(User user) {
//        this.user.setValue(user);
//    }
//
//    public MutableLiveData<Boolean> getSyncLiveData() {
//        return syncLiveData;
//    }
//
//    public void setSyncLiveData(Boolean syncValue) {
//        this.syncLiveData.setValue(syncValue);
//    }
//
//    public MutableLiveData<Boolean> getProgressBar() {
//        return progressBar;
//    }
//
//    public void setProgressBar(Boolean progressBar) {
//        this.progressBar.setValue(progressBar);
//    }
//
//    public File generateImageFile(String imageName) {
//        String path = Collect.SITES_PATH +
//                File.separator +
//                imageName +
//                ".jpg";
//
//        int i = 2;
//        File f = new File(path);
//        while (f.exists()) {
//
//            path = Collect.SITES_PATH +
//                    File.separator +
//                    imageName +
//                    "_" +
//                    i +
//                    ".jpg";
//
//
//            f = new File(path);
//            i++;
//        }
//        return f;
//    }
//}
