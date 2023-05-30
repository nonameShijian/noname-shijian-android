package com.noname.shijian;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

public class MediaService extends Service {
    private final String NOTIFICATION_CHANNEL_ID="com.noname.shijian.MediaService";
    private final String NOTIFICATION_CHANNEL_NAME="com.noname.shijian.channel_name";
    private final String NOTIFICATION_CHANNEL_DESC="com.noname.shijian.channel_desc";
    public MediaService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startNotification();
    }

    public void startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Call Start foreground with notification
            Intent notificationIntent = new Intent(this, MediaService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_stat_noname))
                    .setSmallIcon(R.drawable.ic_stat_noname)
                    .setContentTitle("Starting Service")
                    .setContentText("Starting monitoring service")
                    .setContentIntent(pendingIntent);
            Notification notification = notificationBuilder.build();
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            startForeground(1, notification); //必须使用此方法显示通知，不能使用notificationManager.notify，否则还是会报上面的错误
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int currentResultCode = intent.getIntExtra("resultCode", 0);
        Intent resultData = intent.getParcelableExtra("resultData");
        int minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getBaseContext().getSystemService(MEDIA_PROJECTION_SERVICE);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(currentResultCode, Objects.requireNonNull(resultData));
        AudioRecord.Builder builder = new AudioRecord.Builder();
        builder.setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build())
                .setBufferSizeInBytes(minBufferSize);
        AudioPlaybackCaptureConfiguration config =
                new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();
        builder.setAudioPlaybackCaptureConfig(config);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                AudioRecord audioRecord = builder.build();
            }
        } catch (Exception e) {
            Log.e("录音器错误", "录音器初始化失败");
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
