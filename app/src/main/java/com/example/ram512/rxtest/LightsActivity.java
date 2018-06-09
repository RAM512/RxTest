package com.example.ram512.rxtest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;

import java.util.Arrays;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class LightsActivity extends AppCompatActivity {
    private final static String TAG = LightsActivity.class.getSimpleName();
    private TextView mConnectionStateTextView;
    private TextView mWriteDataTextView;
    private TextView mWriteAnswerTextView;
    private TextView mReadAnswerTextView;
    private Disposable mConnectionStateDisposable;
    private PublishSubject<Boolean> mDisconnectionTrigger;

    enum Light {TAIL, LEFT, RIGHT}

    private static String TRAILER_MODE = "2f490001-b8ed-495c-882f-1b153728a885";
    private final static UUID UUID_TRAILER_MODE = UUID.fromString(TRAILER_MODE);
    private io.reactivex.Observable<RxBleConnection> mConnectionObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lights);
        prepareViews();

        mDisconnectionTrigger = PublishSubject.create();
        RxBleDevice device = ((RxTestApplication) getApplication()).getBleClient().getBleDevice(MainActivity.MAC);
        mConnectionObservable = prepareConnectionObservable(device);
        mConnectionStateDisposable = prepareConnectionStateObservable(device)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        state -> mConnectionStateTextView.setText(state.name())
                );
    }

    private Observable<RxBleConnection.RxBleConnectionState> prepareConnectionStateObservable(RxBleDevice device) {
        return device.observeConnectionStateChanges();
    }

    private Observable<RxBleConnection> prepareConnectionObservable(RxBleDevice device) {
        return device
                .establishConnection(false)
                .takeUntil(mDisconnectionTrigger)
                .retryWhen(errors -> errors.filter(error -> error instanceof BleDisconnectedException))
                .compose(ReplayingShare.instance())
                ;
    }

    private Observable<RxBleConnection.RxBleConnectionState> getConnectionStateObservable(RxBleDevice device) {
        return device.observeConnectionStateChanges();
    }

    private void turnOnLight(Light light) {
        Log.d(TAG, "turnOnLight: " + light);
        clearWriteAnswer();
        byte lightId;
        switch (light) {
            case TAIL:
                lightId = 1;
                break;
            case LEFT:
                lightId = 2;
                break;
            case RIGHT:
                lightId = 3;
                break;
            default:
                Log.e(TAG, "turnOnLight: unknown light");
                lightId = 0;
                break;
        }
        byte[] data = {4, lightId, 0};
        mWriteDataTextView.setText(Arrays.toString(data));
        wrightCharacteristic(data);
    }

    private void turnLightsOff() {
        Log.d(TAG, "turnLightsOff()");
        clearWriteAnswer();
        byte[] data = {4, 0, 0};
        mWriteDataTextView.setText(Arrays.toString(data));
        wrightCharacteristic(data);
    }

    private void wrightCharacteristic(byte[] data) {
        mConnectionObservable
                .flatMapSingle(connection -> connection.writeCharacteristic(UUID_TRAILER_MODE, data))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        answer -> {
                            String answerString = Arrays.toString(answer);
                            String msg = "wrightCharacteristic: answer is " + answerString;
                            Log.d(TAG, msg);
//                            showToast(msg);
                            mWriteAnswerTextView.setText(answerString);
                        },
                        throwable -> {
                            String msg1 = "wrightCharacteristic: failure " + throwable.getClass().getSimpleName();
                            Log.e(TAG, msg1);
                            showToast(msg1);
                        },
                        () -> {
                            String msg2 = "wrightCharacteristic: onComplete()";
                            Log.d(TAG, msg2);
                            showToast(msg2);
                        }
                );
    }

    private void readCharacteristic() {
        clearReadAnswer();
        mConnectionObservable
                .flatMapSingle(connection -> connection.readCharacteristic(UUID_TRAILER_MODE))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        answer -> {
                            String answerString = Arrays.toString(answer);
                            String msg = "readCharacteristic: answer is " + answerString;
                            Log.d(TAG, msg);
//                            showToast(msg);
                            mReadAnswerTextView.setText(answerString);
                        },
                        throwable -> {
                            String msg1 = "readCharacteristic: failure " + throwable.getClass().getSimpleName();
                            Log.e(TAG, msg1);
                            showToast(msg1);
                        },
                        () -> {
                            String msg2 = "readCharacteristic: onComplete()";
                            Log.d(TAG, msg2);
                            showToast(msg2);
                        }
                );
    }

    private void connect() {
        mConnectionObservable
                .subscribe();
    }

    private void disconnect() {
        mDisconnectionTrigger.onNext(true);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void clearWriteAnswer() {
        mWriteAnswerTextView.setText("");
    }

    private void clearReadAnswer() {
        mReadAnswerTextView.setText("");
    }

    private void prepareViews() {
        mConnectionStateTextView = findViewById(R.id.connection_state_textview);
        mWriteDataTextView = findViewById(R.id.write_data_textview);
        mWriteAnswerTextView = findViewById(R.id.write_answer_textview);
        mReadAnswerTextView = findViewById(R.id.read_answer_textview);
        findViewById(R.id.tail_button).setOnClickListener(view -> turnOnLight(Light.TAIL));
        findViewById(R.id.left_button).setOnClickListener(view -> turnOnLight(Light.LEFT));
        findViewById(R.id.right_button).setOnClickListener(view -> turnOnLight(Light.RIGHT));
        findViewById(R.id.off_button).setOnClickListener(view -> turnLightsOff());
        findViewById(R.id.read_button).setOnClickListener(view -> readCharacteristic());
    }

    @Override
    protected void onStart() {
        super.onStart();
        connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnect();
    }
}
