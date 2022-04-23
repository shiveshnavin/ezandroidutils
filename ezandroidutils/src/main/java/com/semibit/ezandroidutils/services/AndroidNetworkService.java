package com.semibit.ezandroidutils.services;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.binding.GenericUserViewModel;
import  com.semibit.ezandroidutils.interfaces.CacheUtil;
import  com.semibit.ezandroidutils.interfaces.GenricDataCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkRequestCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkService;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.utils.JWTUtils;
import  com.semibit.ezandroidutils.EzUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.semibit.ezandroidutils.BuildConfig;


public class AndroidNetworkService implements NetworkService {


    public static HashMap<String, Integer> apiUsage;
    Context act;
    String accessToken;
    GenricUser user;
    static String firebaseAuthToken = "";
    static String providerToken = "";
    String appVersionCode = "" + App.VERSION_CODE;
    CacheUtil cacheUtil;
    String authHeader = null;

    public static AndroidNetworkService instance;
    public static NetworkService getInstance(Context applicationContext) {
        if(instance == null){
            instance = new AndroidNetworkService(applicationContext);
        }
        return instance;
    }

    public AndroidNetworkService(Context act) {
        this.act = act;
        this.accessToken = EzUtils.requireNotNull(BaseActivity.accessToken);
        GenericUserViewModel.getInstance().getUser().observeForever(new Observer<GenricUser>() {
            @Override
            public void onChanged(GenricUser u) {
                user = u;
            }
        });
        cacheUtil = CacheService.getInstance();
        try {
            appVersionCode = "" + App.VERSION_CODE;
            firebaseAuthToken = BaseActivity.getFirebaseToken(true);
            if (EzUtils.DEBUG_ENABLED) {
                EzUtils.e("Network", " Firebase token " + firebaseAuthToken);
                EzUtils.e("Network", " ProviderToken token " + providerToken);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getProviderToken(){
        try {

            if (!Strings.isNullOrEmpty(EzUtils.getKey(Constants.KEY_PROVIDERTOKEN, act))) {
                providerToken = EzUtils.getKey(Constants.KEY_PROVIDERTOKEN, act);
            }
            if (Strings.isNullOrEmpty(providerToken)) {
                providerToken = "";
                return providerToken;
            }
            JSONObject jsonObject = new JSONObject(JWTUtils.decoded(providerToken));
            Date date = new Date(jsonObject.getLong("exp") * 1000);
            Date curr = new Date(System.currentTimeMillis() + 2 * 60000);
            if (date.before(curr)) {
                EzUtils.e("token expired in 5 mins. refreshing");
                BaseActivity.refreshProviderToken((tokenNew, statusCode) -> {
                    if (statusCode > -1) {
                        providerToken = tokenNew;
                        EzUtils.setKey(Constants.KEY_PROVIDERTOKEN, tokenNew, act);
                    }
                });
            }
        }catch (Exception e){
            CrashReporter.reportException(e);
        }
        return providerToken;
    }

    public String getBasicAuthHeader(){
        user = GenericUserViewModel.getInstance().getUser().getValue();
        if(user == null)
            user = EzUtils.readUserData();
        if(user!=null){
            authHeader = ""+user.getId()+":"+user.getPassword();
            authHeader = Base64.encodeToString(authHeader.getBytes(),Base64.DEFAULT).trim();
            authHeader = "Basic "+authHeader;
        }
        return authHeader;
    }

    public static String convertWithIteration(Map<String, Integer> map) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String key : map.keySet()) {
            mapAsString.append(key + " = " + map.get(key) + ",\n ");
        }
        mapAsString.delete(mapAsString.length() - 2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    @Override
    public void updateTokens(String googleToken, String firebaseToken) {
        if (googleToken != null)
            providerToken = googleToken;
        if (firebaseToken != null)
            firebaseAuthToken = firebaseToken;
    }

    public void recordUse(String url, String body) {
        if (apiUsage == null)
            apiUsage = new HashMap<>();
        String data = url + "--";
//        if(body!=null){
//            data+=(EzUtils.js.toJson(body));
//        }
        if (!apiUsage.containsKey(data)) {
            apiUsage.put(data, 1);
            return;
        }
        Integer count = apiUsage.get(data) + 1;
        apiUsage.put(data, count);
    }

    @Override
    public void callGetString(String url, final boolean showLoading, final NetworkRequestCallback call) {
        
        EzUtils.e("CallGET", url);
        if (cacheUtil.getFromCache(url, call)) {
            return;
        }
        recordUse(url, null);
        AndroidNetworking.get(url)
                .addHeaders(getAllHeaders())
                .build().getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String response) {
                if (EzUtils.DEBUG_ENABLED)
                    EzUtils.e("CallGET Resp", response);

                cacheUtil.putIntoCache(url, response);
                call.onSuccessString(response);

                
            }

            @Override
            public void onError(ANError anError) {
                
                EzUtils.e("CallGET", anError.getErrorDetail());
                EzUtils.e("CallGET", anError.getErrorBody());
                call.onFail(anError);
            }
        });
    }

    @Override
    public void invalidateAllRuntimeValues() {
        authHeader = null;
        user = null;
    }

    public Map<String, String> getAllHeaders() {

        HashMap<String, String> allHeaders = new HashMap<>();

        allHeaders.put("accesstoken", accessToken);

        try {
            String token = getProviderToken();
            if (!Strings.isNullOrEmpty(token)) {
                allHeaders.put("Authorization", "Bearer " + token);
            }
            if (user != null && !Strings.isNullOrEmpty(user.getPassword())) {
                String usernamePaswd = user.getId() + ":" + user.getPassword();
                String usernamePaswd64 = Base64.encodeToString(usernamePaswd.getBytes(), Base64.NO_WRAP);
                if(allHeaders.get("Authorization") == null)
                    allHeaders.put("Authorization", "Basic " + usernamePaswd64);
                allHeaders.put("UAuthorization", "Basic " + usernamePaswd64);

            }
            allHeaders.put("firebasetoken", firebaseAuthToken);
            allHeaders.put(Constants.KEY_PROVIDERTOKEN, providerToken);
            allHeaders.put("version", appVersionCode);
            String userId = "";
            if(user!=null && !Strings.isNullOrEmpty(user.getId())){
                allHeaders.put("userid", (user == null ? "" : user.getId()));
            }
            else {
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if(firebaseUser!=null){
                    allHeaders.put("userid", firebaseUser.getUid());
                }
            }
        } catch (Exception e) {
            CrashReporter.reportException(e);
        }

        return allHeaders;
    }

    @Override
    public void callGet(String url, final boolean showLoading, final NetworkRequestCallback call) {
        
        EzUtils.e("CallGET", url);

        if (cacheUtil.getFromCache(url, call)) {
            return;
        }

        recordUse(url, null);
        AndroidNetworking.get(url)
                .addHeaders(getAllHeaders())
                .build().getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String response) {
                if (EzUtils.DEBUG_ENABLED)
                    EzUtils.e("CallGET Resp", response);

                try {
                    cacheUtil.putIntoCache(url, response);
                    call.onSuccess(new JSONObject(response));

                } catch (JSONException e) {
                    //EzUtils.e("CallGet", "Error parsing Jsonobj , found JSOn array");
                    // e.printStackTrace();
                }
                call.onSuccessString(response);
                
            }

            @Override
            public void onError(ANError anError) {
                
                EzUtils.e("CallGET", anError.getErrorDetail());
                EzUtils.e("CallGET", anError.getErrorBody());
                call.onFail(anError);
            }
        });
    }

    @Override
    public void callPost(String url, final boolean showLoading, final NetworkRequestCallback call) {

        callPost(url, new JSONObject(), showLoading, call);

    }

    @Override
    public void callPost(String url, Object body, final boolean showLoading, final NetworkRequestCallback call) {

        try {
            callPost(url, new JSONObject(EzUtils.js.toJson(body)), showLoading, call);
        } catch (JSONException e) {
            if (EzUtils.DEBUG_ENABLED) e.printStackTrace();
        }

    }


    @Override
    public void callPostString(String url, JSONObject body, final boolean showLoading, final NetworkRequestCallback call) {

        


        if (EzUtils.DEBUG_ENABLED) {
            EzUtils.e("CallPost", url);
            EzUtils.e("CallPost", body.toString());
        }

        if (cacheUtil.getFromCache(url, body, call)) {
            return;
        }


        recordUse(url, null);
        AndroidNetworking.post(url)
                .addHeaders(getAllHeaders())
                .addJSONObjectBody(body).build().getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String response) {
                if (EzUtils.DEBUG_ENABLED)
                    EzUtils.e("CallPost Resp", response);
                cacheUtil.putIntoCache(url, body, response);
                call.onSuccessString(response);

                
            }

            @Override
            public void onError(ANError anError) {
                
                EzUtils.e("CallGET", anError.getErrorDetail());
                EzUtils.e("CallGET", anError.getErrorBody());
                call.onFail(anError);
            }
        });
    }

    @Override
    public void callPost(String url, @NonNull JSONObject body, final boolean showLoading, final NetworkRequestCallback call) {

        


        if (EzUtils.DEBUG_ENABLED) {
            EzUtils.e("CallPost", url);
            EzUtils.e("CallPost", body.toString());
        }


        if (cacheUtil.getFromCache(url, body, call)) {
            return;
        }

        recordUse(url, body.toString());
        AndroidNetworking.post(url)
                .addHeaders(getAllHeaders())
                .addJSONObjectBody(body).build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        if (EzUtils.DEBUG_ENABLED)
                            EzUtils.e("CallPost Resp", response);

                        try {
                            call.onSuccess(new JSONObject(response));
                            cacheUtil.putIntoCache(url, body, response);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        call.onSuccessString(response);

                        
                    }

                    @Override
                    public void onError(ANError anError) {
                        
                        EzUtils.e("CallPost", anError.getErrorDetail());
                        EzUtils.e("CallPost", anError.getErrorBody());
                        call.onFail(anError);
                    }
                });
    }


    public void uploadFile(String filename, File file, GenricDataCallback cb) {


        EzUtils.e("CallPost Upload", file.length());
        AndroidNetworking.upload(Constants.HOST + Constants.API_UPLOAD_IMAGE)
                .addHeaders(getAllHeaders())
                .addMultipartFile("verifdoc", file)
                .addMultipartParameter("userid", user.getId())
                .addMultipartParameter("prefix", "" + filename)
                .addMultipartParameter("filename", "" + filename)
                .setTag("uploadTest")
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                    }
                })
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {

                        EzUtils.e(response.optString("url"));
                        if (response.optString("url").equals("")) {
                            cb.onStart(null, -1);
                            return;
                        }
                        cb.onStart(response.optString("url"), 1);

                    }

                    @Override
                    public void onError(ANError error) {
                        EzUtils.toast(act, act.getString(R.string.upload_failed));
                        cb.onStart(null, -1);
                    }
                });

    }

    @Nullable
    @Override
    public void setAccessToken(String token) {
        accessToken = token;

    }

}
