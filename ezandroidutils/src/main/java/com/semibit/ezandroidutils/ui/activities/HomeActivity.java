package com.semibit.ezandroidutils.ui.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.binding.GenericUserViewModel;
import  com.semibit.ezandroidutils.domain.dotpot.binding.GameViewModel;
import  com.semibit.ezandroidutils.models.ActionItem;
import  com.semibit.ezandroidutils.services.CrashReporter;
import  com.semibit.ezandroidutils.services.EventBusService;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.utils.ResourceUtils;
import  com.semibit.ezandroidutils.EzUtils;

import java.util.Date;

public class HomeActivity extends BaseActivity {

    NavController navController;
    BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            setContentView(R.layout.activity_home);
        }
        catch (Exception e){
            EzUtils.toast(this, ResourceUtils.getString(R.string.error_msg_restart));
            inAppNavService.restartApp(this);
            CrashReporter.reportException(e);
            return;
        }
        navView = findViewById(R.id.nav_view);
        setUpToolbar();
        checkUpdate();
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_wallet)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        GenericUserViewModel.getInstance()
                .updateLocalAndNotify(getApplicationContext(), EzUtils.readUserData());
        WalletViewModel.getInstance().refresh(null);
        GameViewModel.getInstance().refreshAmounts(this);

        if (!isNetworkAvailable()) {
            EzUtils.diagInfo(navView, getString(R.string.no_network), getString(R.string.ok), R.drawable.error, dialogInterface -> {

            },-1);
        }

        handleIntent();
        refreshFBAccessToken(App.getAppContext());
    }

    public void handleIntent(){
        Intent it = getIntent();
        if(it!=null){
            if(it.hasExtra("action")){
                String action = it.getStringExtra("action");
                    ActionItem a = new ActionItem();
                    a.actionType = action;
                    a.act = this;
                    EventBusService.getInstance().doActionItem(a);
            }
        }
    }

    @Override
    public void onBackPressed() {
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.nav_view);
        int seletedItemId = bottomNavigationView.getSelectedItemId();
        int backStack = fragmentManager.getBackStackEntryCount();
        if (backStack < 1) {
            if (R.id.navigation_dashboard == seletedItemId || R.id.navigation_wallet == seletedItemId) {
                navController.navigateUp();
            } else {
                super.onBackPressed();
            }
        } else {
            getSupportFragmentManager().popBackStack();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        inAppNavService.onActivityResult(requestCode,resultCode,data);


        if (requestCode == WebViewActivity.REQUEST_PAYMENT){
            if(resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_FIRST_USER) {
                if(data == null)
                    data = new Intent();
                Object s = data.getExtras().toString();
                try {
                    addedToWalletConfirm(navView,
                            data.getStringExtra("id"),
                            data.getStringExtra("amount"),
                            data.getStringExtra("timeStamp"),
                            null,
                            resultCode == Activity.RESULT_OK ? R.drawable.ic_done_tick : R.drawable.ic_pending, dialogInterface -> {
                            }, resultCode == Activity.RESULT_OK ? 1 : 0);
                } catch (Exception e) {
                    CrashReporter.reportException(e);
                    EzUtils.toast(ctx,getString(R.string.payment_success));
                }
                navController.navigate(R.id.navigation_wallet);
            }
            else{
                String message = getString(R.string.error) + "\n\n" + getString(R.string.error_msg_try_again);
                if (data != null && data.hasExtra("message")) {
                    message = message + " " + data.getStringExtra("message");
                }
                EzUtils.diagInfo(navView, message, getString(R.string.retry), R.drawable.error, dialogInterface -> {

                }, -1);
            }
        }

    }

    public static Dialog addedToWalletConfirm(View anchor, String txnid,
                                              String amount,
                                              String timeStamp,
                                              String secondaryActionText,
                                              int drawableIcon,
                                              final EzUtils.ClickCallBack click, int level) {

        try {
            final View dialogView = View.inflate(anchor.getContext(), R.layout.diag_wallet_add_success, null);

            final Dialog dialog = new Dialog(anchor.getContext(), R.style.PopupDialog);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(dialogView);

            TextView stText = dialogView.findViewById(R.id.text);
            TextView amtText = dialogView.findViewById(R.id.amount);
            TextView txnsuccess = dialogView.findViewById(R.id.txnsuccess);

            Button done = dialogView.findViewById(R.id.done);
            ImageView img = dialogView.findViewById(R.id.img);
            int mColor = R.color.colorTextSuccessDark;

            String amtStr = String.format("%s %s", ResourceUtils.getString(R.string.currency), amount);
            amtText.setText(amtStr);
            stText.setText(String.format(ResourceUtils.getString(R.string.txnId_and_date), txnid, EzUtils.getDateTime(new Date(Long.parseLong(timeStamp)), "hh:mm a dd MMM yyyy")));

            try {
                View contIcon = dialogView.findViewById(R.id.contIcon);
                switch (level) {
                    case 0:
                        mColor = R.color.colorTextWarning;
                        txnsuccess.setText(R.string.txn_pending);

                        break;
                    case 1:
                        mColor = R.color.colorTextSuccessDark;
                        txnsuccess.setText(String.format(ResourceUtils.getString(R.string.wallet_updated)));

                        break;
                    case -1:
                        mColor = R.color.colorTextError;
                        break;
                }

                contIcon.getBackground().setColorFilter(Color.parseColor(EzUtils.colorToHexNoAlpha(ResourceUtils.getColor(mColor))), PorterDuff.Mode.SRC_ATOP);

                img.setImageDrawable(anchor.getContext().getDrawable(drawableIcon));
            } catch (Exception e) {
                CrashReporter.reportException(e);
            }

      /*  RelativeLayout.LayoutParams params=new RelativeLayout.LayoutParams(dialogView.getWidth(),dialogView.getWidth());
        img.setLayoutParams(params);
*/
            if (secondaryActionText != null)
                done.setText(secondaryActionText);


            done.setOnClickListener((v) -> {
                dialog.dismiss();
                if (click != null)
                    click.done(dialog);
            });

            try {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                Window window = dialog.getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(ResourceUtils.getColor(mColor));
            } catch (Exception e) {
                if (EzUtils.DEBUG_ENABLED) e.printStackTrace();
            }

            try {
                dialog.show();
            } catch (Exception e) {
                if (EzUtils.DEBUG_ENABLED) e.printStackTrace();
            }

            return dialog;
        } catch (Exception e) {
            CrashReporter.reportException(e);
            e.printStackTrace();
            return null;
        }

    }

    public static void refreshFBAccessToken(Context act){

        try{
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();
            if(user!=null){
                auth.getAccessToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            String idToken = task.getResult().getToken();
                            if(!Strings.isNullOrEmpty(idToken)){
                                EzUtils.setKey(Constants.KEY_PROVIDERTOKEN, idToken, act);
                            }
                        }
                    }
                });
            }
        }catch (Exception e){
            CrashReporter.reportException(e);
        }
    }


}