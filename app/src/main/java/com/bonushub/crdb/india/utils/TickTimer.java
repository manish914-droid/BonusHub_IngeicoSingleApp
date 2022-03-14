/*
 *  ===========================================================================================
 *  = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *     This software is supplied under the terms of a license agreement or nondisclosure
 *     agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *     disclosed except in accordance with the terms in that agreement.
 *          Copyright (C) 2020 -? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 *  Description: // Detail description about the function of this module,
 *               // interfaces with the other modules, and dependencies.
 *  Revision History:
 *  Date	               Author	                   Action
 *  2020/06/01 	         Qinny Zhou           	      Create
 *  ===========================================================================================
 */
package com.bonushub.crdb.india.utils;

import android.os.CountDownTimer;

public class TickTimer {

    public static final int DEFAULT_TIMEOUT = 60;
    private Timer timer;

    public interface OnTimerTickListener {
        void onTick(long leftTime);
    }

    public interface OnTimerFinishListener {
        void onFinish();
    }

    private static class Timer extends CountDownTimer {
        private OnTimerTickListener tickListener;
        private OnTimerFinishListener finishListener;
        Timer(long timeout, long tickInterval) {
            super(timeout * 1000, tickInterval * 1000);
        }

        public void registerTickListener(OnTimerTickListener timerTickListener) {
            this.tickListener = timerTickListener;
        }

        public void registerFinishListener(OnTimerFinishListener timerFinishListener) {
            this.finishListener = timerFinishListener;
        }
        @Override
        public void onFinish() {
            if (finishListener != null)
                finishListener.onFinish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (tickListener != null)
                tickListener.onTick(millisUntilFinished / 1000);
        }
    }

    public TickTimer() {

    }

    public void start() {
        if (timer != null) {
            timer.cancel();
        }
        updateTimer(DEFAULT_TIMEOUT);
        timer.start();
    }

    public void start(int timeout, OnTimerFinishListener timerFinishListener) {
        if (timer != null) {
            timer.cancel();
        }
        updateTimer(timeout);
        timer.registerFinishListener(timerFinishListener);
        timer.start();
    }

    public void start(int timeout, OnTimerTickListener timerTickListener, OnTimerFinishListener timerFinishListener) {
        if (timer != null) {
            timer.cancel();
        }
        updateTimer(timeout);
        timer.registerFinishListener(timerFinishListener);
        timer.registerTickListener(timerTickListener);
        timer.start();
    }
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer.registerFinishListener(null);
            timer.registerTickListener(null);
            timer = null;
        }


    }

    private void updateTimer(int timeout) {
        timer = new Timer(timeout, 1);
    }
}
