package com.mcivicm.media.camera2;

import android.hardware.camera2.CameraManager;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 摄像头的可用性
 */

public class AvailabilityObservable extends Observable<Pair<String, Boolean>> {

    private CameraManager cameraManager = null;

    public AvailabilityObservable(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @Override
    protected void subscribeActual(Observer<? super Pair<String, Boolean>> observer) {
        observer.onSubscribe(new AvailabilityAdapter(observer));
    }


    private class AvailabilityAdapter extends CameraManager.AvailabilityCallback implements Disposable {

        private Observer<? super Pair<String, Boolean>> observer;

        private AtomicBoolean disposed = new AtomicBoolean(false);

        AvailabilityAdapter(Observer<? super Pair<String, Boolean>> observer) {
            this.observer = observer;
            if (cameraManager != null) {
                cameraManager.registerAvailabilityCallback(this, null);
            }
        }

        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            super.onCameraAvailable(cameraId);
            //千万注意：当打开的CameraDevice调用关闭之后，它所对应的cameraId会立即变成可用状态，从而触发该事件
            if (!isDisposed()) {
                observer.onNext(Pair.create(cameraId, true));
            }
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            super.onCameraUnavailable(cameraId);
            //千万注意：当一个CameraDevice调用打开之后，它所对应的cameraId会立即变成不可用状态，从而触发该事件
            if (!isDisposed()) {
                observer.onNext(Pair.create(cameraId, false));
            }
        }

        @Override
        public void dispose() {
            if (!disposed.get()) {
                cameraManager.unregisterAvailabilityCallback(this);
                disposed.set(true);
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }
}
