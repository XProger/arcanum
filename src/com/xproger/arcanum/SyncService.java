package com.xproger.arcanum;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncService extends Service {
    private static final Object syncObj = new Object();
    private static SyncAdapter adapter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Common.logError("SyncService created");
        synchronized (syncObj) {
            if (adapter == null)
                adapter = new SyncAdapter(getApplicationContext(), true);            
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Common.logError("SyncService destroyed");        
    }

    @Override
    public IBinder onBind(Intent intent) {
        return adapter.getSyncAdapterBinder();
    }
}
