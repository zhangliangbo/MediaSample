package com.mcivicm.media.helper;

import android.Manifest;
import android.app.Activity;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;

import com.mcivicm.media.AudioActivity;
import com.mcivicm.media.audio.PCMAudioSource;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

/**
 * 语音录制助手
 */

public class AudioRecordHelper {

    public static final int sampleRate = 44100;//采样率

    /**
     * 请求录音权限
     *
     * @param activity
     * @return
     */
    public static Observable<Boolean> recordAudioPermission(final Activity activity) {
        return new RxPermissions(activity)
                .request(Manifest.permission.RECORD_AUDIO)
                .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return Observable.just(true);
                        } else {
                            return new RxPermissions(activity)
                                    .shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
                                    .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                                        @Override
                                        public ObservableSource<Boolean> apply(Boolean aBoolean) throws Exception {
                                            if (aBoolean) {
                                                return Observable.error(new Exception("您需要授权录音权限才能开始录音"));
                                            } else {
                                                return Observable.error(new Exception("您已拒绝了录音权限并选择不再提醒，如需重新打开录音权限，请在设置->权限里面打开"));
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * pcm实时语音数据
     *
     * @return
     */
    public static Observable<byte[]> pcmAudioData(int sampleRate, int channelFormat, int audioFormat) {
        return new PCMAudioSource(createInstance(sampleRate, channelFormat, audioFormat), null);
    }

    /**
     * pcm实时语音数据
     *
     * @param sampleRate
     * @param channelFormat
     * @param audioFormat
     * @param recordFinishedCallback 非主线程调用，【不要】执行UI相关操作
     * @return
     */
    public static Observable<byte[]> pcmAudioData(int sampleRate, int channelFormat, int audioFormat, Runnable recordFinishedCallback) {
        return new PCMAudioSource(createInstance(sampleRate, channelFormat, audioFormat), recordFinishedCallback);
    }

    /**
     * 新建一个实例
     *
     * @param sampleRate
     * @param channelFormat
     * @param audioFormat
     * @return
     */
    public static AudioRecord createInstance(int sampleRate, int channelFormat, int audioFormat) {
        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                sampleRate,//适用于所有设备
                channelFormat,
                audioFormat,
                AudioRecord.getMinBufferSize(sampleRate, channelFormat, audioFormat)//两倍的缓冲
        );
        //噪声抑制
        NoiseSuppressor noiseSuppressor = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
        }
        if (noiseSuppressor != null) {
            if (!noiseSuppressor.getEnabled()) {
                noiseSuppressor.setEnabled(true);
            }
        }
        //回声消除
        AcousticEchoCanceler acousticEchoCanceler = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            acousticEchoCanceler = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
        }
        if (acousticEchoCanceler != null) {
            if (!acousticEchoCanceler.getEnabled()) {
                acousticEchoCanceler.setEnabled(true);
            }
        }
        return audioRecord;
    }

    /**
     * 根据pcm数据计算音量
     *
     * @param pcm
     */
    public static double calculateVolume(byte[] pcm, int offset, int len) {
        if (pcm == null || pcm.length == 0) return 0D;
        byte[] useful = new byte[len];
        System.arraycopy(pcm, offset, useful, 0, len);
        long total = 0;
        for (byte one : useful) {
            total += one * one;
        }
        double mean = (double) total / (double) len;
        return 20 * Math.log10(mean);
    }

    /**
     * 根据pcm数据计算音量
     *
     * @param pcm
     */
    public static double calculateVolume(byte[] pcm) {
        if (pcm == null || pcm.length == 0) return 0D;
        return calculateVolume(pcm, 0, pcm.length);
    }

}

