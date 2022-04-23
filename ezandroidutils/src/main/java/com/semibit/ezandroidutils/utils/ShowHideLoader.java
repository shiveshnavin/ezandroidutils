package com.semibit.ezandroidutils.utils;

import android.content.Context;
import android.view.View;

public class ShowHideLoader {

    private Context ctx;
    private View content;
    private View loader;

    public ShowHideLoader(){
    }

    public static ShowHideLoader create(){
        return new ShowHideLoader();
    }

    public ShowHideLoader content(View content){
        this.content=content;
        return this;
    }
    public ShowHideLoader loader(View loader){
        this.loader=loader;
        return this;
    }
    public ShowHideLoader loaded(){
        try {
            if(this.content!=null){
                content.setVisibility(View.VISIBLE);
                content.setAlpha(1f);
            }
            if(this.loader!=null){
                this.loader.setVisibility(View.GONE);
                this.loader.setAlpha(0f);
            }
        } catch (Exception ignored) {
        }
        return this;
    }

    public ShowHideLoader loading(){
        try {
            if(this.content!=null){
                content.setVisibility(View.GONE);
                content.setAlpha(0f);
            }
            if(this.loader!=null){
                this.loader.setVisibility(View.VISIBLE);
                loader.animate().alpha(1f).setDuration(500).start();
            }
        } catch (Exception ignored) {
        }
        return this;
    }


}
