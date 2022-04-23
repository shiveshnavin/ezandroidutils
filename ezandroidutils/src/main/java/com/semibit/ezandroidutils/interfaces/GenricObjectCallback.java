package com.semibit.ezandroidutils.interfaces;

import com.semibit.ezandroidutils.EzUtils;

import java.util.ArrayList;

public interface GenricObjectCallback<T> {

    default void onEntity(T data){
        EzUtils.e("GenricObjectCallback::onEntity Not Implemented");
    };
    default void onEntitySet(ArrayList<T> listItems){
        EzUtils.e("GenricObjectCallback::onEntitySet Not Implemented");
    };
    default void onError(String message){
        EzUtils.e("GenricObjectCallback::onError Not Implemented");
    }
}
