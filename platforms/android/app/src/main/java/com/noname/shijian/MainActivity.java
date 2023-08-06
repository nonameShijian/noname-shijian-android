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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;

import java.io.DataOutputStream;
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
    public void onCreate(Bundle savedInstanceState) {
        LOG.e("onCreate" ,"111");
        super.onCreate(savedInstanceState);
        super.init();

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

        WebView.setWebContentsDebuggingEnabled(true);

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        SystemWebView webview = (SystemWebView) appView.getView();
        WebSettings settings = webview.getSettings();
        int textZoom = settings.getTextZoom();
        Log.e("textZoom", "WebView当前的字体变焦百分比是: " + textZoom + "%");
        settings.setTextZoom(100);
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " WebViewFontSize/100% 无名杀诗笺版/" + FinishImport.getAppVersion(MainActivity.this));
        webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this, MainActivity.this, webview) , "noname_shijianInterfaces");

        //startService(new Intent(this, ScreenRecordingService.class));
        //mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        movieDirPath = getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        // movieDirPath = getExternalFilesDir(null).getAbsolutePath();
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
            stopRecording(false);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 停止录屏并销毁文件
        if (isRecording) {
            stopRecording(false);
        }
    }

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
    public static void stopRecording(boolean keepFile) {
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

        /*
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        } else {
            if (audioFile.exists()) {
                audioFile.delete();
            }
        }*/

        if (recordingAudioThread != null) {
            //recordingAudioThread.interrupt();
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

        Thread thread = new Thread(){
            public void run() {
                while (!recordingThreadNotRun){
                    Log.e("mergeAudioAndVideo","waiting");
                }
                MergeMovieAndVoiceUtil.mergeAudio(audioFile.getAbsolutePath()/*.replace("pcm", "mp3")*/, videoFile.getAbsolutePath(), movieDirPath + "/对局录像.mp4");
                //MergeMovieAndVoiceUtil.muxVideoAudio(audioFile.getAbsolutePath(), videoFile.getAbsolutePath(), movieDirPath + "/对局录像.mp4");
                Log.e("mergeAudioAndVideo", movieDirPath + "/对局录像.mp4");

//                    if (videoFile.exists()) {
//                        videoFile.delete();
//                    }
//                    if (audioFile.exists()) {
//                        audioFile.delete();
//                    }

                videoFile = null;
                audioFile = null;

                //this.interrupt();
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
}