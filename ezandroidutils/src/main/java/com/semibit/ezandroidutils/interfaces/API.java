package com.semibit.ezandroidutils.interfaces;


import com.semibit.ezandroidutils.models.GenricUser;

public interface API {

    void getGenricUser(String userId, GenricObjectCallback<GenricUser> cb);

}