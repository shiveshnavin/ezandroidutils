package com.semibit.ezandroidutils.ui.messaging;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.interfaces.GenricCallback;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.services.DBService;
import  com.semibit.ezandroidutils.services.InAppNavService;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.ui.BaseFragment;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class MessagingFragment extends BaseFragment {

    public BaseActivity act;
    public Context ctx;
    public InAppNavService navService;
    public int fragmentId;
    public GenricUser user;
    public TextView title ;
    public ImageView logo;
    public String groupId;
    public String recieverId;

    MessagingService messagingService;
    MessagingAdapter messagingAdapter;
    ArrayList<InAppMessage> messages = new ArrayList<>();

    private static MessagingFragment mInstance;
    public static MessagingFragment getInstance(){
        if(mInstance==null)
            mInstance = new MessagingFragment();
        return mInstance;
    }


    private final GenricObjectCallback<InAppMessage> onNewMessages = new GenricObjectCallback<InAppMessage>() {
        @Override
        public void onEntitySet(ArrayList<InAppMessage> listItems) {
            loader.setVisibility(View.GONE);
            Set<InAppMessage> deduped = new TreeSet((Comparator<InAppMessage>) (o1, o2) -> {
                if (o1.id.equals(o2.id)){
                    return 0;
                }
                return 1;
            });
            deduped.addAll(listItems);
            messages.addAll(deduped);
            messagingAdapter.notifyDataSetChanged();
            list.smoothScrollToPosition(messages.size());
            listItems.stream().forEach(c-> DBService.getInstance(ctx).makeSeen(c.id));
            DBService.getInstance(ctx).makeSeen(groupId);
        }
    };
    private final GenricObjectCallback<InAppMessage> onOldMessages = new GenricObjectCallback<InAppMessage>() {
        @Override
        public void onEntitySet(ArrayList<InAppMessage> listItems) {
            loader.setVisibility(View.GONE);
            messages.addAll(listItems);
            messagingAdapter.notifyDataSetChanged();
        }
    };
    private final GenricCallback onResetSignal = () -> messages.clear();


    public void setUpToolbar(View root){
        title = root.findViewById(R.id.title);
        logo = root.findViewById(R.id.logo);
        if(logo!=null)
        {
            logo.setOnClickListener(v->{
                getActivity().onBackPressed();
            });
        }
    }


    public void setTitle(String titl){
        if(title!=null)
            title.setText(titl);
    }

    public void init(){
        act = (BaseActivity) getActivity();
        ctx = getContext();
        navService = new InAppNavService(act);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
       try{
           getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
       }catch (Exception e){
           e.printStackTrace();
       }
        init();
        fragmentId = container.getId();
        user = EzUtils.readUserData();
        recieverId = FirebaseRemoteConfig.getInstance().getString("support_user_id");

        View root = inflater.inflate(R.layout.messaging_fragment, container, false);
        findViews(root);
        setUpToolbar(root);
        setTitle(getString(R.string.support));
        groupId = "support:"+user.getEmail();
        messagingService = new MessagingService(groupId,ctx,getViewLifecycleOwner(),onNewMessages,onOldMessages,onResetSignal);
        messagingAdapter = new MessagingAdapter(messages,ctx,user,messagingService);

        list.setAdapter(messagingAdapter);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(ctx);
//        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        list.setLayoutManager(mLayoutManager);
        loader.setVisibility(View.VISIBLE);

        messagingService.fetchMessages(DBService.getInstance(ctx).getLatestMessage(groupId));

        send.setOnClickListener(view -> {
            if(EzUtils.isEmpty(messageEdit.getText().toString()))
                return;
            loader.setVisibility(View.VISIBLE);
            messagingService.sendMessage(user.getId(),recieverId,messageEdit.getText().toString());
            messageEdit.setText("");
        });
        return root;
    }

    public void setActivityAndContext(BaseActivity act) {
        this.act = act;
        this.ctx = ctx;
        this.navService = act.inAppNavService;
    }

    private ConstraintLayout contChat;
    private RecyclerView list;
    private LinearProgressIndicator loader;
    private ConstraintLayout messgCont;
    private EditText messageEdit;
    private ImageView attach;
    private ImageView send;
    private ConstraintLayout toolCont;
    private LinearLayout headCont;


    private void findViews(View root) {
        contChat = root.findViewById( R.id.cont_chat );
        list = root.findViewById( R.id.list );
        loader = root.findViewById( R.id.loader );
        messgCont = root.findViewById( R.id.messg_cont );
        messageEdit = root.findViewById( R.id.message_edit );
        attach = root.findViewById( R.id.attach );
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        send = root.findViewById( R.id.send );
        toolCont = root.findViewById( R.id.tool_cont );
        logo = root.findViewById( R.id.logo );
        headCont = root.findViewById( R.id.head_cont );
        title = root.findViewById( R.id.title );
    }

}


