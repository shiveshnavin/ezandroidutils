package com.semibit.ezandroidutils.ui;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.androidnetworking.error.ANError;
import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.BuildConfig;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.binding.GenericUserViewModel;
import  com.semibit.ezandroidutils.interfaces.API;
import  com.semibit.ezandroidutils.interfaces.GenricDataCallback;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkRequestCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkService;
import  com.semibit.ezandroidutils.models.ActionItem;
import  com.semibit.ezandroidutils.models.DeviceInfo;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.services.AndroidNetworkService;
import  com.semibit.ezandroidutils.services.CacheService;
import  com.semibit.ezandroidutils.services.CrashReporter;
import  com.semibit.ezandroidutils.services.DBService;
import  com.semibit.ezandroidutils.services.DownloadOpenService;
import  com.semibit.ezandroidutils.services.EventBusService;
import  com.semibit.ezandroidutils.services.InAppNavService;
import  com.semibit.ezandroidutils.services.RestAPI;
import  com.semibit.ezandroidutils.ui.activities.HomeActivity;
import  com.semibit.ezandroidutils.ui.activities.SplashActivity;
import  com.semibit.ezandroidutils.utils.FadePopup;
import  com.semibit.ezandroidutils.utils.TextAndContentPicker;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.squareup.picasso.Picasso;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by shivesh on 7/1/18.
 */

public abstract class BaseActivity extends AppCompatActivity {

    public static String TAG = "BaseApp";
    public static String accessToken;
    public static DBService databaseHelper;
    public static FirebaseAnalytics mfbanalytics;
    public static FirebaseRemoteConfig mFirebaseRemoteConfig;
    public static API restApi;
    public static NetworkService netService;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public FirebaseAuth mAuth;
    public Context ctx;
    public BaseActivity act;
    public ImageView logo;
    public GenricUser user;
    public View list;
    public Dialog lastDialog = null;
    public InAppNavService inAppNavService;
    public FragmentManager fragmentManager;
    boolean isActivityAlive = false;
    androidx.appcompat.widget.Toolbar toolbar;
    TextView titl;
    View loading;
    boolean wasLoggedOut = false;
    Menu mMenu;
    ProgressDialog pd;
    TextAndContentPicker picker;
    ProgressDialog hideShow;

    @NonNull
    public static String getFirebaseToken(boolean ignoreExpiry) {

        String toknjstr = EzUtils.getKey("fb_token", App.getAppContext());
        try {
            JSONObject jo = new JSONObject(toknjstr);
            long expiry = jo.getLong("expiry");
            if(expiry < System.currentTimeMillis())
                refreshApiKeyFromRemoteConfig();
            if (!ignoreExpiry && expiry < System.currentTimeMillis())
                return "";
            return jo.getString("token");
        } catch (Exception e) {
            EzUtils.e("fb_token not present in cache");
            return "";
        }

    }

    public static String refreshApiKeyFromRemoteConfig() {
        try {

            String jstr = mFirebaseRemoteConfig.getString("access_token");

            if(jstr==null || jstr.length()<5){
                mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if(mFirebaseRemoteConfig.getString("access_token")!=null)
                         refreshApiKeyFromRemoteConfig();
                    }
                });
                return null;
            }
            if (jstr == null || jstr.length() < 2) {
                jstr = EzUtils.getKey("access_token", App.getAppContext());
            }
            JSONArray jsonArray = new JSONArray(jstr);
            for (int i = 0; i < jsonArray.length(); i++) {
                accessToken = jsonArray.getString(i);
                break;
            }
            EzUtils.setKey("access_token", jstr, App.getAppContext());
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return EzUtils.requireNotNull(accessToken);
    }

    public void setUpToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_ham);
        logo = findViewById(R.id.logo);
        logo.setOnClickListener(v -> {
            finish();
        });

    }

    @Override
    public void setTitle(CharSequence title) {

        super.setTitle("");
        View tool_cont = findViewById(R.id.tool_cont);
        if (tool_cont != null) {
            titl = tool_cont.findViewById(R.id.title);
            titl.setText(title);

        }


    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        mAuth = FirebaseAuth.getInstance();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        fragmentManager = getSupportFragmentManager();
        inAppNavService = new InAppNavService(this);
        super.onCreate(savedInstanceState);

        try {
            if (EzUtils.getUser() != null && EzUtils.readUserData() != null) {

                user = EzUtils.readUserData();
            }
        } catch (Exception e) {
            if (EzUtils.DEBUG_ENABLED) e.printStackTrace();
        }

        ctx = this;
        act = this;
        picker = new TextAndContentPicker(this, null);

        if (databaseHelper == null)
            databaseHelper = DBService.getInstance(getApplicationContext());
        if (mfbanalytics == null)
            mfbanalytics = FirebaseAnalytics.getInstance(getApplicationContext());
        if (netService == null)
            netService = AndroidNetworkService.getInstance(getApplicationContext());
        if (netService != null)
        {
            accessToken = refreshApiKeyFromRemoteConfig();
            netService.setAccessToken(accessToken);
        }
        if (restApi == null)
            restApi = RestAPI.getInstance(getApplicationContext());


        isActivityAlive = true;


        //EzUtils.e("access",accessToken);
    }

    @Override
    protected void onDestroy() {

        if (lastDialog != null && lastDialog.isShowing())
            lastDialog.dismiss();
        try {
            CacheService.getInstance().dumpCache();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public void startLogout() {
        EzUtils.logout();
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        finishAffinity();
        startActivity(intent);
    }

    public void startHome() {
        startHome(new Intent());
    }
    public void startHome(Intent intent) {

        Intent itt = new Intent(ctx, HomeActivity.class);
        if(intent.hasExtra("action"))
        itt.putExtra("action",intent.getStringExtra("action"));
        startActivity(itt);
        finish();
        GenericUserViewModel.getInstance().onlyIfPresent(new GenricObjectCallback<GenricUser>() {
            @Override
            public void onEntity(GenricUser data) {
                FirebaseCrashlytics.getInstance().setUserId(data.getId());
            }
        });

//
//        user = EzUtils.readUserData();
//        user = EzUtils.readUserData();
//
//
//        if (processDeepLink() == null) {
//            startActivity(new Intent(ctx, HomeActivity.class));
//            finishAffinity();
//        }
    }

    ActionItem processDeepLink() {
        Intent intent = getIntent();
        Uri data = intent.getData();
        EzUtils.e("DeepLink", "Checking Data" + data);
        getIntent().setData(null);
        if (data == null || data.getPathSegments() == null)
            return null;
        List<String> params = data.getPathSegments();
        ActionItem item = new ActionItem();
        item.act = this;

        if (params.get(0).equals("home")) {
            item.actionType = Constants.ACTION_HOME;
            item.dataId = params.get(1);
        }

        EventBusService.getInstance().doActionItem(item);
        return item;
    }

    private void updateUser(JSONObject jop) {

    }

    public void setFirebaseToken(String token) {

        try {
            JSONObject jo = new JSONObject();
            long expiry = System.currentTimeMillis()
                    + (mFirebaseRemoteConfig.getLong("firebase_token_expiry"));

            jo.put("token", token);
            jo.put("expiry", expiry);
            EzUtils.setKey("fb_token", jo.toString(), act);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateFcm() {

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    EzUtils.setKey("fcm_token",token,ctx);
                    GenricUser curUser =
                    GenericUserViewModel.getInstance()
                            .getUser().getValue();
                    if(curUser!=null){
                        curUser.setFcmToken(token);
                    }
                });
    }

    public int getcolor(@ColorRes int r) {
        return getResources().getColor(r);
    }

    public Drawable getdrawable(@DrawableRes int r) {
        return getResources().getDrawable(r);
    }

    public String getstring(@StringRes int res) {


        return getResources().getString(res);

    }

    public void downloadAndOpenFile(String url, String filename) {
        DownloadOpenService openFile = new DownloadOpenService();
        openFile.downloadFile(this, url, filename);
    }


    public void showPD(boolean show) {

        if (hideShow == null) {
            hideShow = new ProgressDialog(ctx);
            hideShow.setMessage("Loading...");
        }
        if (show)
            hideShow.show();
        else
            hideShow.dismiss();


    }

    public void loadStart() {
        load(true);
    }

    public void loadStop() {
        load(false);
    }

    public void load(boolean isloading) {

        if (loading == null)
            loading = findViewById(R.id.loading);
        if (list == null)
            list = findViewById(R.id.list);

        if (loading == null || list == null)
            return;


        if (isloading) {

            loading.setAlpha(1.0f);
            list.setVisibility(View.GONE);

        } else {

            loading.setAlpha(0.0f);
            list.setVisibility(View.VISIBLE);

        }

    }

    private AnimationDrawable getProgressBarAnimation() {

        GradientDrawable rainbow1 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW});

        GradientDrawable rainbow2 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.YELLOW, Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN});

        GradientDrawable rainbow3 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN});

        GradientDrawable rainbow4 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA, Color.BLUE});

        GradientDrawable rainbow5 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA});

        GradientDrawable rainbow6 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED});


        GradientDrawable[] gds = new GradientDrawable[]{rainbow1, rainbow2, rainbow3, rainbow4, rainbow5, rainbow6};

        AnimationDrawable animation = new AnimationDrawable();

        for (GradientDrawable gd : gds) {
            animation.addFrame(gd, 100);

        }

        animation.setOneShot(false);

        return animation;


    }

    public void addPressReleaseAnimation(@NonNull final View base) {

        final Animation press = AnimationUtils.loadAnimation(base.getContext(), R.anim.rec_zoom_out);
        final Animation release = AnimationUtils.loadAnimation(base.getContext(), R.anim.rec_zoom_nomal);

        base.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        base.startAnimation(press);
                        break;
                    case MotionEvent.ACTION_UP:

                        base.startAnimation(release);

                    case MotionEvent.ACTION_CANCEL:

                        base.startAnimation(release);


                        break;
                    default:
                        break;
                }


                return false;
            }
        });


    }


    public void showDrawer() {


        View navIcon = toolbar.getChildAt(1);

        FadePopup popup = new FadePopup(this, navIcon, null);

        popup.popup();
        LinearLayout cont = popup.getContainer();

        PopupMenu p = new PopupMenu(this, null);
        Menu menu = p.getMenu();
        getMenuInflater().inflate(R.menu.menu_drawer, menu);

        int pad = EzUtils.pxFromDp(ctx, 30).intValue();
        cont.setPadding(pad, pad, pad, pad);


        int i = 0;
        while (i < menu.size()) {
            MenuItem mi = menu.getItem(i++);

            final View row = getLayoutInflater().inflate(R.layout.utl_row_menu, null);
            final TextView tv = row.findViewById(R.id.txt);
            tv.setId(R.id.txt + 100);
            ImageView im = row.findViewById(R.id.img);
            row.setId(mi.getItemId());
            row.setTag(mi.getTitle());
            tv.setTextSize(24);


            tv.setText(mi.getTitle());
            im.setImageDrawable(mi.getIcon());

            row.setOnClickListener((v) -> {

                EzUtils.e("ID ", v.getId() + " tag " + v.getTag());
                Intent mIntent = null;
                switch (v.getId()) {
                    case R.id.menu_home:

                        mIntent = new Intent(ctx, HomeActivity.class);
                        finishAffinity();

                        break;
                    case R.id.notifications:


//                        showNotifications();
                        // mIntent = new Intent(ctx,CartActivity.class);

                        break;
                    case R.id.menu_account:

                        inAppNavService.starMyAccount();
                        // mIntent = new Intent(ctx,MyAccount.class);

                        break;
                    case R.id.menu_about:

                        showAbout();

                        // mIntent = new Intent(ctx,AboutUs.class);

                        break;
                    case R.id.menu_logout:

                        startLogout();
                        break;

                }

                if (mIntent != null)
                    startActivity(mIntent);
                popup.dismiss();


            });

            cont.setOrientation(LinearLayout.VERTICAL);
            cont.setGravity(Gravity.CENTER);

            cont.addView(row);

        }


    }

    public void showAbout() {

        String text = getstring(R.string.about);
//       String text2= AndroidNetworkService.convertWithIteration(AndroidNetworkService.apiUsage);
//
//        EzUtils.e("usage","--------------------");
//        EzUtils.e("usage",text2);
//        EzUtils.e("usage","--------------------");

        lastDialog = EzUtils.diagInfo2(ctx, text, "Feedback", R.drawable.logo_inv, new EzUtils.ClickCallBack() {
            @Override
            public void done(DialogInterface dialogInterface) {

                ShareCompat.IntentBuilder.from(act)
                        .setType("message/rfc822")
                        .addEmailTo(mFirebaseRemoteConfig.getString("email"))
                        .setSubject("Feedback")
                        .setText("Hey ! I want to give feedback about app .")
                        .setChooserTitle("Send E-Mail")
                        .startChooser();


            }
        });


    }

    public void showHelp(String help_key) {

        if (EzUtils.getKey(help_key, ctx) == null) {
            String text = "";

            if (help_key.equals("signup")) {
                text = getstring(R.string.help_signup);
            }

            EzUtils.setKey(help_key, "dontshow", ctx);
            if (text.length() > 3) {
                lastDialog = EzUtils.diagInfo2(ctx, text, "Don't Show Again", R.drawable.logo_inv, new EzUtils.ClickCallBack() {
                    @Override
                    public void done(DialogInterface dialogInterface) {


                    }
                });
            }


        }


    }

    public void shareText(String tosend, String link) {
        try {
            tosend += " " + link + " ";
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        Intent sendintent = new Intent();
        sendintent.setAction(Intent.ACTION_SEND);
        sendintent.putExtra(Intent.EXTRA_TEXT, tosend);
        sendintent.setType("text/plain");
        startActivity(Intent.createChooser(sendintent, "Share"));


    }

    public void showOptions(String[] options, int[] icons, GenricDataCallback cb) {
        EzUtils.diagBottomMenu(ctx, "",
                options,
                icons,
                true, "CANCEL",
                cb);


    }

    public void broadCastNewMessage() {
        Intent intent2 = new Intent("NEW_MESSAGAGES");
        LocalBroadcastManager
                .getInstance(ctx.getApplicationContext()).sendBroadcast(intent2);

    }

    public void resourceNotAvailable() {
        lastDialog = EzUtils.diagInfo2(ctx, getString(R.string.resc_not_available_dialog),
                "Dismiss", R.drawable.ic_info_about_48dp, new EzUtils.ClickCallBack() {
                    @Override
                    public void done(DialogInterface dialogInterface) {

                    }
                });
    }

    public DeviceInfo getDeviceInfo() {
        String url = mFirebaseRemoteConfig.getString("location_api");
        String DeviceLang = Resources.getSystem().getConfiguration().locale.getLanguage();

        DeviceInfo dif = null;
        try {
            dif = EzUtils.js.fromJson(EzUtils.getKey("device_info", ctx), DeviceInfo.class);
        } catch (Exception e) {


        }
        if (dif == null)
            dif = new DeviceInfo();

        dif.lang = DeviceLang;

        DeviceInfo finalDif = dif;
        netService.callGet(url, false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    finalDif.country = response.optString("country");
                    finalDif.countryCode = response.optString("country");
                    finalDif.region = response.optString("region");
                    finalDif.latLng = "" + response.optString("loc");
                    EzUtils.setKey("device_info", EzUtils.js.toJson(finalDif), ctx);
                } catch (Exception e) {
                    EzUtils.setKey("device_info", EzUtils.js.toJson(finalDif), ctx);

                }
            }

            @Override
            public void onFail(ANError job) {

                String cCode = Locale.getDefault().getCountry();
                finalDif.countryCode = cCode;
                EzUtils.setKey("device_info", EzUtils.js.toJson(finalDif), ctx);
                EzUtils.e(job.getErrorBody());

            }
        });

        return dif;
    }

    public String getParamsData(String url) {
        return url.replace("${userid}", user.getId()).
                replace("${useralias}", user.getAlias());

    }

    public void pickDoc() {

        picker.pickDoc();
    }

    public void pickFile() {

        picker.pickFile();
    }

    public void pickImage() {

        picker.pickImage();
    }

    public void pickLocation() {

        picker.pickLocation();
    }

    public void uploadFile(String filename, File file, GenricDataCallback cb, boolean showPd) {

        if (showPd) {
            pd = new ProgressDialog(ctx);
            pd.setMessage(getString(R.string.uploading));
        }
        netService.uploadFile(filename, file, new GenricDataCallback() {
            @Override
            public void onStart(String data1, int data2) {
                if (showPd) {
                    pd.dismiss();
                }
                cb.onStart(data1, data2);
            }
        });

    }

    public boolean isNetworkAvailable() {
        Context context = act;
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public void viewImage(ImageView imgView, ArrayList<String> urls, int pos) {
        imgView.setOnClickListener((v) -> {

            ImageLoader<String> imageLoader = new ImageLoader<String>() {
                @Override
                public void loadImage(ImageView imageView, String image) {


                    if (image.startsWith("http"))
                        Picasso.get().load(image).
                                error(R.drawable.broken_image)
                                .placeholder(R.drawable.loading_bg)
                                .into(imageView);
                    else {

                        File f = new File(image);
                        Picasso.get().load(f).placeholder(R.drawable.loading_bg).
                                error(R.drawable.broken_image)
                                .into(imageView);
                    }


                }
            };

            StfalconImageViewer.Builder<String> builder
                    = new StfalconImageViewer.Builder<String>(ctx, urls,
                    imageLoader)
                    .withStartPosition(pos)
                    .withTransitionFrom(imgView);

            builder.build().show(true);

        });
    }

    protected void viewImage(String img) {

       /* DownloadManager.Request r = new DownloadManager.Request(Uri.parse(procuct.getImage()));
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "product_"+ procuct.getId()+".jpg");
        r.allowScanningByMediaScanner();
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(r);
       */
        EzUtils.toast(this, "Not Implemented : Use Stefecon Image Viwer");
    }

    public void viewImage(ImageView imgView, String url) {
        ArrayList<String> s = new ArrayList<>();
        s.add(url);
        viewImage(imgView, s, 0);
    }

    public String getClipped(String bodyText) {
        String bodyTextTo = bodyText;
        try {
            int mx = (int) act.mFirebaseRemoteConfig.getLong("read_more_length");
            if (bodyText.length() > mx) {

                bodyTextTo = bodyText.substring(0, mx) + "...read more";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bodyTextTo;
    }



    public boolean isShowing=false;

    @Override
    protected void onResume() {
        super.onResume();
        isShowing = true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        isShowing = false;

    }


    public void checkUpdate() {

        DownloadOpenService downloadOpenService = new DownloadOpenService();
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
        int versionCode = BuildConfig.VERSION_CODE;
        /*
        {
            "versionCode":1,
            "message":"Latest update!",
            "url":"http://download.com/app/apk.apk",
            "mandatory":true
        }
         */
        try {
            RestAPI.getInstance().checkUpdate(versionCode, new GenricObjectCallback<JSONObject>() {
                @Override
                public void onEntity(JSONObject response) {
                    int codeUpdated = response.optInt("versionCode", versionCode);
                    if(response.optBoolean("lock",false)){
                        EzUtils.toast(ctx, "Application is temporarily unavailable.");
                        finish();
                    }
                    if (codeUpdated > versionCode) {
                        Dialog d = EzUtils.diagInfo2(ctx, response.optString("message"),
                                "Update", R.drawable.ic_logo,
                                new EzUtils.ClickCallBack() {
                                    @Override
                                    public void done(DialogInterface dialogInterface) {
                                        try {

                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(response.optString("url")));
                                            startActivity(browserIntent);

    //                                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.REQUEST_INSTALL_PACKAGES) == PackageManager.PERMISSION_DENIED) {
    //                                            downloadOpenService.downloadFile(act, response.optString("url"), getString(R.string.app_name) + ".apk");
    //
    //                                        } else {
    //
    //                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(response.optString("url")));
    //                                            startActivity(browserIntent);
    //                                        }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                        d.setCancelable(!response.optBoolean("mandatory", false));
                    }
                }

                @Override
                public void onError(String message) {

                }
            });
        } catch (Exception e) {
            CrashReporter.reportException(e);
        }

    }

    public Point size;

    public boolean isSmallScreen(){
        try{
            if(size == null){
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                size = new Point();
                wm.getDefaultDisplay().getRealSize(size);
            }
            if(size.y < 1281){
                return true;
            }

        }catch (Exception e){
            CrashReporter.reportException(e);
        }
        return false;
    }
    public int getAdjustedSize(int originalSize){
        if(isSmallScreen())
            return (int)(originalSize / 1.5);
        return originalSize;
    }

}
