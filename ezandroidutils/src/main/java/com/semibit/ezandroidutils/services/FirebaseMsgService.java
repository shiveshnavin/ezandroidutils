package com.semibit.ezandroidutils.services;

/**
 * Created by shivesh on 21/2/17.
 */

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import  com.semibit.ezandroidutils.ui.messaging.InAppMessage;
import  com.semibit.ezandroidutils.ui.messaging.MessagingService;
import  com.semibit.ezandroidutils.utils.FCMNotificationUtils;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Map;


public class FirebaseMsgService extends FirebaseMessagingService {

    private static final String TAG = "MyAndroidFCMService";
    Gson js;
    Context ctx;
    FCMNotificationUtils utils;
    public static String lastJson = "";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e(TAG, remoteMessage.toString());
        ctx = getApplicationContext();
        EzUtils.init(ctx);



        if (utils == null)
            utils = new FCMNotificationUtils(getApplicationContext());


        Map<String, String> data = remoteMessage.getData();
        try {
            EzUtils.e("firebase notif " + Thread.currentThread().getName(), data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String json = EzUtils.js.toJson(data);
            InAppMessage chatMessage = EzUtils.js.fromJson(json, InAppMessage.class);

            ArrayList<InAppMessage> notifAgg = new ArrayList<>();
            MessagingService.saveMessage(getApplicationContext(), chatMessage, notifAgg);


            if (!json.equals(lastJson)) {
                utils.sendNotification(ctx, "" + chatMessage.getQuotedTextId(), EzUtils.randomInt(3),
                        chatMessage.getMsgTitle(),
                        "" + chatMessage.getRefinedMessage(),
                        null, "messages",
                        chatMessage.getIntent(this), PendingIntent.FLAG_ONE_SHOT,
                        chatMessage);
                lastJson = json;
            }
        } catch (Exception e) {
            if (EzUtils.DEBUG_ENABLED) e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }


}
