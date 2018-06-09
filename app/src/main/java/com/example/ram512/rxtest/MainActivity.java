package com.example.ram512.rxtest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;

import org.reactivestreams.Publisher;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    public final static String MAC = "CC:78:AB:1A:75:03";

    private TextView mStatusTextView;

    private RxBleClient mBleClient;
    private PublishSubject<Boolean> mDisconnectionTrigger;
    private Observable<RxBleConnection> mConnectionObservable;
    private Disposable mConnectionStateDisposable;
    private Disposable mConnectionDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prepareViews();

        mBleClient = ((RxTestApplication) getApplication()).getBleClient();
        mDisconnectionTrigger = PublishSubject.create();
    }

    private void connect(String address) {
        if (mConnectionDisposable != null && !mConnectionDisposable.isDisposed()) {
            disconnect();
        }

        RxBleDevice device = mBleClient.getBleDevice(address);

        mConnectionObservable = getConnectionObservable(device);
        mConnectionDisposable = mConnectionObservable
                .subscribe(
                        connection -> Log.w(TAG, "connect: connected"),
                        throwable -> Log.e(TAG, "connect: failure " + throwable.getClass().getSimpleName()),
                        () -> Log.w(TAG, "connect: onComplete()")
                );

        mConnectionStateDisposable = getConnectionStateObservable(device)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> mStatusTextView.setText(state.name()));
    }

    private void disconnect() {
        mDisconnectionTrigger.onNext(true);
    }

    private Observable<RxBleConnection> getConnectionObservable(RxBleDevice device) {
        return device.establishConnection(false)
                .takeUntil(mDisconnectionTrigger)
                .doOnError(error -> Log.e(TAG, "connect: -----------before retry---------- " + error.getClass().getSimpleName()))
                .retryWhen(errors -> errors.filter(error -> error instanceof BleDisconnectedException))
                .doOnError(error -> Log.e(TAG, "connect: ==============after retry============ " + error.getClass().getSimpleName()));
    }

    private Observable<RxBleConnection.RxBleConnectionState> getConnectionStateObservable(RxBleDevice device) {
        return device.observeConnectionStateChanges();
    }

    private void prepareViews() {
        mStatusTextView = findViewById(R.id.status_textview);
        findViewById(R.id.connect_button).setOnClickListener(view -> connect(MAC));
        findViewById(R.id.disconnect_button).setOnClickListener(view -> disconnect());
        findViewById(R.id.lights_button).setOnClickListener(view -> startLightsActivity());
    }

    private void startLightsActivity() {
        Intent intent = new Intent(this, LightsActivity.class);
        startActivity(intent);
    }
}
