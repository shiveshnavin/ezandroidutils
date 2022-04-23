package com.semibit.ezandroidutils.ui.activities;

import static  com.semibit.ezandroidutils.ui.activities.HomeActivity.refreshFBAccessToken;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;

import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.binding.GenericUserViewModel;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.services.LoginService;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigInfo;

public class SplashActivity extends BaseActivity {

    ImageView animLogo;
    private TextView head;
    private TextView subhead;
    private LinearLayout bottomContSplash;
    private VideoView videoView;
    private Button signup;
    private Button login;
    private LoginService loginService;

    private void findViews() {
        head = (TextView) findViewById(R.id.head);
        subhead = (TextView) findViewById(R.id.subhead);
        videoView = (VideoView) findViewById(R.id.videoView1);

        bottomContSplash = (LinearLayout) findViewById(R.id.bottomContSplash);
        signup = (Button) findViewById(R.id.signup);
        login = (Button) findViewById(R.id.request);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        findViews();
//        final MediaPlayer mp = MediaPlayer.create(this, R.raw.notif_tone);
        //mp.start();

        try{
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }catch (Exception ignored){

        }

        checkUpdate();
        animLogo = findViewById(R.id.animLogo);
        animLogo.setVisibility(View.VISIBLE);
        EzUtils.animate_avd(animLogo);
        updateFcm();
        String accessToken = EzUtils.getKey("access_token", ctx);
        if (accessToken != null) {
            mFirebaseRemoteConfig.ensureInitialized().addOnCompleteListener(new OnCompleteListener<FirebaseRemoteConfigInfo>() {
                @Override
                public void onComplete(@NonNull Task<FirebaseRemoteConfigInfo> task) {
                    refreshApiKeyFromRemoteConfig();
                    go();
                }
            });
        } else
            go();
        refreshFBAccessToken(App.getAppContext());

    }

    private void go() {
        loginService = new LoginService(this);
        loginService.getLoggedInUser(new GenricObjectCallback<GenricUser>() {
            @Override
            public void onEntity(GenricUser data) {
                if(data!=null && data.validate()){
                    animateAndHome(data.validate());
                }
                else {
                    inAppNavService.startRegister();
                    finish();
                }
            }

            @Override
            public void onError(String message) {

                if (!isNetworkAvailable() && EzUtils.readUserData() != null) {
                    GenericUserViewModel.getInstance().getUser().postValue(EzUtils.readUserData());
                    animateAndHome(true);
                } else
                  animateAndHome(false);
            }
        });
    }

    private void animateAndHome(boolean navToHomeAuto) {

        if (navToHomeAuto) {
            animLogo.postDelayed(() -> {
                inAppNavService.startHome(getIntent());
                finish();
            }, 1000);
        } else {
            EzUtils.logout();
            animLogo.postDelayed(this::showButtoms, 1000);
        }
    }

    private void showButtoms() {
        head.animate().setDuration(500).alpha(1.0f);
//        subhead.animate().setDuration(500).alpha(1.0f);
        bottomContSplash.animate().setDuration(500).alpha(1.0f);
        signup.setOnClickListener(v -> {
            inAppNavService.startRegister();
            finish();
        });
        login.setOnClickListener(v -> {
            inAppNavService.startLogin();
            finish();
        });
    }


}