package com.semibit.ezandroidutils.binding;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.semibit.ezandroidutils.App;
import com.semibit.ezandroidutils.EzUtils;
import com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import com.semibit.ezandroidutils.models.GenricUser;
import com.semibit.ezandroidutils.ui.BaseActivity;


public class GenericUserViewModel extends ViewModel {

    private static GenericUserViewModel instance;
    private MutableLiveData<GenricUser> genricUserLive;

    public static GenericUserViewModel getInstance() {
        if (instance == null) {
            instance = new GenericUserViewModel();
            instance.genricUserLive = new MutableLiveData<>();
        }
        return instance;
    }

    public void refresh() {
        GenricUser user = EzUtils.readUserData();
        if (user != null) {
            BaseActivity.restApi.getGenricUser(user.getId(), new GenricObjectCallback<GenricUser>() {
                @Override
                public void onEntity(GenricUser data) {
                    genricUserLive.setValue(data);
                }

                @Override
                public void onError(String message) {
                    EzUtils.e(GenericUserViewModel.class, "Unable to refresh " + message);
                }
            });
        } else {
            EzUtils.e(GenericUserViewModel.class, "Unable to refresh as not user found");
        }
    }

    public MutableLiveData<GenricUser> getUser() {
        return genricUserLive;
    }

    public void updateLocalAndNotify(Context act, GenricUser user) {
        if (act != null) {
            EzUtils.writeUserData(user, act);
        } else {
            EzUtils.writeUserData(user, App.getAppContext());
        }
        if (user != null)
            genricUserLive.postValue(user);

    }

    public void onlyIfPresent(GenricObjectCallback<GenricUser> cb){
        if(getUser().getValue()!=null){
            cb.onEntity(getUser().getValue());
        }
    }

}