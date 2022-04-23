package com.semibit.ezandroidutils.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.EzUtils;

public class LoadingView extends androidx.appcompat.widget.AppCompatImageView {
    boolean isInit = false;

    private void setDimens(){
        setMinimumHeight(EzUtils.pxFromDp(getContext(),32f).intValue());
        setMinimumWidth(EzUtils.pxFromDp(getContext(),32f).intValue());
    }

    public LoadingView(Context context) {
        super(context);
        setDimens();
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setDimens();
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setDimens();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInit)
            postDelayed(() -> {
                isInit = true;
                setImageResource(R.drawable.avd_load);
                EzUtils.animate_avd(LoadingView.this);
            }, 100);
    }
}
