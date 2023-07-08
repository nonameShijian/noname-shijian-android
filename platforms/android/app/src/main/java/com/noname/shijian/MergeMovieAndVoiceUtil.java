package com.noname.shijian;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MergeMovieAndVoiceUtil {

    @SuppressLint("WrongConstant")
    public static void mergeAudio(String audioFile, String videoFile, String outputFile) {
        // 创建一个MediaExtractor对象，用于从音频文件中提取音频轨道
        MediaExtractor audioExtractor = new MediaExtractor();
        try {
            audioExtractor.setDataSource(audioFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // 检查音频文件是否有音频轨道，并获取其格式
        int audioTrackCount = audioExtractor.getTrackCount();
        if(audioTrackCount <= 0){
            System.out.println("No audio track found in " + audioFile);
            return;
        }
        Log.e("getTrackCount", String.valueOf(audioTrackCount));
        audioExtractor.selectTrack(0); // 选择第一个音频轨道
        MediaFormat audioFormat = audioExtractor.getTrackFormat(0);

        // 创建一个MediaExtractor对象，用于从视频文件中提取视频轨道
        MediaExtractor videoExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(videoFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // 检查视频文件是否有视频轨道，并获取其格式
        int videoTrackCount = videoExtractor.getTrackCount();
        if(videoTrackCount <= 0){
            System.out.println("No video track found in " + videoFile);
            return;
        }
        videoExtractor.selectTrack(0); // 选择第一个视频轨道
        MediaFormat videoFormat = videoExtractor.getTrackFormat(0);

        // 创建一个MediaMuxer对象，用于将音频和视频轨道复用到输出文件中
        MediaMuxer muxer = null;
        try {
            muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // 添加音频和视频轨道，并获取它们的索引
        int audioTrackIndex = muxer.addTrack(audioFormat);
        int videoTrackIndex = muxer.addTrack(videoFormat);
        // 开始复用
        muxer.start();

        // 定义一些变量，用于读取和写入音频和视频数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo(); // 用于存储媒体数据的元信息，如时间戳，大小，偏移量等
        int bufferSize = 256 * 1024; // 设置缓冲区的大小，根据需要调整
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize); // 创建一个字节缓冲区，用于存储媒体数据
        boolean isAudioDone = false; // 标记音频是否读取完毕
        boolean isVideoDone = false; // 标记视频是否读取完毕

        // 循环读取和写入音频和视频数据，直到其中一个结束
        while (!isAudioDone || !isVideoDone) {
            // 读取音频数据
            if (!isAudioDone) {
                // 从音频轨道中读取一帧数据到缓冲区中
                int audioSize = audioExtractor.readSampleData(buffer, 0);
                if (audioSize < 0) {
                    // 如果读取到了文件末尾，标记音频结束
                    isAudioDone = true;
                } else {
                    // 如果读取到了有效的数据，设置缓冲区信息
                    bufferInfo.size = audioSize;
                    bufferInfo.offset = 0;
                    bufferInfo.presentationTimeUs = audioExtractor.getSampleTime(); // 获取当前帧的时间戳
                    bufferInfo.flags = audioExtractor.getSampleFlags(); // 获取当前帧的标志位，如是否为关键帧等
                    // 将缓冲区中的数据写入到输出文件的音频轨道中
                    muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo);
                    // 移动到下一帧
                    audioExtractor.advance();
                }
            }

            // 读取视频数据
            if (!isVideoDone) {
                // 从视频轨道中读取一帧数据到缓冲区中
                int videoSize = videoExtractor.readSampleData(buffer, 0);
                if (videoSize < 0) {
                    // 如果读取到了文件末尾，标记视频结束
                    isVideoDone = true;
                } else {
                    // 如果读取到了有效的数据，设置缓冲区信息
                    bufferInfo.size = videoSize;
                    bufferInfo.offset = 0;
                    bufferInfo.presentationTimeUs = videoExtractor.getSampleTime(); // 获取当前帧的时间戳
                    bufferInfo.flags = videoExtractor.getSampleFlags(); // 获取当前帧的标志位，如是否为关键帧等
                    // 将缓冲区中的数据写入到输出文件的视频轨道中
                    muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                    // 移动到下一帧
                    videoExtractor.advance();
                }
            }
        }

        // 结束复用并释放资源
        muxer.stop();
        muxer.release();
        audioExtractor.release();
        videoExtractor.release();
    }

    public static void muxVideoAudio(String videoFilePath, String audioFilePath, String outputFile) {
        String TAG = "muxVideoAudio";
        try {
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoFilePath);
            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audioFilePath);
            MediaMuxer muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoExtractor.selectTrack(0);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
            int videoTrack = muxer.addTrack(videoFormat);
            audioExtractor.selectTrack(0);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
            int audioTrack = muxer.addTrack(audioFormat);
            Log.d(TAG, "Video Format " + videoFormat.toString());
            Log.d(TAG, "Audio Format " + audioFormat.toString());
            boolean sawEOS = false;
            int frameCount = 0;
            int offset = 100;
            int sampleSize = 256 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            muxer.start();
            while (!sawEOS) {
                videoBufferInfo.offset = offset;
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);
                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    sawEOS = true;
                    videoBufferInfo.size = 0;
                } else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
//noinspection WrongConstant
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo);
                    videoExtractor.advance();
                    frameCount++;
                }
            }
            boolean sawEOS2 = false;
            int frameCount2 = 0;
            while (!sawEOS2) {
                frameCount2++;
                audioBufferInfo.offset = offset;
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);
                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                    sawEOS2 = true;
                    audioBufferInfo.size = 0;
                } else {
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
//noinspection WrongConstant
                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
                    audioExtractor.advance();
                }
            }
            muxer.stop();
            muxer.release();
            Log.d(TAG,"Output: "+outputFile);
        } catch (IOException e) {
            Log.d(TAG, "Mixer Error 1 " + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Mixer Error 2 " + e.getMessage());
        }
    }

}