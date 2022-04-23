package com.semibit.ezandroidutils.binding;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.semibit.ezandroidutils.App;
import com.semibit.ezandroidutils.EzUtils;

import java.util.ArrayList;

public class NotificationsViewModel extends ViewModel {

    private static NotificationsViewModel instance;
    private MutableLiveData<ArrayList<EzUtils.NotificationMessage>> notifications;

    public static NotificationsViewModel getInstance() {
        if (instance == null) {
            instance = new NotificationsViewModel();
            instance.notifications = new MutableLiveData<>();
        }
        return instance;
    }

    public void refresh() {
        ArrayList<EzUtils.NotificationMessage> notifList = EzUtils.NotificationMessage.getAll(App.getAppContext());
        notifications.postValue(notifList);
    }

    public MutableLiveData<ArrayList<EzUtils.NotificationMessage>> getNotifications() {
        return notifications;
    }

    public void updateLocalAndNotify(Context act, ArrayList<EzUtils.NotificationMessage> user) {
        notifications.postValue(user);
    }

}