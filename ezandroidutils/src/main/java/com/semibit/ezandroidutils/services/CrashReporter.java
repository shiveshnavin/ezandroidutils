package com.semibit.ezandroidutils.services;

import  com.semibit.ezandroidutils.EzUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class CrashReporter {

    public static void reportMessage(String...msg){
        StringBuilder sb = new StringBuilder();
        for(String m:msg){
            sb.append(m).append(" ");
        }
        FirebaseCrashlytics.getInstance().log(sb.toString());
    }

    public static void reportException(Exception e){
        EzUtils.e(e.getMessage());
        e.printStackTrace();
        FirebaseCrashlytics.getInstance().recordException(e);
    }

}
