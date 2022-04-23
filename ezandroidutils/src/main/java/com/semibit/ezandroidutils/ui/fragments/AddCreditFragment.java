package com.semibit.ezandroidutils.ui.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Strings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.adapters.GenriXAdapter;
import  com.semibit.ezandroidutils.domain.dotpot.binding.GameViewModel;
import  com.semibit.ezandroidutils.interfaces.GenricCallback;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.domain.dotpot.models.game.Game;
import  com.semibit.ezandroidutils.services.CrashReporter;
import  com.semibit.ezandroidutils.services.RestAPI;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.ui.BaseFragment;
import  com.semibit.ezandroidutils.ui.activities.PaytmPaymentActivity;
import  com.semibit.ezandroidutils.ui.activities.WebViewActivity;
import  com.semibit.ezandroidutils.utils.ResourceUtils;
import  com.semibit.ezandroidutils.utils.ShowHideLoader;
import  com.semibit.ezandroidutils.EzUtils;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class AddCreditFragment extends BaseFragment {

    private static AddCreditFragment mInstance;
    private GenriXAdapter<Float> adapter;
    private RecyclerView listTransactions;
    private View loader;
    GenricObjectCallback<Game> onDone = new GenricObjectCallback<Game>() {
        @Override
        public void onEntity(Game data) {

            new Handler(Looper.myLooper()).postDelayed(() -> {
                ShowHideLoader.create().content(listTransactions).loader(loader).loaded();
            }, 1000);

            navService.startGame(data);
            if (data.getAmount() > 0)
                WalletViewModel.getInstance().refresh(null);

        }

        @Override
        public void onError(String message) {
            ShowHideLoader.create().content(listTransactions).loader(loader).loaded();
            EzUtils.diagBottom(ctx, getString(R.string.error), message, R.drawable.error);
        }
    };
    GenricObjectCallback<Game> onInsuff = new GenricObjectCallback<Game>() {
        @Override
        public void onError(String message) {
            ShowHideLoader.create().content(listTransactions).loader(loader).loaded();
            EzUtils.diagBottom(ctx, getString(R.string.insufficient_credits_header),
                    getString(R.string.insufficient_credits), true, getString(R.string.add_credits), new GenricCallback() {
                        @Override
                        public void onStart() {
                            navService.startAddCredits(fragmentId);
                        }
                    });
        }
    };

    public static AddCreditFragment getInstance() {
//        if (mInstance == null)
            mInstance = new AddCreditFragment();
        return mInstance;
    }

    public static void checkWalletAndStartGame(Float amount,
                                               GenricObjectCallback<Game> onDone,
                                               GenricObjectCallback<Game> onNoBalance,
                                               String player2Id) {

        Wallet data = WalletViewModel.getInstance().getWallet().getValue();

        if (data == null || data.getCreditBalance() >= amount) {

            RestAPI.getInstance().createGame(amount, player2Id, onDone);


        } else {
            onNoBalance.onError(ResourceUtils.getString(R.string.insufficient_credits_header));
        }
    }

    ConstraintLayout root;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        act = (BaseActivity) getActivity();
        root = (ConstraintLayout) inflater.inflate(R.layout.fragment_credit, container, false);
        listTransactions = root.findViewById(R.id.list);
        loader = root.findViewById(R.id.loader);

        setUpToolbar(root);

        go();

        return root;
    }

    private void go() {
        Bundle arguments = getArguments();
        String action = null;
        if (arguments != null) {
            action = arguments.getString("action");
        }
        if (action == null) {
            action = "select_pay_amount";
        }
        String finalAction = action;
        if (action.equals("select_game_amount")) {
            if (arguments.getFloat("amount") > 0) {
                showDialog(arguments.getFloat("amount"), new GenricCallback() {
                    @Override
                    public void onStart() {
                        checkWalletAndStartGame(arguments.getFloat("amount"), onDone,onInsuff,"");
                    }
                }, null);
            }
            setTitle(ResourceUtils.getString(R.string.select_game_credits));
            setUpAmounts(GameViewModel.getInstance().getGameAmounts().getValue(), finalAction);

        } else {

            setTitle(ResourceUtils.getString(R.string.add_credits));
            setUpAmounts(WalletViewModel.getInstance().getPayAmounts().getValue(), finalAction);
        }

    }

    private void setUpAmounts(List<Float> listData, final String action) {

        Collections.sort(listData);
        listData.remove(0f);
        if (action.equals("select_game_amount")) {
            listData.add(0,0f);
        }
        adapter = new GenriXAdapter<Float>(getContext(), R.layout.row_credit, listData) {
            @Override
            public void onBindViewHolder(@NonNull CustomViewHolder viewHolder, int i) {

                final int pos = viewHolder.getAdapterPosition();
                final CustomViewHolder vh = (CustomViewHolder) viewHolder;
                final Float amount = listData.get(pos);


                vh.textView(R.id.currency).setText(App.getStringRes(R.string.currency));
                if (amount == 0f && action.equals("select_game_amount")) {
                    vh.textView(R.id.currency).setVisibility(View.GONE);
                    vh.textView(R.id.walletBalance).setText(R.string.practice);
                    vh.textView(R.id.yourWalletBalanceTxt).setText(R.string.brush_up_practice_txt);
                } else {
                    vh.textView(R.id.currency).setVisibility(View.VISIBLE);
                    vh.textView(R.id.walletBalance).setText(String.format("%s", amount.intValue()));
                    vh.textView(R.id.yourWalletBalanceTxt).setText(String.format(ResourceUtils.getString(R.string.possible_rewards), "\n"+getString(App.getStringRes(R.string.currency)), "" + Game.possibleAwards(amount)));
                }

                if (action.equals("select_game_amount")) {
                    vh.textView(R.id.addBtn).setText(R.string.start_game);
//                    vh.textView(R.id.addBtn).setTextColor(act.getcolor(R.color.colorTextSuccess));

                } else {
                    vh.textView(R.id.addBtn).setText(R.string.add_credits);
//                    vh.textView(R.id.addBtn).setTextColor(act.getcolor(R.color.colorTextSuccess));

                }

                vh.itemView.setOnClickListener(view -> {

                    if (action.equals("select_game_amount")) {
                        showDialog(amount, new GenricCallback() {
                            @Override
                            public void onStart() {
                                ShowHideLoader.create().content(listTransactions).loader(loader).loading();
                                checkWalletAndStartGame(amount, onDone, onInsuff, "");
                            }
                        },vh.itemView);
                    } else {
                        ShowHideLoader.create().content(listTransactions).loader(loader).loading();
                        startCreditAdd(amount);
                    }

                });

            }
        };

        listTransactions.setLayoutManager(new GridLayoutManager(getContext(), 2));
        listTransactions.setAdapter(adapter);


    }

    private void startCreditAdd(Float amount) {
        if(FirebaseRemoteConfig.getInstance().getString("payment").equals("paytm")) {
            //createToken
            RestAPI.getInstance(ctx).createTransaction(amount, new GenricObjectCallback<JSONObject>() {
                @Override
                public void onEntity(JSONObject response) {

                    try {
                        ShowHideLoader.create().content(listTransactions).loader(loader).loaded();
                        Intent it = new Intent(ctx, PaytmPaymentActivity.class);
                        it.putExtra("title", ResourceUtils.getString(R.string.pay));
                        if(Strings.isNullOrEmpty(response.optString("ORDER_ID"))){
                            it.putExtra("orderId", response.optString("orderId"));
                        }
                        else {
                            it.putExtra("orderId", response.optString("ORDER_ID"));
                        }

                        it.putExtra("url", response.optString("payurl"));
//
//                        it.putExtra("TOKEN", response.optString("TOKEN"));
//                        it.putExtra("TXN_AMOUNT", response.optString("TXN_AMOUNT"));
//                        it.putExtra("MID", response.optString("MID"));
//                        it.putExtra("CALLBACK_URL", response.optString("CALLBACK_URL"));

                        getActivity().startActivityForResult(it, WebViewActivity.REQUEST_PAYMENT,new Bundle());

                        AnalyticsReporter.getInstance().logTryToPay(amount);
                    } catch (Exception e) {
                        CrashReporter.reportException(e);
                    }

                }

                @Override
                public void onError(String message) {
                    ShowHideLoader.create().content(listTransactions).loader(loader).loaded();
                    EzUtils.diagBottom(ctx, getString(R.string.error), message, R.drawable.error);
                }
            });

        }
        else {
            RestAPI.getInstance(ctx).createTransaction(amount, new GenricObjectCallback<JSONObject>() {
                @Override
                public void onEntity(JSONObject response) {

                    try {
                        new Handler(Looper.myLooper()).postDelayed(() -> {
                            ShowHideLoader.create().content(listTransactions).loader(loader).loaded();
                        }, 5000);

                        Intent it = new Intent(ctx, WebViewActivity.class);
                        it.putExtra("title", ResourceUtils.getString(R.string.pay));
                        it.putExtra("orderId", response.optString("orderId"));
                        it.putExtra("url", response.optString("payurl"));

                        getActivity().startActivityForResult(it, WebViewActivity.REQUEST_PAYMENT,new Bundle());

                        AnalyticsReporter.getInstance().logTryToPay(amount);
                    } catch (Exception e) {
                        CrashReporter.reportException(e);
                    }

                }

                @Override
                public void onError(String message) {
                    ShowHideLoader.create().content(listTransactions).loader(loader).loaded();
                    EzUtils.diagBottom(ctx, getString(R.string.error), message, R.drawable.error);
                }
            });

        }

    }

    public void showDialog(float amount, GenricCallback cb, View itemView){

        StringBuilder sbr = new StringBuilder();

        sbr.append(EzUtils.getHtml(ctx,
                String.format(ResourceUtils.getString(R.string.confirm_game)
                        ,ResourceUtils.getString(App.getStringRes(R.string.currency))
                        ,amount),R.color.colorTextPrimary));
        sbr.append(String.format(" "+ getString(R.string.possible_rewards), getString(App.getStringRes(R.string.currency)), "" + Game.possibleAwards(amount)));

        View rootView = getLayoutInflater().inflate(R.layout.diag_confirm,null);

        TextView info = (TextView)rootView.findViewById( R.id.info );
        ImageView img = (ImageView)rootView.findViewById( R.id.img );
        TextView text = (TextView)rootView.findViewById( R.id.text );
        Button pokeBtn = rootView.findViewById( R.id.pokeBtn );
        Button pokeBtn2 = rootView.findViewById( R.id.pokeBtn2 );

        EzUtils.addPressReleaseAnimation(pokeBtn);
        EzUtils.addPressReleaseAnimation(pokeBtn2);

        final Dialog dialog = new Dialog(ctx,R.style.PopupDialogNoFullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(rootView);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(ResourceUtils.getColor(R.color.transparent)));

        dialog.show();


//        dialog.setContentView(layoutResId);
        View v = dialog.getWindow().getDecorView();
        v.setBackgroundResource(android.R.color.transparent);

        info.setText(String.format(ResourceUtils.getString(R.string.awards_possible), getString(App.getStringRes(R.string.currency)), Game.possibleAwards(amount)));
        text.setText(Html.fromHtml(sbr.toString().replace("\n","<br>")));
        pokeBtn.setOnClickListener(c->{
            cb.onStart();
            dialog.dismiss();
        });
        pokeBtn2.setOnClickListener(c->{
            dialog.dismiss();
        });

//
//        EzUtils.diagBottom(ctx,
//               ""
//                ,
//
//                sbr.toString().replace("\n","<br>")
//
//                ,
//                true,
//                ResourceUtils.getString(R.string.start_game),
//                R.drawable.ic_pot,
//                () ->{
//                    cb.onStart();
//                }
//        );
    }

}