package com.semibit.ezandroidutils.utils;

import android.os.CountDownTimer;
import android.view.View;

import com.semibit.ezandroidutils.interfaces.GenricCallback;
import com.semibit.ezandroidutils.interfaces.GenricDataCallback;

public class ProgressiveTextAnimator {


    public GenricCallback onFinish;
    public GenricDataCallback onTick;
    public View view;
    public CountDownTimer ctr;
    public int count = 10;

    private int initialValue, curValue, finalValue, step;
    private long durationMilis = -1;
    private long durationPerStep = -1;

    public static ProgressiveTextAnimator fixedDurationAnimator(GenricDataCallback onTick, GenricCallback onFinish, View view, long durationMilis) {
        ProgressiveTextAnimator progressiveTextAnimator = new ProgressiveTextAnimator();
        progressiveTextAnimator.onFinish = onFinish;
        progressiveTextAnimator.onTick = onTick;
        progressiveTextAnimator.view = view;
        if (durationMilis <= -1) {
            durationMilis = 1;
        }
        progressiveTextAnimator.durationMilis = durationMilis;
        return progressiveTextAnimator;
    }

    public static ProgressiveTextAnimator fixedStepDurationAnimator(GenricDataCallback onTick, GenricCallback onFinish, View view, long durationPerStep) {
        ProgressiveTextAnimator progressiveTextAnimator = new ProgressiveTextAnimator();
        progressiveTextAnimator.onFinish = onFinish;
        progressiveTextAnimator.onTick = onTick;
        progressiveTextAnimator.view = view;
        if (durationPerStep <= -1) {
            durationPerStep = 1;
        }
        progressiveTextAnimator.durationPerStep = durationPerStep;
        return progressiveTextAnimator;
    }

    public void start(int initVal, int finalVal, int stepValue) {

        this.initialValue = initVal;
        this.curValue = initVal;
        this.finalValue = finalVal;
        this.step = stepValue;

        int noSteps = Math.max((finalVal - initVal) / stepValue, 1);

        if (durationMilis <= -1) {
            durationMilis = stepValue * durationPerStep;
        }
        if (durationPerStep <= -1) {
            durationPerStep = durationMilis / noSteps;
        }

        ctr = new CountDownTimer(durationMilis, durationPerStep) {
            @Override
            public void onTick(long l) {
                curValue += stepValue;
                if(view != null){
                    view.setScaleX(1.5f);
                    view.setScaleY(1.5f);

                    view.animate().setDuration(1000).scaleX(1);
                    view.animate().setDuration(1000).scaleY(1);
                }

                if (onTick != null && curValue < finalValue) {
                    onTick.onStart("" + curValue, (int) curValue);
                }
                else if(curValue >= finalValue){
                    onTick.onStart("" + finalValue, (int) finalValue);
                }

            }

            @Override
            public void onFinish() {
                if (onFinish != null)
                    onCompleted();
            }
        };
        ctr.start();

    }

    public void stop() {
        if (ctr != null)
            ctr.cancel();
    }

    public void reset() {
        stop();
        start(initialValue,finalValue,step);
    }

    public void onCompleted() {
        onFinish.onStart();
    }
}
