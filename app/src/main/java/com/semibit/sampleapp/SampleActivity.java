package com.semibit.sampleapp;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.semibit.ezandroidutils.EzUtils;

public class SampleActivity extends SampleBaseActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.getInstance();
        EzUtils.toast(this,"OK");

    }
}
