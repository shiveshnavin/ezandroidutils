package com.semibit.ezandroidutils.ui.fragments;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
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
import  com.semibit.ezandroidutils.interfaces.GenricDataCallback;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.services.CrashReporter;
import  com.semibit.ezandroidutils.services.LoginService;
import  com.semibit.ezandroidutils.ui.BaseFragment;
import  com.semibit.ezandroidutils.ui.activities.AccountActivity;
import  com.semibit.ezandroidutils.utils.ResourceUtils;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class VerifyPhoneFragment extends BaseFragment {

    private static VerifyPhoneFragment mInstance;
    String phoneNumber;
    CountDownTimer countDownTimer;
    long timeout;
    String lastVerificationid = "";
    PhoneAuthProvider.ForceResendingToken lastforceResendingToken = null;
    private AccountActivity act;
    private LinearLayout contLogin;
    private TextInputLayout contentphone;
    private TextInputEditText phone;
    private TextInputLayout contentpaswd;
    private TextInputEditText password;
    private LinearLayout linearLayout;
    private Button login;
    GenricObjectCallback<Task<AuthResult>> onVerificatoinComplete = new GenricObjectCallback<Task<AuthResult>>() {
        @Override
        public void onEntity(Task<AuthResult> task) {
            act.loginService.getTempGenricUser().setPhone(phoneNumber);
            GenericUserViewModel.getInstance().updateLocalAndNotify(act, act.loginService.getTempGenricUser());
            setUpPhoneVerif();
            next();
        }

        @Override
        public void onError(String message) {
            contentpaswd.setError(message);
            EzUtils.snack(act,message);
            if(!message.equals(ResourceUtils.getString(R.string.invalidotp))){
                setUpPhoneVerif();
            }
            else {
                login.setText(R.string.verify_otp);
            }

        }
    };
    private TextView subtext,termstext;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            act.loginService.signInWithPhoneAuthCredential(act, phoneAuthCredential, onVerificatoinComplete);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            contentphone.setError(getString(R.string.error_msg) + " " + e.getMessage());
            setUpPhoneVerif();
//                    EzUtils.snack(act,getString(R.string.error_msg)+e.getMessage());
        }

        @Override
        public void onCodeSent(@NonNull String Verificationid, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(Verificationid, forceResendingToken);
            lastVerificationid = Verificationid;
            lastforceResendingToken = forceResendingToken;
            contentpaswd.setVisibility(View.VISIBLE);
            login.setText(R.string.verify_otp);
            login.setOnClickListener(v -> {
                login.setText(R.string.processing);
                String paswd = password.getText().toString();
                if (paswd.length() > 3) {
                    act.loginService.verifyPhoneNumberWithCode(act, lastVerificationid, paswd, onVerificatoinComplete);
                    contentpaswd.setError(null);
                } else
                    contentpaswd.setError(getString(R.string.invalidotp));
            });
            subtext.setVisibility(View.VISIBLE);
            if (countDownTimer != null)
                countDownTimer.cancel();

            countDownTimer = new CountDownTimer(timeout * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    long left = (l) / 1000;
                    try {
                        subtext.setText(getString(R.string.retry_in_30s, left));
                    } catch (Exception e) {
                        e.printStackTrace();
                        CrashReporter.reportException(e);
                    }
                }

                @Override
                public void onFinish() {
                    try {
                        subtext.setText(R.string.retry);
                        subtext.setOnClickListener(v -> {
                            login.setText(R.string.processing);
                            act.loginService.resendVerificationCode(phoneNumber, timeout, forceResendingToken, callbacks);
                            subtext.setVisibility(View.GONE);
                            subtext.setOnClickListener(null);
                        });
                    } catch (Exception e) {
                        CrashReporter.reportException(e);
                    }
                }
            };
            countDownTimer.start();
        }

        ;

        @Override
        public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
            super.onCodeAutoRetrievalTimeOut(s);
        }
    };
    View.OnClickListener onClickLoginSendOtp = v -> {
        phoneNumber = phone.getText().toString();

        String ccode = ResourceUtils.getString(R.string.ccode);
        if (!phoneNumber.contains(ccode))
            phoneNumber = ccode + phoneNumber;


        if (act.loginService.getTempGenricUser().getPhone() != null &&
                act.loginService.getTempGenricUser().getPhone().equals(phoneNumber)) {
            if(LoginService.isPasswordMandatoryForSignup()) {
                act.beginChangePassword(true);
            }else{
                act.startHome();
            }
            return;
        }

        if (!LoginService.isValidPhone(phoneNumber)) {
            contentphone.setError(getString(R.string.invalidinput));
        } else {
            login.setText(R.string.processing);
            act.loginService.checkPhoneExists(phoneNumber, new GenricDataCallback() {

                @Override
                public void onStart(String data1, int data2) {
                    if (data2 == 1) {
                        contentphone.setError(null);

                        timeout = act.mFirebaseRemoteConfig.getLong("otp_timeout_sec");
                        login.setOnClickListener(null);
                        act.loginService.sendOTP(phoneNumber, timeout, callbacks);

                        password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View view, boolean b) {
                                if (b) {
                                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
                                    if (clipboard != null && clipboard.hasPrimaryClip()) {
                                        try {
                                            String possibleOTP = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
                                            if (EzUtils.isNumeric(possibleOTP) && possibleOTP.length() < 8) {
                                                password.setText(possibleOTP);
                                            }
                                        } catch (Exception e) {
                                            EzUtils.e("Unable to get OTP from clipboard ! : 200");
                                        }
                                    }
                                }
                            }
                        });

                    } else {
                        login.setText(R.string.send_otp);
                        contentphone.setError(data1);
                    }
                }
            });

        }

    };

    public static VerifyPhoneFragment getInstance() {
        if (mInstance == null)
            mInstance = new VerifyPhoneFragment();
        return mInstance;
    }

    private void setUpPhoneVerif() {
        subtext.setVisibility(View.GONE);
        contentpaswd.setError(null);
        login.setOnClickListener(onClickLoginSendOtp);
        contentpaswd.setVisibility(View.GONE);
        password.setText("");
        login.setText(R.string.send_otp);
        if (countDownTimer != null)
            countDownTimer.cancel();
    }

    private void findViews(View root) {
        contLogin = (LinearLayout) root.findViewById(R.id.cont_login);
        contentphone = (TextInputLayout) root.findViewById(R.id.contentphone);
        phone = (TextInputEditText) root.findViewById(R.id.phone);
        contentpaswd = (TextInputLayout) root.findViewById(R.id.contentpaswd);
        password = (TextInputEditText) root.findViewById(R.id.password);
        linearLayout = (LinearLayout) root.findViewById(R.id.linearLayout);
        login = (Button) root.findViewById(R.id.request);
        subtext = (TextView) root.findViewById(R.id.subtext);
        termstext = (TextView) root.findViewById(R.id.termstext);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        act = (AccountActivity) getActivity();
        init();

        View root = inflater.inflate(R.layout.fragment_phone, container, false);
        findViews(root);
        String textBtm = termstext.getText().toString();
        termstext.setText(Html.fromHtml(textBtm));
        termstext.setOnClickListener(v->{
            act.inAppNavService.startWebsite(act.getString(R.string.terms), Constants.HOST+"/toc.html?app=1");
        });
        if (act.loginService.getTempGenricUser().getPhone() != null) {
            phone.setText(act.loginService.getTempGenricUser().getPhone());
        }
        else {
            phone.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    if(s.length()==10){
                        EzUtils.hideSoftKeyboard(act);
                    }

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }
        login.setOnClickListener(onClickLoginSendOtp);

        return root;

    }

    private synchronized void next() {


        act.loginService.commitPasswordAndPhone(null, null, phoneNumber, new GenricObjectCallback<GenricUser>() {
            @Override
            public void onEntity(GenricUser data) {
                login.setVisibility(View.VISIBLE);
                if (LoginService.isValidPhone(phoneNumber)) {
                    AnalyticsReporter.getInstance().logSignup();
                    if (LoginService.isPasswordMandatoryForSignup())
                        act.beginChangePassword(true);
                    else
                        act.startHome();
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


    @Override
    public void onDestroyView() {
        try {
            if (countDownTimer != null)
                countDownTimer.cancel();
        } catch (Exception e) {
            CrashReporter.reportException(e);
        }
        super.onDestroyView();
    }
}