package com.semibit.ezandroidutils.ui.fragments;

import static  com.semibit.ezandroidutils.services.LoginService.RC_SIGN_IN;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.ui.BaseFragment;
import  com.semibit.ezandroidutils.ui.activities.AccountActivity;
import  com.semibit.ezandroidutils.utils.ShowHideLoader;
import  com.semibit.ezandroidutils.utils.TesterBot;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginFragment extends BaseFragment {

    private AccountActivity act;

    private LinearLayout contLogin;
    private TextInputLayout contentmail;
    private TextInputEditText email;
    private View loader;
    private TextInputLayout contentpaswd;
    private TextInputEditText paswd;
    private LinearLayout linearLayout;
    private Button signup;
    private Button login;
    private TextView forgotPassword;
    private View contInputs;

    private static LoginFragment mInstance;
    public static LoginFragment getInstance(){
        if(mInstance==null)
            mInstance = new LoginFragment();
        return mInstance;
    }
    private void findViews(View root) {
        contLogin = (LinearLayout) root.findViewById(R.id.cont_login);
        contentmail = (TextInputLayout) root.findViewById(R.id.contentmail);
        loader =  root.findViewById(R.id.loader);
        email = (TextInputEditText) root.findViewById(R.id.email);
        contentpaswd = (TextInputLayout) root.findViewById(R.id.contentpaswd);
        paswd = (TextInputEditText) root.findViewById(R.id.paswd);
        linearLayout = (LinearLayout) root.findViewById(R.id.linearLayout);
        signup = (Button) root.findViewById(R.id.signup);
        login = (Button) root.findViewById(R.id.request);
        forgotPassword = (TextView) root.findViewById(R.id.forgotPassword);
        contInputs = root.findViewById(R.id.contInputs);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        act = (AccountActivity) getActivity();
        View root = inflater.inflate(R.layout.fragment_login, container, false);
        findViews(root);
        ShowHideLoader showHideLoader = ShowHideLoader.create().loader(loader);

        signup.setOnClickListener(v -> {
            act.loginService.googleLogin(RC_SIGN_IN);
            contInputs.setVisibility(View.GONE);
            forgotPassword.setVisibility(View.GONE);
            login.setTag("google");
        });
        login.setOnClickListener(v ->{
            if(login.getTag() == null || login.getTag().equals("google")){
                contInputs.setVisibility(View.VISIBLE);
                forgotPassword.setVisibility(View.VISIBLE);
                login.setTag("email");
                return;
            }
            showHideLoader.loading();
                act.loginService.emailPhoneLogin(
                        email.getText().toString(),
                        paswd.getText().toString(),
                        new GenricObjectCallback<GenricUser>() {
                            @Override
                            public void onEntity(GenricUser data) {
                                showHideLoader.loaded();
                                act.finish();
                                act.inAppNavService.startHome();
                                AnalyticsReporter.getInstance().logLogin();
                            }

                            @Override
                            public void onError(String message) {
                                showHideLoader.loaded();
                                contentpaswd.setError(getString(R.string.invalidorpaswd));
                                contentmail.setError(getString(R.string.invalidemailorpaswd));
                            }
                        });
        });


        forgotPassword.setOnClickListener(v -> {

            if (email.getText().toString().isEmpty()) {
                contentmail.setError(getString(R.string.emailorphone));
                return;
            } else {
                contentmail.setError(null);
            }
            showHideLoader.loading();
            act.loginService.sendPasswordResetMail(email.getText().toString(),
                    (data1, data2) -> {
                        showHideLoader.loaded();

                        if (data2 == 1)
                        {
                            try {
                                EzUtils.diagBottom(act, getString(R.string.reset_sent),getString(R.string.reset_sent_info)
                                ,true,getString(R.string.dismiss),null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else
                            EzUtils.snack(act, getString(R.string.error_msg));

                    });

        });

//        TesterBot.execute(testerBot,new Handler(),2000);

        return root;
    }

    TesterBot testerBot = new TesterBot() {
        @Override
        public void eval() {
                email.setText(App.testEmail);
                paswd.setText(App.testPass);
                email.setEnabled(false);
                paswd.setEnabled(false);
                login.callOnClick();
        }
    };

}