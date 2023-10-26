/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.noname.shijian;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.noname.shijian.tbs.X5ProcessInitService;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsCommonCode;
import com.tencent.smtt.sdk.TbsListener;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;
import org.jeremyup.cordova.x5engine.X5WebView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends CordovaActivity {
    public final static int FILE_CHOOSER_RESULT_CODE = 1;

    public static String movieDirPath;

    // 录屏所需变量
    public static MediaProjectionManager mProjectionManager;
    public static MediaProjection mMediaProjection;
    public static VirtualDisplay mVirtualDisplay;
    public static MediaRecorder mMediaRecorder;
    public static final int MediaRecord_REQUEST_CODE = 100;
    public static boolean isRecording = false;
    public static Intent mResultData = null;
    public static File videoFile = null;

    public static AudioRecord audioRecord = null;

    public static File audioFile = null;
    // 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
    public static final int SAMPLE_RATE_INHZ = 44100;

    // 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;

    // 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // 获取录音的缓存大小
    public static final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);

    // 录音的工作线程
    public static Thread recordingAudioThread;

    private PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);

    @Override
    public CordovaWebViewEngine makeWebViewEngine() {
        if (false) {
            initX5();
            return new org.jeremyup.cordova.x5engine.X5WebViewEngine(this, this.preferences);
        }
        return super.makeWebViewEngine();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOG.e("onCreate" ,"111");
        super.onCreate(savedInstanceState);
        super.init();
        startX5WebProcessPreinitService();

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        if (extras != null) {
            String ext = extras.getString("extensionImport");
            if (ext != null) {
                LOG.e("ext" ,ext);
                FinishImport.ext = ext;
            }
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        View view = appView.getView();
        Log.e("webview", String.valueOf(view));
        if (view instanceof org.jeremyup.cordova.x5engine.X5WebView) {
            X5WebView webview = (X5WebView) view;
            com.tencent.smtt.sdk.WebSettings settings = webview.getSettings();
            int textZoom = settings.getTextZoom();
            Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
            settings.setTextZoom(100);
            String userAgent = settings.getUserAgentString();
            settings.setUserAgentString(userAgent + " WebViewFontSize/100% 无名杀诗笺版/" + FinishImport.getAppVersion(MainActivity.this));
            webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this, MainActivity.this, webview) , "noname_shijianInterfaces");
            org.jeremyup.cordova.x5engine.X5WebView.setWebContentsDebuggingEnabled(true);
        } else {
            SystemWebView webview = (SystemWebView) view;
            WebSettings settings = webview.getSettings();
            int textZoom = settings.getTextZoom();
            Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
            settings.setTextZoom(100);
            String userAgent = settings.getUserAgentString();
            settings.setUserAgentString(userAgent + " WebViewFontSize/100% 无名杀诗笺版/" + FinishImport.getAppVersion(MainActivity.this));
            webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this, MainActivity.this, webview) , "noname_shijianInterfaces");
            WebView.setWebContentsDebuggingEnabled(true);
        }

        //startService(new Intent(this, ScreenRecordingService.class));
        //mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        movieDirPath = getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LOG.e("onNewIntent" ,"111");
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getExtras() != null) {
            String ext = intent.getExtras().getString("extensionImport");
            if (ext != null) {
                LOG.e("ext" ,ext);
                FinishImport.ext = ext;
            }
        }
    }

    @Override
    public void onDestroy() {
        // 获取缓存目录
        File tempDir = getExternalCacheDir();
        File[] tempFiles = tempDir.listFiles();
        if (tempFiles != null) {
            for (File tempFile : tempFiles) {
                tempFile.delete();
            }
        }
        // 停止录屏并销毁文件
        if (isRecording) {
            stopRecording(false, null);
        }

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 停止录屏并销毁文件
        if (isRecording) {
            stopRecording(false, null);
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.e("onActivityResult-requestCode", String.valueOf(requestCode));
        Log.e("onActivityResult-resultCode", String.valueOf(resultCode));
        Log.e("onActivityResult-intent", String.valueOf(intent));
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri result = intent.getData();
                // 处理文件Uri对象
                Intent newIntent = new Intent(this, NonameImportActivity.class);
                newIntent.setData(result);
                newIntent.setAction(Intent.ACTION_VIEW);
                startActivity(newIntent);
            }
        } else if (requestCode == MediaRecord_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                mResultData = intent;
                startRecording();
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private static volatile boolean recordingThreadNotRun = true;

    /** 开始录屏 */
    private void startRecording() {
        try {
            setFileName();
            mMediaRecorder = new MediaRecorder();
            mMediaProjection = mProjectionManager.getMediaProjection(Activity.RESULT_OK, mResultData);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                AudioRecord.Builder builder = new AudioRecord.Builder();

                builder.setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_INHZ)
                        .setChannelMask(CHANNEL_CONFIG)
                        .setEncoding(AUDIO_FORMAT)
                        .build())
                        .setBufferSizeInBytes(minBufferSize);

                AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mMediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();

                builder.setAudioPlaybackCaptureConfig(config);

                try {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        audioRecord = builder.build();
                        int state = audioRecord.getState();
                        if (AudioRecord.STATE_INITIALIZED != state) {
                            audioRecord = null;
                            throw new Exception("AudioRecord无法初始化，请检查录制权限或者是否其他app没有释放录音器");
                        }
                    }
                } catch (Exception e) {
                    Log.e("录音器错误", "录音器初始化失败");
                }
            } else {
                Toast.makeText(this, "System Audio Capture is not Supported on this Device", Toast.LENGTH_LONG).show();
            }

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;
            int screenDensity = metrics.densityDpi;
            // mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            //  mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoSize(getScreenWidth(this), getScreenHeight(this));
            mMediaRecorder.setVideoFrameRate(80);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMediaRecorder.setOutputFile(videoFile); // 文件路径
            } else {
                ParcelFileDescriptor fd = null;
                try {
                    fd = ParcelFileDescriptor.open(videoFile, ParcelFileDescriptor.MODE_READ_WRITE);
                    mMediaRecorder.setOutputFile(ParcelFileDescriptor.fromFd(fd.getFd()).getFileDescriptor());
                } catch (IOException e) {
                    Log.e("setOutputFile", String.valueOf(e));
                } finally {
                    if (fd != null) {
                        try {
                            fd.close();
                        } catch (IOException e) {
                            Log.e("fd.close", String.valueOf(e));
                        }
                    }
                }
            }
            // mMediaRecorder.setAudioEncodingBitRate(5 * 1024 * 1024);
            mMediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024); //设置编码比特率
            mMediaRecorder.setMaxDuration(1000 * 60 * 60); // 最长录制一小时
            try {
                mMediaRecorder.prepare();
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture",
                        screenWidth, screenHeight, screenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mMediaRecorder.getSurface(), null, null);
            } catch (IOException e) {
                Log.e("mMediaRecorder.prepare", String.valueOf(e));
            }

            mMediaRecorder.start();

            if (audioRecord != null) {
                recordingAudioThread = new Thread(() -> {
                    if (audioRecord == null) return;
                    recordingThreadNotRun = false;
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(audioFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e(TAG, "临时缓存文件未找到");
                    }
                    if (fos == null) {
                        recordingThreadNotRun = true;
                        return;
                    }
                    byte[] data = new byte[minBufferSize];
                    int read;
                    audioRecord.startRecording();
                    final FileOutputStream ffos = fos;
                    PCMEncoderAAC pcmEncoderAAC = new PCMEncoderAAC(SAMPLE_RATE_INHZ, new PCMEncoderAAC.EncoderListener() {
                        @Override
                        public void encodeAAC(byte[] data) {
                            try {
                                ffos.write(data);
                            }catch (Throwable e){

                            }
                        }
                    });
                    while (isRecording && recordingAudioThread != null) {
                        if (audioRecord != null) {
                            read = audioRecord.read(data, 0, minBufferSize);
                            if (read > 0) {
                                /*
                                try {
                                    fos.write(data,0,read);
                                    fos.flush();
                                    Log.i("audioRecordTest", "写录音数据->" + read);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (Throwable e){
                                    Log.e("audioRecordTest","",e);
                                    e.printStackTrace();
                                }*/
                                pcmEncoderAAC.encodeData(data);
                            }
                        }
                    }
                    if(audioRecord!=null) {
                        audioRecord.stop();
                        audioRecord.release();
                        audioRecord = null;
                    }

                    try {
                        // 关闭数据流
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    recordingThreadNotRun = true;
                });

                recordingAudioThread.start();
            }
            isRecording = true;
        }catch (Throwable e){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,"error:"+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /** 停止录屏 */
    public static void stopRecording(boolean keepFile, String fileName) {
        if (!isRecording) return;

        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (recordingAudioThread != null) {
            recordingAudioThread = null;
        }

        isRecording = false;

        if (!keepFile && videoFile != null) {
            if (videoFile.exists()) {
                videoFile.delete();
            }
        }

        if (!keepFile && audioFile != null) {
            if (audioFile.exists()) {
                audioFile.delete();
            }
        }

        if (!keepFile) return;

        if (videoFile != null) {
            Log.e("stopRecording", "文件保存在：" + videoFile.getAbsolutePath());
        }

        if (audioFile != null) {
            Log.e("stopRecording", "文件保存在：" + audioFile.getAbsolutePath());
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.e("mergeAudioAndVideo",fileName);

        Thread thread = new Thread(){
            public void run() {
                while (!recordingThreadNotRun){
                    Log.e("mergeAudioAndVideo","waiting");
                }
                String currentTime = formatter.format(new Date());
                MergeMovieAndVoiceUtil.mergeAudio(audioFile.getAbsolutePath(), videoFile.getAbsolutePath(), movieDirPath + "/" + fileName + "-" + currentTime + ".mp4");
                Log.e("mergeAudioAndVideo", movieDirPath + "/" + fileName + "-" + currentTime + ".mp4");
                if (videoFile != null && videoFile.exists()) {
                    videoFile.delete();
                }
                videoFile = null;
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
                audioFile = null;
            }
        };

        if (videoFile != null && audioFile != null) thread.start();
    }

    /** 生成视频音频文件名 */
    private void setFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentTime = formatter.format(new Date());
        String fileName = "video_" + currentTime + ".mp4";
        String fileName2 = "audio_" + currentTime + ".aac";

        videoFile = new File(movieDirPath, fileName);

        if (videoFile.exists()) {
            videoFile.delete();
        }

        try {
            videoFile.createNewFile();
        } catch (IOException e) {
            Log.e("setVideoFile", "创建文件失败");
        }

        //if (audioRecord != null) {
            audioFile = new File(movieDirPath, fileName2);
            if (audioFile.exists()) {
                audioFile.delete();
            }

            try {
                audioFile.createNewFile();
            } catch (IOException e) {
                Log.e("setAudioFile", "创建文件失败");
            }
        //}
    }

    /**
     * 获取屏幕的宽度px
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.widthPixels;
    }

    /**
     * 获取屏幕的高度px
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.heightPixels;
    }

    private ProgressDialog dialog;

    private void initX5() {
        Context context = this;
        dialog = new ProgressDialog(this);
        dialog.setTitle("正在下载X5内核");
        dialog.setCancelable(false);
        dialog.setIcon(R.mipmap.ic_launcher);
        // 水平进度条
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();

        /* 设置允许移动网络下进行内核下载。默认不下载，会导致部分一直用移动网络的用户无法使用x5内核 */
        QbSdk.setDownloadWithoutWifi(true);

        QbSdk.setCoreMinVersion(QbSdk.CORE_VER_ENABLE_202207);

        Log.e("initX5Environment", "initX5Environment");

        QbSdk.setTbsListener(new TbsListener() {
            /**
             * @param stateCode 用户可处理错误码请参考{@link TbsCommonCode}
             */
            @Override
            public void onDownloadFinish(int stateCode) {
                Log.i("App", "onDownloadFinished: " + stateCode);
                if (stateCode == TbsCommonCode.DOWNLOAD_SUCCESS) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.setTitle("正在安装内核");
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.dismiss();
                    });
                }
            }

            /**
             * @param stateCode 用户可处理错误码请参考{@link TbsCommonCode}
             */
            @Override
            public void onInstallFinish(int stateCode) {
                Log.i("App", "onInstallFinished: " + stateCode);
                String m;
                if (stateCode == TbsCommonCode.INSTALL_SUCCESS) {
                    m = "内核安装完成";
                    Log.i("App", m);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.dismiss();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dialog.dismiss();
                    });
                }
            }

            /**
             * 首次安装应用，会触发内核下载，此时会有内核下载的进度回调。
             * @param progress 0 - 100
             */
            @Override
            public void onDownloadProgress(int progress) {
                Log.i("App", "Core Downloading: " + progress);
                new Handler(Looper.getMainLooper()).post(() -> {
                    dialog.setProgress(progress);
                });
            }
        });

        /* 此过程包括X5内核的下载、预初始化，接入方不需要接管处理x5的初始化流程，希望无感接入 */
        QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                // 内核初始化完成，可能为系统内核，也可能为系统内核
            }

            /**
             * 预初始化结束
             * 由于X5内核体积较大，需要依赖wifi网络下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
             * 内核下发请求发起有24小时间隔，卸载重装、调整系统时间24小时后都可重置
             * 调试阶段建议通过 WebView 访问 debugtbs.qq.com -> 安装线上内核 解决
             * @param isX5 是否使用X5内核
             */
            @Override
            public void onViewInitFinished(boolean isX5) {
                Log.i("onViewInitFinished", String.valueOf(isX5));
                // hint: you can use QbSdk.getX5CoreLoadHelp(context) anytime to get help.
                Log.i("getX5CoreLoadHelp", QbSdk.getX5CoreLoadHelp(context));
            }
        });

    }

    /**
     * 启动X5 独立Web进程的预加载服务。优点：
     * 1、后台启动，用户无感进程切换
     * 2、启动进程服务后，有X5内核时，X5预加载内核
     * 3、Web进程Crash时，不会使得整个应用进程crash掉
     * 4、隔离主进程的内存，降低网页导致的App OOM概率。
     *
     * 缺点：
     * 进程的创建占用手机整体的内存，demo 约为 150 MB
     */
    @SuppressLint("LongLogTag")
    private void startX5WebProcessPreinitService() {
        String currentProcessName = QbSdk.getCurrentProcessName(this);
        // 设置多进程数据目录隔离，不设置的话系统内核多个进程使用WebView会crash，X5下可能ANR
        com.tencent.smtt.sdk.WebView.setDataDirectorySuffix(QbSdk.getCurrentProcessName(this));
        Log.e("startX5WebProcessPreinitService", currentProcessName);
        if (currentProcessName.equals(this.getPackageName())) {
            this.startService(new Intent(this, X5ProcessInitService.class));
        }
    }
}