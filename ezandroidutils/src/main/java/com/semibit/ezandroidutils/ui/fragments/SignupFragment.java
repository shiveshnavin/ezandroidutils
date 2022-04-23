package com.semibit.ezandroidutils.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.binding.GenericUserViewModel;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.services.CrashReporter;
import  com.semibit.ezandroidutils.services.LoginService;
import  com.semibit.ezandroidutils.ui.BaseFragment;
import  com.semibit.ezandroidutils.ui.activities.AccountActivity;
import  com.semibit.ezandroidutils.utils.DateTimePicker;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class SignupFragment extends BaseFragment {

    private static SignupFragment mInstance;
    private AccountActivity act;
    private LinearLayout contLogin;
    private TextInputLayout contentmail;
    private TextInputEditText email;
    private TextInputLayout contentname;
    private TextInputEditText name;
    private TextInputLayout contentpaswd;
    private TextInputEditText paswd;
    private LinearLayout linearLayout;
    private Button login;

    public static SignupFragment getInstance() {
        if (mInstance == null)
            mInstance = new SignupFragment();
        return mInstance;
    }

    private void findViews(View root) {
        contLogin = (LinearLayout) root.findViewById(R.id.cont_login);
        contentmail = (TextInputLayout) root.findViewById(R.id.contentmail);
        email = (TextInputEditText) root.findViewById(R.id.email);
        contentname = (TextInputLayout) root.findViewById(R.id.contentname);
        name = (TextInputEditText) root.findViewById(R.id.name);
        contentpaswd = (TextInputLayout) root.findViewById(R.id.contentpaswd);
        paswd = (TextInputEditText) root.findViewById(R.id.paswd);
        linearLayout = (LinearLayout) root.findViewById(R.id.linearLayout);
        login = (Button) root.findViewById(R.id.request);

        TextView subtext = root.findViewById(R.id.subtext);
        if (subtext != null) {
            String textBtm = subtext.getText().toString();
            subtext.setText(Html.fromHtml(textBtm));
            subtext.setOnClickListener(v -> {
                act.inAppNavService.startWebsite(act.getString(R.string.privacy_policy), Constants.HOST + "/privacy-policy.html?app=1");
            });
        }
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        init();
        act = (AccountActivity) getActivity();
        ctx = getActivity();
        View root = inflater.inflate(R.layout.fragment_signup, container, false);
        findViews(root);

        if (act.loginService.getTempGenricUser() != null) {
            setUpUI(act.loginService.getTempGenricUser());
        } else {
            EzUtils.toast(getContext(), getString(R.string.error_msg));
            act.beginLogin(false);
        }

        return root;
    }

    private void setUpUI(GenricUser user) {
        email.setText(user.getEmail());
        name.setText(user.getName());
        try {
            if (!EzUtils.isEmpty(user.getDob()))
                paswd.setText(user.getDateofbirthString());
            else
                paswd.setText("");
        } catch (Exception e) {
            CrashReporter.reportException(e);
        }

        DateTimePicker dateTimePicker = new DateTimePicker(act, DateTimePicker.DATE_ONLY, (DateTimePicker.MiliisCallback) dateTime -> {
            user.setDob("" + dateTime);
            paswd.setText(user.getDateofbirthString());
        });

        long maxBirth = System.currentTimeMillis() - 15 * 31556952000L;
        long initBirth = System.currentTimeMillis() - 23 * 31556952000L;

        dateTimePicker.setDateConstraints(0, initBirth, maxBirth);
        paswd.setOnClickListener(v -> {
            dateTimePicker.pick(false);
        });
        boolean disableAgeCheck = FirebaseRemoteConfig.getInstance().getBoolean("disable_age_check");
        if(disableAgeCheck){
            contentpaswd.setVisibility(View.GONE);
        }
        login.setOnClickListener(v -> {

            boolean ok = true;

            if (!disableAgeCheck && (user.getDob() == null || user.getAge() < 16)) {
                ok = false;
                contentpaswd.setError(getString(R.string.must_be18));
            } else
                contentpaswd.setError(null);

            if (user.getName() == null || user.getName().length() <= 1) {
                ok = false;
                contentname.setError(getString(R.string.invalidinput));
            } else
                contentname.setError(null);

            if (user.getEmail() == null || user.getEmail().length() <= 1 || !user.getEmail().contains("@")) {
                ok = false;
                contentmail.setError(getString(R.string.invalidinput));
            } else
                contentmail.setError(null);

            if (ok) {
                user.setName(name.getText().toString());
                login.setVisibility(View.GONE);
                act.loginService.commitTemporaryUserToServer(new GenricObjectCallback<GenricUser>() {
                    @Override
                    public void onEntity(GenricUser data) {
                        login.setVisibility(View.VISIBLE);
                        GenericUserViewModel.getInstance().updateLocalAndNotify(act, data);
                        Intent it = act.getIntent();

                        // from edit profile after logging in
                        if (it != null && it.getStringExtra("action") != null
                                && it.getStringExtra("action").equals(Constants.ACTION_ACCOUNT)) {
                            act.finish();
                        }


                        // user has entered a valid phone
                        else if (LoginService.isValidPhone(user.getPhone())) {
                            {
                                if (LoginService.isPasswordMandatoryForSignup() && EzUtils.isEmpty(user.getPassword())) {
                                    act.beginChangePassword(true);
                                } else {
                                    act.inAppNavService.startHome();
                                }
                            }
                        } else
                            act.beginPhone(true);
                    }

                    @Override
                    public void onError(String message) {
                        login.setText(R.string.next);
                        login.setVisibility(View.VISIBLE);
                        EzUtils.snack(act, getString(R.string.error_msg) + " " + message);
                    }
                });

            }

        });

    }
}