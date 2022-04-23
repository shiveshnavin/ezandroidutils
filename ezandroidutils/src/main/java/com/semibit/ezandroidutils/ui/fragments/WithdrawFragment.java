package com.semibit.ezandroidutils.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.services.CrashReporter;
import  com.semibit.ezandroidutils.services.RestAPI;
import  com.semibit.ezandroidutils.ui.BaseFragment;
import  com.semibit.ezandroidutils.ui.activities.HomeActivity;
import  com.semibit.ezandroidutils.utils.ShowHideLoader;
import  com.semibit.ezandroidutils.EzUtils;
import  com.semibit.ezandroidutils.views.LoadingView;
import  com.semibit.ezandroidutils.views.RoundRectCornerImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class WithdrawFragment extends BaseFragment {

    private static WithdrawFragment mInstance;
    ShowHideLoader showHideLoader;
    private LinearLayout contLogin;
    private TextInputLayout contamount;
    private TextInputEditText amount;
    private TextView available;
    private TextView selectPayMethod;
    private ConstraintLayout contPaytm;
    private RadioButton paytmRadio;
    private TextView textPaytm;
    private RoundRectCornerImageView paytmImg;
    private TextInputLayout contPaytmNo;
    private TextInputEditText paytmNo;
    private ConstraintLayout contUpi;
    private RadioButton UpiRadio;
    private TextView textUpi;
    private RoundRectCornerImageView UpiImg;
    private TextInputLayout contUpiId;
    private TextInputEditText UpiIdNo;
    private TextView subtext;
    private LinearLayout linearLayout;
    private LoadingView loader;
    private Button request;

    public static WithdrawFragment getInstance() {
        if (mInstance == null)
            mInstance = new WithdrawFragment();
        return mInstance;
    }

    private void findViews(View root) {
        contLogin = (LinearLayout) root.findViewById(R.id.cont_login);
        contamount = (TextInputLayout) root.findViewById(R.id.contamount);
        amount = (TextInputEditText) root.findViewById(R.id.amount);
        available = (TextView) root.findViewById(R.id.available);
        selectPayMethod = (TextView) root.findViewById(R.id.selectPayMethod);
        contPaytm = (ConstraintLayout) root.findViewById(R.id.contPaytm);
        paytmRadio = (RadioButton) root.findViewById(R.id.paytmRadio);
        textPaytm = (TextView) root.findViewById(R.id.textPaytm);
        paytmImg = (RoundRectCornerImageView) root.findViewById(R.id.paytmImg);
        contPaytmNo = (TextInputLayout) root.findViewById(R.id.contPaytmNo);
        paytmNo = (TextInputEditText) root.findViewById(R.id.paytmNo);
        contUpi = (ConstraintLayout) root.findViewById(R.id.contUpi);
        UpiRadio = (RadioButton) root.findViewById(R.id.UpiRadio);
        textUpi = (TextView) root.findViewById(R.id.textUpi);
        UpiImg = (RoundRectCornerImageView) root.findViewById(R.id.UpiImg);
        contUpiId = (TextInputLayout) root.findViewById(R.id.contUpiId);
        UpiIdNo = (TextInputEditText) root.findViewById(R.id.UpiIdNo);
        subtext = (TextView) root.findViewById(R.id.subtext);
        linearLayout = (LinearLayout) root.findViewById(R.id.linearLayout);
        loader = (LoadingView) root.findViewById(R.id.loader);
        request = (Button) root.findViewById(R.id.request);
        showHideLoader = ShowHideLoader.create().content(request).loader(loader);
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        act = (HomeActivity) getActivity();
        init();

        View root = inflater.inflate(R.layout.fragment_withdraw, container, false);
        setUpToolbar(root);
        findViews(root);
        setTitle(getString(R.string.withdraw));
        amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(EzUtils.isEmpty(amount.getText().toString())){
                    request.setText(R.string.withdraw);
                }
                else{
                    try {
                        Integer i = Integer.parseInt(amount.getText().toString());
                        request.setText(String.format("%s  %s %d"
                                ,getString(R.string.withdraw)
                                ,getString(R.string.money_currency),i));

                    } catch (Exception e) {
                        try {
                            request.setText(R.string.invalidinput);
                        } catch (Exception exception) {
                            CrashReporter.reportException(e);
                        }
                        CrashReporter.reportException(e);
                    }
                }
            }
        });
        root.findViewById(R.id.bgg).setOnClickListener(c->{

        });
        WalletViewModel.getInstance().getWallet().observe(getViewLifecycleOwner(), wallet -> {
            available.setText(String.format(getString(R.string.available_award_balance), getString(App.getStringRes(R.string.currency)),""+wallet.getWinningBalance()));
        });

        String allowedPayments = FirebaseRemoteConfig.getInstance().getString("allowed_payment_methods");
        if(!allowedPayments.contains("paytm")){
            contPaytm.setVisibility(View.GONE);
        }
        if(!allowedPayments.contains("upi")){
            contUpi.setVisibility(View.GONE);
        }

        if(EzUtils.getKey("paytm",ctx)!=null){
            paytmNo.setText(EzUtils.getKey("paytm",ctx));
        }
        if(EzUtils.getKey("upi",ctx)!=null){
            UpiIdNo.setText(EzUtils.getKey("upi",ctx));
        }

        paytmRadio.setOnCheckedChangeListener((c, checked) -> {
            TransitionManager.beginDelayedTransition(contLogin);
            if (checked) {
                UpiRadio.setChecked(false);
                contPaytmNo.setVisibility(View.VISIBLE);
                contUpiId.setVisibility(View.GONE);

            }

        });

        UpiRadio.setOnCheckedChangeListener((c, checked) -> {
            TransitionManager.beginDelayedTransition(contLogin);
            if (checked) {
                paytmRadio.setChecked(false);
                contPaytmNo.setVisibility(View.GONE);
                contUpiId.setVisibility(View.VISIBLE);
            }
        });

        contPaytm.setOnClickListener(c->{
            paytmRadio.setChecked(true);
        });
        contUpi.setOnClickListener(c->{
            UpiRadio.setChecked(true);
        });


        request.setOnClickListener(c -> {

            String amts = amount.getText().toString();
            if(EzUtils.isEmpty(amts)){
                contamount.setError(getString(R.string.invalidinput));
                return;
            }
            long amt = Long.parseLong(amount.getText().toString());
            long minWithdrawlAmount = FirebaseRemoteConfig.getInstance().getLong("min_withdrawl_amount");

            if(UpiRadio.isChecked()){
                if(EzUtils.isEmpty(UpiIdNo.getText().toString())){
                    EzUtils.snack(act, R.string.select_pay);
                    return;
                }
            }
            else
            if(paytmRadio.isChecked()){
                if(EzUtils.isEmpty(paytmNo.getText().toString())){
                    EzUtils.snack(act, R.string.select_pay);
                    return;
                }
            }
            else {
                EzUtils.snack(act, R.string.select_pay);
                return;
            }

            if(amt < minWithdrawlAmount){
                contamount.setError(String.format(getString(R.string.min_withdrawl),""+ minWithdrawlAmount));
                return;
            }

            Wallet wallet = WalletViewModel.getInstance().getWallet().getValue();
           if(wallet !=null){
               if(wallet.getWinningBalance() > amt){
                   amount.setText("");
                   contamount.setError(null);
                   showHideLoader.loading();
                   String method = "";
                   if (paytmRadio.isChecked())
                       method = "paytm";
                   if (UpiRadio.isChecked())
                       method = "upi";

                   RestAPI.getInstance().withdraw(method, paytmNo.getText().toString(), UpiIdNo.getText().toString(), amt, new GenricObjectCallback<String>() {
                       @Override
                       public void onEntity(String data) {
                           showHideLoader.loaded();
                           WalletViewModel.getInstance().refresh(null);
                           try {
                               EzUtils.diagInfo(request, data, ctx.getString(R.string.dismiss),
                                       paytmRadio.isChecked() ?
                                               R.drawable.ic_paytm : R.drawable.ic_upi
                                       , dialogInterface -> {

                                       },1);
                           } catch (Exception e) {
                               CrashReporter.reportException(e);
                           }
                           AnalyticsReporter.getInstance().logWithdrawl(amt,data);
                       }

                       @Override
                       public void onError(String message) {
                           showHideLoader.loaded();
                           EzUtils.snack(act,message);
                       }
                   });
               }
               else {
                   contamount.setError(getString(R.string.insufficient_award));
               }
           }


        });

        return root;

    }

}