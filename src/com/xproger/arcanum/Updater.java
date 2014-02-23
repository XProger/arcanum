package com.xproger.arcanum;

import android.os.Handler;
import android.os.Looper;

public class Updater {
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mStatusChecker;

    public Updater(final int interval, final Runnable callback) {
        mStatusChecker = new Runnable() {
            @Override
            public void run() {
            	callback.run();
                mHandler.postDelayed(this, interval);
            }
        };
    }

    public synchronized void startUpdates(){
        mStatusChecker.run();
    }

    public synchronized void stopUpdates(){
        mHandler.removeCallbacks(mStatusChecker);
    }
}