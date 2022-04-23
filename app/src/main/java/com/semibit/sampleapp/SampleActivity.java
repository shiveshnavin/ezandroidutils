package com.semibit.sampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.semibit.ezandroidutils.EzUtils;
import com.semibit.ezandroidutils.ui.BaseActivity;

public class SampleActivity extends BaseActivity {

    @Override
    public void startLogout() {

    }

    @Override
    public void startHome(Intent intent) {

    }

    @Override
    public void checkUpdate() {

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.getInstance();
        EzUtils.toast(this,"OK");

    }
}
