package com.semibit.ezandroidutils.ui.fragments;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.adapters.GenriXAdapter;
import  com.semibit.ezandroidutils.ui.BaseFragment;
import  com.semibit.ezandroidutils.utils.ResourceUtils;
import  com.semibit.ezandroidutils.utils.ShowHideLoader;
import  com.semibit.ezandroidutils.EzUtils;
import  com.semibit.ezandroidutils.views.RoundRectCornerImageView;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class WalletFragment extends BaseFragment {

    private WalletViewModel walletViewModel;
    private GenriXAdapter<Transaction> adapter;

    private ConstraintLayout contWalletBalancecont;
    private LinearLayout contWalletBalance;
    private TextView currency;
    private TextView walletBalance;
    private TextView yourWalletBalanceTxt;
    private View yourWalletBalanceSep;
    private ConstraintLayout contaddMoneyBalance;
    private TextView addMoneyBal;
    private TextView addMoneyBalTxt;
    private RoundRectCornerImageView addMoneyBalIcon;
    private RoundRectCornerImageView next;
    private Button addBtn;
    private View withdrawBtn;
    private LinearLayout contNumbers;
    private ConstraintLayout contAwardBalance;
    private Button awardBalAction;
    private TextView awardBalTxt;
    private TextView awardBal;
    private RoundRectCornerImageView awardBalIcon;
    private ConstraintLayout contRefBalance;
    private TextView refBal;
    private TextView refBalTxt;
    private RoundRectCornerImageView refBalIcon;
    private TextView recentTxnTxt;
    private TextView showTransactions;
    private TabLayout tabTxns;
    private TabItem tabAll;
    private TabItem tabCredit;
    private TabItem tabDebit;
    private RecyclerView listTransactions;
    private View loader;
    private ShowHideLoader showHideLoader;

    private void findViews(View root) {
        loader = root.findViewById(R.id.loader);
        contWalletBalancecont = (ConstraintLayout) root.findViewById(R.id.contWalletBalancecont);
        contWalletBalance = (LinearLayout) root.findViewById(R.id.contWalletBalance);
        currency = (TextView) root.findViewById(R.id.currency);
        walletBalance = (TextView) root.findViewById(R.id.walletBalance);
        yourWalletBalanceTxt = (TextView) root.findViewById(R.id.yourWalletBalanceTxt);
        yourWalletBalanceSep = (View) root.findViewById(R.id.yourWalletBalanceSep);
        contaddMoneyBalance = (ConstraintLayout) root.findViewById(R.id.contaddMoneyBalance);
        addMoneyBal = (TextView) root.findViewById(R.id.addMoneyBal);
        addMoneyBalTxt = (TextView) root.findViewById(R.id.addMoneyBalTxt);
        addMoneyBalIcon = (RoundRectCornerImageView) root.findViewById(R.id.addMoneyBalIcon);
        next = (RoundRectCornerImageView) root.findViewById(R.id.next);
        addBtn = (Button) root.findViewById(R.id.addBtn);
        withdrawBtn = root.findViewById(R.id.withdrawBtn);
        contNumbers = (LinearLayout) root.findViewById(R.id.contNumbers);
        contAwardBalance = (ConstraintLayout) root.findViewById(R.id.editProfileCont);
        awardBalAction = (Button) root.findViewById(R.id.awardBalAction);
        awardBalTxt = (TextView) root.findViewById(R.id.awardBalTxt);
        awardBal = (TextView) root.findViewById(R.id.editProfileTxt);
        awardBalIcon = (RoundRectCornerImageView) root.findViewById(R.id.awardBalIcon);
        contRefBalance = (ConstraintLayout) root.findViewById(R.id.contRefBalance);
        refBal = (TextView) root.findViewById(R.id.refBal);
        refBalTxt = (TextView) root.findViewById(R.id.refBalTxt);
        refBalIcon = (RoundRectCornerImageView) root.findViewById(R.id.refBalIcon);
        recentTxnTxt = (TextView) root.findViewById(R.id.recentTxnTxt);
        showTransactions = (TextView) root.findViewById(R.id.show_transactions);
        tabTxns = (TabLayout) root.findViewById(R.id.tabTxns);
        tabAll = tabTxns.findViewById(R.id.tab_all);
        tabCredit = tabTxns.findViewById(R.id.tab_credit);
        tabDebit = tabTxns.findViewById(R.id.tab_debit);
        listTransactions = (RecyclerView) root.findViewById(R.id.listTransactions);
    }



    private static WalletFragment mInstance;
    public static WalletFragment getInstance(){
        if(mInstance==null)
            mInstance = new WalletFragment();
        return mInstance;
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mInstance = this;
        walletViewModel = WalletViewModel.getInstance();
        View root = inflater.inflate(R.layout.fragment_wallet, container, false);
        findViews(root);
        showHideLoader = ShowHideLoader.create().content(listTransactions).loader(loader);

        try {
            if (act.fragmentManager.getBackStackEntryCount() > 0) {
                act.fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        } catch (Exception e) {
            EzUtils.e("Minor err at WalletFragment:115");
            // e.printStackTrace();
        }
        currency.setText(App.getStringRes(R.string.currency));
        showTransactions.setOnClickListener(v -> {
            showTransactions.setVisibility(View.GONE);
            walletViewModel.getTransactions().observe(getViewLifecycleOwner(), this::setUpTransactionsList);
            String doc = "all";
            if (tabTxns.getSelectedTabPosition() == 1) {
                doc = "wallet_credit";
            } else if (tabTxns.getSelectedTabPosition() == 2) {
                doc = "wallet_debit";
            }
            walletViewModel.refresh(doc);
            showHideLoader.loading();
        });

        walletViewModel.getWallet().observe(getViewLifecycleOwner(), this::setUpWallet);
        addBtn.setOnClickListener(view -> navService.startAddCredits(fragmentId));
        tabTxns.postDelayed(() -> {

            tabTxns.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    showHideLoader.loading();
                    showTransactions.callOnClick();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });


        }, 500);
        return root;
    }

    private void setUpWallet(Wallet wallet) {

        walletBalance.setText("" + wallet.getCreditBalance());
        awardBal.setText(getString(R.string.award_bal) + getString(App.getStringRes(R.string.currency)) + " " + wallet.getWinningBalance());
        refBal.setText(getString(R.string.ref_bal) + getString(App.getStringRes(R.string.currency)) + " " + wallet.getAggReferralBalance());

    }

    private void setUpTransactionsList(List<Transaction> transactionsList) {
        if (transactionsList == null)
            return;
        showHideLoader.loaded();

        Collections.sort(transactionsList, (t1, transaction) -> transaction.getTimeStamp().compareTo(t1.getTimeStamp()));

        if (adapter == null) {
            adapter = new GenriXAdapter<Transaction>(getContext(), R.layout.row_transaction, transactionsList) {
                @Override
                public void onBindViewHolder(@NonNull CustomViewHolder viewHolder, int i) {

                    final int pos = viewHolder.getAdapterPosition();
                    final CustomViewHolder vh = (CustomViewHolder) viewHolder;
                    final Transaction transaction = transactionsList.get(pos);
                    vh.textView(R.id.txnId).setText(Html.fromHtml(getString(R.string.orderNo) + transaction.getId()));
                    vh.textView(R.id.txnAmtTxt).setText(getString(App.getStringRes(R.string.currency)) + " " + transaction.getAmount());
                    vh.textView(R.id.txnStatusTxt).setText(transaction.getDisplayStatus());

                    vh.textView(R.id.txnAmtTxt).setTextColor(ResourceUtils.getColor(transaction.getStatusColor()));
//                  vh.textView(R.id.txnStatusTxt).setTextColor(ResourceUtils.getColor(transaction.getStatusColor()));
                    vh.textView(R.id.txnDateTxt).setText(EzUtils.getDateTime(new Date(transaction.getTimeStamp()), "hh:mm a dd MMM yyyy"));

                    vh.imageView(R.id.txnIcon).setImageResource(transaction.getTxtTypeIcon());
                    vh.textView(R.id.txnDetails).setText(transaction.getDescription());

                    vh.base.setOnLongClickListener(view -> {
                        EzUtils.copyToClipBoard(String.format(getString(R.string.your_order_id), getString(R.string.app_name)), transaction.getId(), ctx);
                        return true;
                    });
                    vh.base.setOnClickListener(view -> {
                        if(transaction.getTxnType().equals("wallet_withdrawl")){
                            navService.startChatMessaging(fragmentId);
                        }
                    });

                }
            };

            listTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
            listTransactions.setAdapter(adapter);
        } else {
            adapter.itemList.clear();
            adapter.itemList.addAll(transactionsList);
            adapter.notifyDataSetChanged();
        }


    }

}