package com.semibit.ezandroidutils.services;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.semibit.ezandroidutils.Constants;
import com.semibit.ezandroidutils.R;
import com.semibit.ezandroidutils.ui.BaseActivity;
import com.semibit.ezandroidutils.ui.BaseFragment;
import com.semibit.ezandroidutils.ui.activities.WebViewActivity;
import com.semibit.ezandroidutils.ui.messaging.MessagingFragment;

public class InAppNavService {

    private BaseActivity ctx;
    private FragmentManager fragmentManager;

    public InAppNavService(BaseActivity ctx) {
        this.ctx = ctx;
        fragmentManager = ctx.fragmentManager;
    }

    private void startActivity(Intent it) {
        ctx.startActivity(it);
    }

    public void startHome(Intent intent) {
        ctx.startHome(intent);
    }

    public void startHome() {
        ctx.startHome();
    }


    public void startChatMessaging(@IdRes int fragmentViewId) {
        fragmentTransaction(fragmentViewId, MessagingFragment.getInstance(), "suport", null, true, Constants.TRANSITION_HORIZONTAL);
    }

    public void startWebsite(String title, String url) {
        Intent it = new Intent(ctx, WebViewActivity.class);
        it.putExtra("title", title);
        it.putExtra("url", url);
        ctx.startActivity(it);

    }
    public <T extends BaseFragment> void fragmentTransaction(@IdRes int fragmentViewId, T target
            , String name, Bundle data, boolean addToBackStack) {

        fragmentTransaction(fragmentViewId, target, name, data, addToBackStack, Constants.TRANSITION_VERTICAL);

    }


    public <T extends BaseFragment> void fragmentTransaction(@IdRes int fragmentViewId, T target
            , String name, Bundle data, boolean addToBackStack, int transition) {
        fragmentTransaction(fragmentViewId,target,name,data,addToBackStack,transition,true);
    }

    public <T extends BaseFragment> void fragmentTransaction(@IdRes int fragmentViewId, T target
            , String name, Bundle data, boolean addToBackStack, int transition, boolean replaceFragment) {

        try {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            if (transition == Constants.TRANSITION_HORIZONTAL)
                fragmentTransaction = fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
            else if (transition == Constants.TRANSITION_VERTICAL)
                fragmentTransaction = fragmentTransaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top, R.anim.slide_in_top, R.anim.slide_out_bottom);
            target.setArguments(data);
            target.setActivityAndContext(ctx);
            if (replaceFragment)
                fragmentTransaction.replace(fragmentViewId, target)
                        .setReorderingAllowed(true);
            else
                fragmentTransaction.add(fragmentViewId, target)
                        .setReorderingAllowed(true);

            if (addToBackStack) {
                fragmentTransaction = fragmentTransaction.addToBackStack(name);
            } else {
                fragmentTransaction = fragmentTransaction
                        .disallowAddToBackStack();
            }
            fragmentTransaction.commit();
        } catch (Exception e) {
            CrashReporter.reportException(e);
        }
    }

}
