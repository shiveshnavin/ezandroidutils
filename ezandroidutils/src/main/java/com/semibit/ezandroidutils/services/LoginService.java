package com.semibit.ezandroidutils.services;

import static  com.semibit.ezandroidutils.Constants.API_USERS;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.androidnetworking.error.ANError;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.binding.GenericUserViewModel;
import  com.semibit.ezandroidutils.interfaces.GenricDataCallback;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkRequestCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkService;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.ui.activities.AccountActivity;
import  com.semibit.ezandroidutils.utils.ResourceUtils;
import  com.semibit.ezandroidutils.EzUtils;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoginService {

    public static int RC_SIGN_UP = 1001;
    public static int RC_SIGN_IN = 1002;
    private final BaseActivity ctx;
    private final InAppNavService inAppNavService;
    private final GoogleSignInClient mGoogleSignInClient;
    private final FirebaseAuth firebaseAuth;
    private final GoogleSignInOptions gso;
    public NetworkService networkService;
    private GenricUser tempGenricUser;

    public LoginService(BaseActivity ctx) {
        this.ctx = ctx;
        inAppNavService = ctx.inAppNavService;
        firebaseAuth = ctx.mAuth;
        networkService = BaseActivity.netService;
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(BaseActivity.mFirebaseRemoteConfig.getString("google_web_client_id"))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(App.getAppContext(), gso);

    }

    public static boolean isValidPhone(String phone) {
        return phone != null && (phone.length() == 10 || phone.length() == 13 && phone.startsWith("+"));
    }

    public static boolean isPasswordMandatoryForSignup() {
        return FirebaseRemoteConfig.getInstance().getBoolean("password_manadatory");
    }

    private GoogleSignInAccount getSignedInAccount() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);
        return account;
    }

    public GenricUser getTempGenricUser() {
        if (tempGenricUser == null) {
            tempGenricUser = EzUtils.readUserData();
        }
        return tempGenricUser;
    }

    public void setTempGenricUser(GenricUser tempGenricUser) {
        this.tempGenricUser = tempGenricUser;
    }

    public void googleLogin(int code) {

        GoogleSignInAccount account = getSignedInAccount();

        if (account != null) {
            EzUtils.logout();
        }
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        ctx.startActivityForResult(signInIntent, code);
    }

    public void emailPhoneLogin(String emailPhone,
                                String paswd,
                                GenricObjectCallback<GenricUser> cb) {


        JSONObject jop = new JSONObject();
        try {
            if (emailPhone.isEmpty() || paswd.isEmpty()) {
                cb.onError(ctx.getString(R.string.invalidemailorpaswd));
                return;
            }
            jop.put("password", paswd);

            if (emailPhone.contains("@")) {
                jop.put("email", emailPhone);
            } else if (EzUtils.isValidMobile(emailPhone)) {
                String ccode = ResourceUtils.getString(R.string.ccode);
                if (!emailPhone.contains(ccode))
                    emailPhone = ccode + emailPhone;
                jop.put("phone", emailPhone);
            } else {
                cb.onError(ctx.getString(R.string.invalidemailorpaswd));
                return;
            }

            String fcmToken = EzUtils.getKey("fcm_token", ctx);
            if (fcmToken != null) {
                jop.put("fcmToken", fcmToken);
            }

        } catch (Exception ignored) {
        }

        networkService.callPost((Constants.u(API_USERS))
                , jop, false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {

                        GenricUser genricUser = EzUtils.js.fromJson(response.toString(), GenricUser.class);
                        if (genricUser != null)
                            firebaseAuth.signInWithEmailAndPassword(genricUser.getEmail(), genricUser.getPassword()).addOnCompleteListener(ctx, (task) -> {
                                if (!task.isSuccessful()) {
                                    try {
                                        EzUtils.toast(ctx, ctx.getstring(R.string.problem_logging_in) + task.getException().getMessage());
                                    } catch (Exception e) {
                                        CrashReporter.reportException(e);
                                    }
                                } else {
                                    FirebaseUser firebaseUser = task.getResult().getUser();
                                    try {
                                        refreshProviderToken((token, status) -> {
                                            EzUtils.setKey(Constants.KEY_PROVIDERTOKEN, token, ctx);
                                            networkService.updateTokens(token, token);
                                        });
                                    } catch (Exception e) {
                                        CrashReporter.reportException(e);
                                    }
                                }
                                cb.onEntity(genricUser);
                                GenericUserViewModel.getInstance().updateLocalAndNotify(ctx.getApplicationContext(), genricUser);
                            });
                        else
                            cb.onError(ctx.getString(R.string.error_msg_try_again));

                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(job.getMessage());
                    }
                });

    }

    public void firebaseId(String providertoken, GenricObjectCallback<GenricUser> cb, Map<String, String> extraProps) {

        EzUtils.setKey(Constants.KEY_PROVIDERTOKEN, providertoken, ctx);
        networkService.updateTokens(providertoken, providertoken);

        JSONObject jop = new JSONObject();
        try {

            String fcmToken = EzUtils.getKey("fcm_token", ctx);
            if (fcmToken != null) {
                jop.put("fcmToken", fcmToken);
            }
            FirebaseUser curUser = firebaseAuth.getCurrentUser();
            if(curUser!=null)
            {
                jop.put("id", curUser.getUid());
                jop.put("email",curUser.getEmail());
            }

            jop.put("fcmToken", fcmToken);

            for (Map.Entry<String, String> entry : extraProps.entrySet()) {
                jop.put(entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
        }

        networkService.callPost(Constants.u(API_USERS), jop, false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                GenricUser genricUser = EzUtils.js.fromJson(response.toString(), GenricUser.class);
                cb.onEntity(genricUser);
                GenericUserViewModel.getInstance().updateLocalAndNotify(ctx, genricUser);
            }

            @Override
            public void onFail(ANError job) {
                cb.onError(job.getMessage());
            }
        });


    }

    public static void refreshProviderToken(GenricDataCallback cb) {

        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser == null) {
            cb.onStart(null, -1);
            return;
        }
        mUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();
                        cb.onStart(idToken, 1);
                    } else {
                        cb.onStart(null, -1);
                    }
                });

    }

    public void getLoggedInUser(GenricObjectCallback<GenricUser> cb) {


        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {

            refreshProviderToken((token, status) -> {
                if (status == 1) {

                    EzUtils.setKey(Constants.KEY_PROVIDERTOKEN, token, ctx);
                    networkService.updateTokens(token, token);
                    firebaseId(token, new GenricObjectCallback<GenricUser>() {
                        @Override
                        public void onEntity(GenricUser data) {
                            GenericUserViewModel.getInstance().updateLocalAndNotify(ctx, data);
                            cb.onEntity(data);
                        }

                        @Override
                        public void onError(String message) {
                            cb.onError(message);
                        }
                    }, null);

                } else {
                    GenricUser nonFirebaseLoginUser = EzUtils.readUserData();
                    if (nonFirebaseLoginUser != null) {
                        emailPhoneLogin(nonFirebaseLoginUser.getEmail(), nonFirebaseLoginUser.getPassword(), new GenricObjectCallback<GenricUser>() {
                            @Override
                            public void onEntity(GenricUser data) {
                                if (data != null) {
                                    GenericUserViewModel.getInstance().updateLocalAndNotify(ctx, data);
                                    cb.onEntity(data);
                                    EzUtils.writeUserData(data, ctx);
                                } else {
                                    cb.onError("Invalid Credentials ! Please login again .");
                                }
                            }

                            @Override
                            public void onError(String message) {
                                cb.onError("An unexpected error occurred . " + message);
                            }
                        });
                    } else
                        cb.onError("No token found");
                }
            });


        });
    }

    public void verifyPhoneNumberWithCode(AccountActivity act, String verificationId, String code, GenricObjectCallback<Task<AuthResult>> onVerificatoinComplete
    ) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(act, credential, onVerificatoinComplete);
    }

    public void resendVerificationCode(String phoneNumber,
                                       long timeout,
                                       PhoneAuthProvider.ForceResendingToken token
            , PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(timeout, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(ctx)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(token)     // ForceResendingToken from callbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void signInWithPhoneAuthCredential(AccountActivity act, PhoneAuthCredential credential, GenricObjectCallback<Task<AuthResult>> onVerificatoinComplete) {
        FirebaseUser gloginUser = firebaseAuth.getCurrentUser();
        if (gloginUser != null) {
            gloginUser.linkWithCredential(credential).addOnCompleteListener(ctx, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        onVerificatoinComplete.onEntity(task);
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            onVerificatoinComplete.onError(ResourceUtils.getString(R.string.invalidotp));
                        }
                        //com.google.firebase.FirebaseException: User has already been linked to the given provider.
                        else if (task.getException() instanceof FirebaseException
                                && task.getException().getMessage().contains("User has already been linked to the given provider.")) {
                            onVerificatoinComplete.onEntity(task);
                        } else {
                            onVerificatoinComplete.onError(task.getException().getMessage());
                        }
                    }
                }
            });
        } else
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(act, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                onVerificatoinComplete.onEntity(task);
                            } else {
                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    onVerificatoinComplete.onError(ResourceUtils.getString(R.string.invalidotp));
                                } else {
                                    onVerificatoinComplete.onError(task.getException().getMessage());
                                }
                            }
                        }
                    });
    }

    public void sendOTP(String phoneNumber, long timeout, PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks) {

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(timeout, TimeUnit.SECONDS)
                        .setActivity(ctx)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void commitTemporaryUserToServer(GenricObjectCallback<GenricUser> cb) {

        String fcmToken = EzUtils.getKey("fcm_token", ctx);
        if (fcmToken != null) {
            getTempGenricUser().setFcmToken(fcmToken);
        }

        networkService.callPost(Constants.u(API_USERS), getTempGenricUser(), false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                GenricUser genricUser = EzUtils.js.fromJson(response.toString(), GenricUser.class);
                cb.onEntity(genricUser);
                GenericUserViewModel.getInstance().updateLocalAndNotify(ctx, genricUser);
            }

            @Override
            public void onFail(ANError job) {
                cb.onError(job.getMessage());
            }
        });
    }

    public void commitPasswordAndPhone(String oldPaswd, String newPaswd,
                                       String phone,
                                       GenricObjectCallback<GenricUser> cb) {


        JSONObject jop = new JSONObject();
        try {

            FirebaseUser curUser = firebaseAuth.getCurrentUser();
            if(curUser!=null)
            {
                jop.put("id", curUser.getUid());
                jop.put("email",curUser.getEmail());
            }
            if (oldPaswd != null && newPaswd != null) {
                jop.put("password", oldPaswd);
                jop.put("newpassword", newPaswd);
            } else if (phone != null) {
                jop.put("newphone", phone);
            } else {
                cb.onError(ctx.getString(R.string.invalidinput));
            }
            String fcmToken = EzUtils.getKey("fcm_token", ctx);
            if (fcmToken != null) {
                jop.put("fcmToken", fcmToken);
            }
        } catch (Exception e) {
        }

        networkService.callPost(Constants.u(API_USERS) + "/" +
                        getTempGenricUser().getId()
                , jop, false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        GenricUser genricUser = EzUtils.js.fromJson(response.toString(), GenricUser.class);
                        cb.onEntity(genricUser);
                        if (genricUser != null && !EzUtils.isEmpty(genricUser.getPassword()) && firebaseAuth.getCurrentUser() != null)
                            firebaseAuth.getCurrentUser().updatePassword(genricUser.getPassword()).addOnCompleteListener((task)->{
                                EzUtils.l("LoginService","Firebase Password Updated !");
                            });

                        GenericUserViewModel.getInstance().updateLocalAndNotify(ctx.getApplicationContext(), genricUser);
                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(job.getMessage());
                    }
                });
    }

    public void checkPhoneExists(String phoneNumber, GenricDataCallback genricDataCallback) {


        networkService.callGet(Constants.u(Constants.API_CHECK_PHONE) + "?phone=" + URLEncoder.encode(phoneNumber)
                , false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {

                        if (response.optBoolean("success")) {
                            genricDataCallback.onStart(response.optString("message"), 1);
                        } else {
                            genricDataCallback.onStart(response.optString("message"), -1);

                        }

                    }

                    @Override
                    public void onFail(ANError job) {
                        genricDataCallback.onStart(null, -1);
                    }
                });


    }

    public void sendPasswordResetMail(String emailPhone, GenricDataCallback cb) {

        String suffix = "";
        if (emailPhone.contains("@")) {
            suffix = "email=" + URLEncoder.encode(emailPhone);
        } else if (EzUtils.isValidMobile(emailPhone)) {
            String ccode = ResourceUtils.getString(R.string.ccode);
            if (!emailPhone.contains(ccode))
                emailPhone = ccode + emailPhone;
            suffix = "phone=" + URLEncoder.encode(emailPhone);
        }
        networkService.callGet(Constants.u(Constants.API_RESET_PASSWORD) + "?" + suffix
                , false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {

                        if (response.optBoolean("success")) {
                            cb.onStart(response.optString("message"), 1);
                        } else {
                            cb.onStart(response.optString("message"), -1);

                        }

                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onStart(null, -1);
                    }
                });


    }
}
