package com.semibit.ezandroidutils;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import androidx.annotation.StringRes;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.semibit.ezandroidutils.models.GenricUser;
import com.semibit.ezandroidutils.services.CrashReporter;
import com.semibit.ezandroidutils.services.DBService;

import org.json.JSONArray;


/**
 * Created by shivesh on 2/8/18.
 */

public class App extends Application {

    public static final int VERSION_CODE = 1;
    public static FirebaseRemoteConfig mFirebaseRemoteConfig;
    public static Context mContext;
    public static App instance;
    public static GenricUser userModel;
    public static final boolean isBotTestMode = false;
    // todo : remove email
    public static final String testEmail = "test@gmail.com";
    public static final String testPass = "Test@123";


    public static GenricUser getGenricUser() {
        if (userModel == null) {
            userModel = EzUtils.readUserData();
        }
        return userModel;
    }

    public static void setGenricUser(GenricUser userModel) {
        App.userModel = userModel;
    }

    public static void switchApp(boolean isDebugApk) {

        if (isDebugApk) {
            //   Constants.HOST = "http://192.168.0.117:8080";
                Constants.HOST="https://yoloplay.in";
        } else {
            String hosts = "[]";
            try{
                JSONArray jsonArray = new JSONArray(hosts);
                Constants.HOST = jsonArray.getString(EzUtils.randomInt(0,jsonArray.length()-1));
                if(EzUtils.isEmpty(Constants.HOST))
                    throw new Exception("Host not present in config");
            }catch (Exception e){
                CrashReporter.reportException(e);
                Constants.HOST = "https://yoloplay.in";
            }
        }
    }

    public static Context getAppContext() {
        return mContext;
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        FirebaseApp.initializeApp(this);
        mContext = this;

        EzUtils.init(this);


        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(3600)
                        .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.default_config);
        mFirebaseRemoteConfig.fetch().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //utl.e("mFirebaseRemoteConfig","ACTIVATED"+mFirebaseRemoteConfig.getAll());
                mFirebaseRemoteConfig.activate();
            }
        });
        EzUtils.init(this);

        try {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getBoolean(String key) {
        return false;
    }
}
