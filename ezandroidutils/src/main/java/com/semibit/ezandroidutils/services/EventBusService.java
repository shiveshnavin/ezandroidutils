package com.semibit.ezandroidutils.services;

import static  com.semibit.ezandroidutils.utils.ResourceUtils.getString;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.adapters.GenriXAdapter;
import  com.semibit.ezandroidutils.models.ActionItem;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.ui.activities.HomeActivity;
import  com.semibit.ezandroidutils.EzUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class EventBusService {

    public static EventBusService mIsntance;

    public EventBusService() {
        mIsntance = this;
    }

    public static EventBusService getInstance() {
        if (mIsntance == null) mIsntance = new EventBusService();

        return mIsntance;
    }

    public void doActionItem(ActionItem cm) {

        BaseActivity act = cm.act;
        String actionType = cm.actionType;
        try {
            if (act != null) {


                if (actionType.equals(Constants.ACTION_HOME)) {
                    act.startHome();
                } else if (actionType.equals(Constants.ACTION_ADD_CREDITS)) {
                    if (act instanceof HomeActivity)
                        act.inAppNavService.startAddCredits(R.id.nav_host_fragment);
                } else if (actionType.equals(Constants.ACTION_REDEEM_OPTIONS)) {

                    ArrayList<ActionItem> listMenu = new ArrayList<>();

                    listMenu.add(ActionItem.ActionItemBuilder.anActionItem()
                            .withTitle(getString(R.string.withdraw))
                            .withAct(act)
                            .withImage("withdraw")
                            .withActionType(Constants.ACTION_WITHDRAW)
                            .withSubTitle(getString(R.string.help_withdraw))
                            .withAccentColorId(""+R.color.material_green_700)
                            .build());


                    listMenu.add(ActionItem.ActionItemBuilder.anActionItem()
                            .withTitle(getString(R.string.shop))
                            .withAct(act)
                            .withImage("shop")
                            .withActionType(Constants.ACTION_SHOP)
                            .withSubTitle(getString(R.string.help_shop))
                            .withAccentColorId(""+R.color.material_teal_700)
                            .build());

                    AtomicReference<BottomSheetDialog> dialogAtomicReference = new AtomicReference<>();
                    GenriXAdapter<ActionItem> actionItemGenriXAdapter =
                            new GenriXAdapter<ActionItem>(act, R.layout.utl_row_menu, listMenu) {
                                @Override
                                public void onBindViewHolder(@NonNull CustomViewHolder viewHolder, int i) {

                                    final int pos = viewHolder.getAdapterPosition();
                                    final CustomViewHolder vh = (CustomViewHolder) viewHolder;
                                    final ActionItem menuItem = itemList.get(pos);

                                    vh.textView(R.id.txt).setText(menuItem.title);
                                    vh.textView(R.id.subTxt).setText(menuItem.subTitle);
                                    if (menuItem.getIcon() != null)
                                        vh.imageView(R.id.img).setImageDrawable(menuItem.getIcon());
                                    try {
                                        vh.base.setBackgroundTintList(ContextCompat.getColorStateList(act, Integer.parseInt(menuItem.accentColorId)));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    EzUtils.addPressReleaseAnimation(vh.view(R.id.contRefBalance));
                                    vh.view(R.id.contRefBalance).setOnClickListener(view -> {
                                        if(dialogAtomicReference.get()!=null){
                                            dialogAtomicReference.get().dismiss();
                                        }
                                        EventBusService.getInstance().doActionItem(menuItem);
                                    });

                                }
                            };

                    dialogAtomicReference.set(EzUtils.diagBottomList(act, "", actionItemGenriXAdapter, true, act.getString(R.string.dismiss),null));
                }else if (actionType.equals(Constants.ACTION_SHOP)) {
                    if (act instanceof HomeActivity)
                        act.inAppNavService.startShop(R.id.nav_host_fragment);
                }else if (actionType.equals(Constants.ACTION_WITHDRAW)) {
                    if (act instanceof HomeActivity)
                        act.inAppNavService.startWithdraw(R.id.nav_host_fragment);
                }
                else if (actionType.equals(Constants.ACTION_EARN_MONEY)) {
                    if (act instanceof HomeActivity)
                        act.inAppNavService.startEarnShop(R.id.nav_host_fragment);
                }
                else if (actionType.equals(Constants.ACTION_PLAY_GAME)) {
                    if (act instanceof HomeActivity)
                        act.inAppNavService.startSelectGameAmount(R.id.nav_host_fragment,null);
                }
                else if (actionType.equals(Constants.ACTION_HOW_TO_PLAY)) {

                    String promoVideo = FirebaseRemoteConfig.getInstance().getString("promo_video");
                    if (promoVideo.startsWith("https://")) {
                        act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(promoVideo)));
                    } else
                        act.inAppNavService.startWebsite(getString(R.string.help_and_guide), Constants.HOST + "/howtoplay.html?app=1");

                }
                else if (actionType.equals(Constants.ACTION_TOC)) {
                    act.inAppNavService.startWebsite(act.getString(R.string.terms),Constants.HOST+"/toc.html");
                }
                else if (actionType.equals(Constants.ACTION_SUPPORT)) {
                    act.inAppNavService.startChatMessaging(R.id.nav_host_fragment);
                }
                else if (actionType.equals(Constants.ACTION_OPEN_WALLET)) {
                    act.inAppNavService.startWallet(R.id.nav_host_fragment);
                }
            }
            if (cm.doFinish)
                if (act != null) {
                    act.finish();
                }

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        }

    }
}
