package com.noname.shijian;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PCMEncoderAAC {


    //比特率
    private final static int KEY_BIT_RATE = 96000;
    //读取数据的最大字节数
    private final static int KEY_MAX_INPUT_SIZE = 1024 * 1024;
    //声道数
    private final static int CHANNEL_COUNT = 2;
    private MediaCodec mediaCodec;
    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;
    private MediaCodec.BufferInfo encodeBufferInfo;
    private EncoderListener encoderListener;

    public PCMEncoderAAC(int sampleRate, EncoderListener encoderListener) {
        this.encoderListener = encoderListener;
        init(sampleRate);
    }

    /**
     * 初始化AAC编码器
     */
    private void init(int sampleRate) {
        try {
            //参数对应-> mime type、采样率、声道数
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                    sampleRate, CHANNEL_COUNT);
            //比特率
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, KEY_MAX_INPUT_SIZE);
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaCodec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaCodec.start();
        encodeInputBuffers = mediaCodec.getInputBuffers();
        encodeOutputBuffers = mediaCodec.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * @param data
     */
    public void encodeData(byte[] data) {
        //dequeueInputBuffer（time）需要传入一个时间值，-1表示一直等待，0表示不等待有可能会丢帧，其他表示等待多少毫秒
        //获取输入缓存的index
        int inputIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputIndex >= 0) {
            ByteBuffer inputByteBuf = encodeInputBuffers[inputIndex];
            inputByteBuf.clear();
            //添加数据
            inputByteBuf.put(data);
            //限制ByteBuffer的访问长度
            inputByteBuf.limit(data.length);
            //把输入缓存塞回去给MediaCodec
            mediaCodec.queueInputBuffer(inputIndex, 0, data.length, 0, 0);
        }
        //获取输出缓存的index
        int outputIndex = mediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
        while (outputIndex >= 0) {
            //获取缓存信息的长度
            int byteBufSize = encodeBufferInfo.size;
            //添加ADTS头部后的长度
            int bytePacketSize = byteBufSize + 7;
            //拿到输出Buffer
            ByteBuffer outPutBuf = encodeOutputBuffers[outputIndex];
            outPutBuf.position(encodeBufferInfo.offset);
            outPutBuf.limit(encodeBufferInfo.offset + encodeBufferInfo.size);

            byte[] aacData = new byte[bytePacketSize];
            //添加ADTS头部
            addADTStoPacket(aacData, bytePacketSize);
            /*
            get（byte[] dst,int offset,int length）:ByteBuffer从position位置开始读，读取length个byte，并写入dst下
            标从offset到offset + length的区域
             */
            outPutBuf.get(aacData, 7, byteBufSize);
            outPutBuf.position(encodeBufferInfo.offset);

            //编码成功
            if (encoderListener != null) {
                encoderListener.encodeAAC(aacData);
            }

            //释放
            mediaCodec.releaseOutputBuffer(outputIndex, false);
            outputIndex = mediaCodec.dequeueOutputBuffer(encodeBufferInfo, 0);
        }
    }

    /**
     * 添加ADTS头
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        // AAC LC
        int profile = 2;
        // 44.1KHz
        int freqIdx = 4;
        // CPE
        int chanCfg = 2;
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    public interface EncoderListener {
        void encodeAAC(byte[] data);
    }

}
