package com.semibit.ezandroidutils.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.androidnetworking.error.ANError;
import com.google.common.base.Strings;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;
import com.paytm.pgsdk.TransactionManager;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkRequestCallback;
import  com.semibit.ezandroidutils.services.CrashReporter;
import  com.semibit.ezandroidutils.services.RestAPI;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.utils.ShowHideLoader;
import  com.semibit.ezandroidutils.EzUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PaytmPaymentActivity extends BaseActivity {

    public void loading() {
        try {
            findViewById(R.id.cont_load_block).setVisibility(View.VISIBLE);
            ImageView animLogo = (ImageView) findViewById(R.id.animLogo);
            EzUtils.animate_avd(animLogo);
        } catch (Exception ignored) {
        }
    }


    public void loaded() {
        try {
            findViewById(R.id.cont_load_block).setVisibility(View.GONE);
            ImageView animLogo = (ImageView) findViewById(R.id.animLogo);
            EzUtils.animate_avd(animLogo);
        } catch (Exception ignored) {
        }
    }

    public static final int REQUEST_SELECT_FILE = 100;
    public static final int REQUEST_PAYTM_PAYMENT = 139;
    private final static int FILECHOOSER_RESULTCODE = 1;
    public ValueCallback<Uri[]> uploadMessage;
    String title = "";

    String orderId;
    Timer timer = new Timer();
    boolean completed = false;
    boolean isProcessing = false;
    private WebView mWebView;
    private ValueCallback<Uri> mUploadMessage;

    ShowHideLoader showHideLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);

        title = getIntent().getStringExtra("title");
        orderId = getIntent().getStringExtra("orderId");

        String TXN_AMOUNT = getIntent().getStringExtra("TXN_AMOUNT");
        String MID = getIntent().getStringExtra("MID");
        String TOKEN = getIntent().getStringExtra("TOKEN");
        String CALLBACK_URL = getIntent().getStringExtra("CALLBACK_URL");
//
//        if(true){
//
//            title = "Test";
//
//             TXN_AMOUNT = "10";
//             MID = "mGkXCb73842366174007";
//
//             TOKEN = "028cf2a174bb4b22afa89d1722b0647b1645865800196";
//            orderId =  "pay_uuvmP3p28254";
//
//            CALLBACK_URL = "http://192.168.0.117:8080/pay/callback?order_id="+orderId;
//        }

//        setTitle(title);
//        setUpToolbar();

        if (!Strings.isNullOrEmpty(TXN_AMOUNT) && !Strings.isNullOrEmpty(MID) && !Strings.isNullOrEmpty(CALLBACK_URL) && !Strings.isNullOrEmpty(TOKEN)) {
            HashMap<String, String> payMap = new HashMap<>();
            payMap.put("ORDER_ID", orderId);
            payMap.put("MID", MID);
            payMap.put("TXN_AMOUNT", TXN_AMOUNT);
            payMap.put("CALLBACK_URL", CALLBACK_URL);

            createOrder(toString(), payMap);

        } else {
            try {
                loading();

                String url = getIntent().getStringExtra("url");
                showHideLoader = ShowHideLoader.create().loader(findViewById(R.id.loader)).content(new View(this));
                showHideLoader.loading();
                netService.callGet(url, false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccessString(String response) {
                        showHideLoader.loaded();
                        try {
                            HashMap<String, String> body = parseInitHtmlForm(response, url);
                            String urlNew = url + body.get("action");
                            retrievePaymentToken(body, urlNew);
                        } catch (Exception e) {

                            CrashReporter.reportException(e);
                            showError("");
                        }
                    }

                    @Override
                    public void onFail(ANError job) {
                        showHideLoader.loaded();
                        showError(job.getMessage());
                    }
                });
            } catch (Exception e) {
                CrashReporter.reportException(e);
                showError("");
            }
        }

    }

    public void retrievePaymentToken(HashMap<String, String> body, String urlNew) {
        showHideLoader.loading();
        netService.callPost(urlNew, body, false, new NetworkRequestCallback() {
            @Override
            public void onSuccessString(String response) {
                try {
                    showHideLoader.loaded();
                    HashMap<String, String> map = getPaymentToken(response, urlNew);
                    String logoUrl = mFirebaseRemoteConfig.getString("logo_url");
                    if (map == null) {
                        showError("");
                        return;
                    }
                    createOrder(map.get("payment_token_id"), body);
                } catch (Exception e) {
                    CrashReporter.reportException(e);
                    showError("");
                }
            }

            @Override
            public void onFail(ANError job) {
                showHideLoader.loaded();
                showError(job.getMessage());
            }
        });
    }

    public static HashMap<String, String> getPaymentToken(String htmlString, String url) {
        Document doc = Jsoup.parse(htmlString, url);
        Elements links = doc.getElementsByTag("script");
        Element dataScript = null;
        for (Element script : links) {
            if (script.data().contains("/* update token value */")) {
                dataScript = script;
                break;
            }
        }
        HashMap<String, String> jsonObject = new HashMap();
        if (dataScript == null)
            return null;
        String jsSource = dataScript.data();

        Pattern pattern = Pattern.compile("(?<=token\":\\s\")(.*)(?=\")");
        Matcher ptokenRegex = pattern.matcher(jsSource);
        while (ptokenRegex.find()) {
            jsonObject.put("payment_token_id", ptokenRegex.group());
        }

        return jsonObject;
    }

    public static HashMap<String, String> parseInitHtmlForm(String htmlString, String url) throws JSONException {
        Document doc = Jsoup.parse(htmlString, url);
        Elements links = doc.getElementsByTag("form");
        Element form = links.get(0);
        String action = form.attr("action");
        HashMap<String, String> jsonObject = new HashMap();
        jsonObject.put("_action", action);
        Queue<Element> parse = new ArrayDeque<>(form.children());
        while (parse.size() > 0) {
            Element link = parse.remove();
            String type = link.tag().getName();
            if (type.equals("input")) {
                String name = link.attr("name");
                String value = link.attr("value");
                jsonObject.put(name, value);
            } else {
                parse.addAll(link.children());
            }
        }
        return jsonObject;
    }


    private boolean isOrderStarted = false;
    public void createOrder(String txnToken, HashMap<String, String> body) {



        new Handler(Looper.myLooper()).postDelayed(() -> {
            if(!isOrderStarted){
                isOrderStarted = true;
                loaded();
            }
        }, 3000);
        PaytmOrder paytmOrder = new PaytmOrder(body.get("ORDER_ID"), body.get("MID"), txnToken, body.get("TXN_AMOUNT"), body.get("CALLBACK_URL"));
        TransactionManager transactionManager = new TransactionManager(paytmOrder, new PaytmPaymentTransactionCallback() {
            @Override
            public void onTransactionResponse(@Nullable Bundle bundle) {
                EzUtils.log(bundle);
                startCheck();
            }

            @Override
            public void networkNotAvailable() {
                showError(getString(R.string.no_network));
            }

            @Override
            public void onErrorProceed(String s) {
                showError(s);
            }

            @Override
            public void clientAuthenticationFailed(String s) {
                showError(s);
            }

            @Override
            public void someUIErrorOccurred(String s) {
                showError(s);

            }

            @Override
            public void onErrorLoadingWebPage(int i, String s, String s1) {
                showError(getString(R.string.error_msg_try_again));
            }

            @Override
            public void onBackPressedCancelTransaction() {
                showError(getString(R.string.error_msg_try_again));
            }

            @Override
            public void onTransactionCancel(String s, Bundle bundle) {
                showError(s);
            }
        });
        transactionManager.setEnableAssist(true);
//        transactionManager.setAppInvokeEnabled(false);
        //transactionManager.startTransactionAfterCheckingLoginStatus(this, clientId, requestCode);
        transactionManager.startTransaction(act, REQUEST_PAYTM_PAYMENT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PAYTM_PAYMENT && data != null) {
            EzUtils.log(data);
//            Toast.makeText(this, data.getStringExtra("nativeSdkForMerchantMessage") + data.getStringExtra("response"), Toast.LENGTH_SHORT).show();
            startCheck();
        } else {
            showError("");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web, menu);
        return true;
    }

    boolean finishOnBack = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (mWebView.canGoBack()) {
                mWebView.goBack();
            } else if (orderId != null) {
                EzUtils.diagBottom(ctx, "", getString(R.string.are_you_sure_back), true, getString(R.string.confirm), () -> {
                    completed = true;
                    clearTimer();
                    setResult(Activity.RESULT_CANCELED, new Intent());
                    finish();
                });
            } else {
                super.onBackPressed();
            }

        } else if (id == R.id.reload) {
            mWebView.reload();
        }

        return super.onOptionsItemSelected(item);
    }

    public void clearTimer() {
        try {
            timer.cancel();
        } catch (Exception e) {

        }
    }

    public void startCheck() {
        loading();
        if (orderId != null) {

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {

                    if (!isProcessing) {
                        isProcessing = true;
                        restApi.checkTransaction(orderId, new GenricObjectCallback<JSONObject>() {
                            @Override
                            public void onEntity(JSONObject data) {
//                                EzUtils.toast(ctx, data.toString());
                                isProcessing = false;
                                String status = data.optString("status");
                                if (status.equals(Constants.TXN_SUCCESS) || status.equals("paid")) {
                                    loaded();
                                    completed = true;
                                    clearTimer();
                                    Intent it = new Intent();
                                    it.putExtra("data", data.toString());
                                    it.putExtra("amount", data.optString("amount"));
                                    it.putExtra("id", data.optString("id"));
                                    String timeStamp = data.optString("timeStamp");
                                    if (Strings.isNullOrEmpty(timeStamp)) {
                                        timeStamp = data.optString("created_at");
                                    }
                                    if (Strings.isNullOrEmpty(timeStamp)) {
                                        timeStamp = data.optString("time");
                                    }
                                    it.putExtra("timeStamp", timeStamp);
                                    setResult(Activity.RESULT_OK, it);
                                    RestAPI.getInstance().invalidateCacheWalletAndTxns();
                                    WalletViewModel.getInstance().refresh(null);
                                    new Handler(Looper.getMainLooper())
                                            .postDelayed(() -> {
                                                WalletViewModel.getInstance().refresh(null);
                                            }, 5000);
                                    finish();
                                    AnalyticsReporter.getInstance().logPurchase(data.optLong("amount"), data.optString("id"));

                                } else if (status.equals(Constants.TXN_FAILURE)) {
                                    String reason = "";
                                    try {
                                        JSONObject extra = new JSONObject(data.optString("extra"));
                                        reason = extra.optString("RESPMSG");
                                    } catch (Exception e) {
                                    }
                                    showError(getString(R.string.txn_failed) + "\n\n" + reason);
                                } else if (!status.equals(Constants.TXN_INITIATED)) {
                                    completed = true;
                                    clearTimer();
                                    Intent it = new Intent();
                                    it.putExtra("data", data.toString());
                                    it.putExtra("amount", data.optString("amount"));
                                    it.putExtra("id", data.optString("id"));
                                    String timeStamp = data.optString("timeStamp");
                                    if (Strings.isNullOrEmpty(timeStamp)) {
                                        timeStamp = data.optString("created_at");
                                    }
                                    if (Strings.isNullOrEmpty(timeStamp)) {
                                        timeStamp = data.optString("time");
                                    }


                                    it.putExtra("timeStamp", timeStamp);
                                    setResult(Activity.RESULT_FIRST_USER, it);
                                    RestAPI.getInstance().invalidateCacheWalletAndTxns();
                                    WalletViewModel.getInstance().refresh(null);
                                    new Handler(Looper.getMainLooper())
                                            .postDelayed(() -> {
                                                WalletViewModel.getInstance().refresh(null);
                                            }, 3000);
                                    finish();
                                    AnalyticsReporter.getInstance().logPurchase(data.optLong("amount"), data.optString("id"));

                                }

                            }

                            @Override
                            public void onError(String message) {
                                isProcessing = false;
                                showError(message != null ? message : "");
                            }
                        });

                    }
                }
            };
            findViewById(R.id.cont_load_block).setVisibility(View.VISIBLE);
            ImageView animLogo = (ImageView) findViewById(R.id.animLogo);
            EzUtils.animate_avd(animLogo);
            timer.scheduleAtFixedRate(timerTask, 0, 1000);

        }
    }

    public void showError(String message) {
        try {
            loaded();
            completed = true;
            clearTimer();
            Intent res = new Intent();
            res.putExtra("message", "" + message);
            setResult(Activity.RESULT_CANCELED, res);
            finish();
            EzUtils.diagBottom(ctx, getString(R.string.error), getString(R.string.error_msg_try_again) + " " + message, false, getString(R.string.retry), this::finish);
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void onDestroy() {
        clearTimer();
        super.onDestroy();
    }

}
