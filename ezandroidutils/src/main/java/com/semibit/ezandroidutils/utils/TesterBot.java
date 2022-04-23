package com.semibit.ezandroidutils.utils;

import android.os.Handler;

import  com.semibit.ezandroidutils.App;

public interface TesterBot {
    static void execute(TesterBot task, Handler handler, long delay) {
        if (!App.isBotTestMode)
            return;
        if (delay > 0)
            handler.postDelayed(task::eval, delay);
        else task.eval();
    }

    void eval();
}
