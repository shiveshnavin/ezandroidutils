package com.semibit.ezandroidutils.services;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.semibit.ezandroidutils.Constants;
import com.semibit.ezandroidutils.models.ActionItem;
import com.semibit.ezandroidutils.ui.BaseActivity;

public class EventBusService {

    public static EventBusService mIsntance;

    public EventBusService() {
        mIsntance = this;
    }

    public static EventBusService getInstance() {
        if (mIsntance == null) mIsntance = new EventBusService();

        return mIsntance;
    }

    public void doActionItem(ActionItem cm) {

        BaseActivity act = cm.act;
        String actionType = cm.actionType;
        try {
            if (act != null) {

                if (actionType.equals(Constants.ACTION_HOME)) {
                    act.startHome();
                }
                if (cm.doFinish)
                    if (act != null) {
                        act.finish();
                    }
            }

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        }

    }
}
