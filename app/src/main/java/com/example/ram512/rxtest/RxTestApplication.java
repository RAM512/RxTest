package com.example.ram512.rxtest;

import android.app.Application;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.internal.RxBleLog;

public class RxTestApplication extends Application {
    private RxBleClient mBleClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mBleClient = RxBleClient.create(this);
        RxBleClient.setLogLevel(RxBleLog.INFO);
    }

    public RxBleClient getBleClient() {
        return mBleClient;
    }
}
