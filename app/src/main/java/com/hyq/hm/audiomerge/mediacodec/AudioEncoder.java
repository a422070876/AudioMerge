package com.hyq.hm.audiomerge.mediacodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.hyq.hm.audiomerge.audio.AudioHolder;
import com.hyq.hm.audiomerge.lame.SimpleLame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 海米 on 2018/10/16.
 */

public class AudioEncoder {
    private static final String AUDIO = "audio/";

    private Handler audioHandler;
    private HandlerThread audioThread;
    public AudioEncoder(){
        audioThread = new HandlerThread("AudioEncoder");
        audioThread.start();
        audioHandler = new Handler(audioThread.getLooper());
    }
    private OnAudioEncoderListener encoderListener;

    public void setEncoderListener(OnAudioEncoderListener encoderListener) {
        this.encoderListener = encoderListener;
    }

    public void start(final String path, final List<AudioHolder> list){
        audioHandler.post(new Runnable() {
            @Override
            public void run() {
                encoders(path,list);
            }
        });
    }

    public void start(final String path, final List<AudioHolder> list,OnAudioEncoderListener encoderListener){
        this.encoderListener = encoderListener;
        start(path,list);
    }
    private static int[] SampleRates = {48000,44100,32000,24000,22050,16000,12000,11025,8000};
    private static int[] Mpeg1BitRates = {320,256,224,192,160,128,112,96,80,64,56,48,40,32};
    private static int[] Mpeg2BitRates = {160,144,128,112,96,80,64,56,48,40,32,24,16,8};
    private static int[] Mpeg25BitRates = {64,56,48,40,32,24,16,8};

    private int audioTrackIndex;
    private AudioHolder decoderHolder = null;
    /**
     * 进行解码和拼接
     */
    private void encoders(String path,List<AudioHolder> list){
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        //统一采样率，比特率和声道
        int bitRate = list.get(0).getBitRate();
        int sampleRate = list.get(0).getSampleRate();
        int channelCount = list.get(0).getChannelCount();
        if(list.size() != 1){
            for (AudioHolder holder:list){
                bitRate = Math.min(bitRate,holder.getBitRate());
                sampleRate = Math.min(sampleRate,holder.getSampleRate());
                channelCount = Math.min(channelCount,holder.getChannelCount());
            }
            sampleRate = format(sampleRate,SampleRates);
            if(sampleRate >= SampleRates[2]){
                bitRate = format(bitRate,Mpeg1BitRates);
            }else if(sampleRate <= SampleRates[6]){
                bitRate = format(bitRate,Mpeg25BitRates);
            }else{
                bitRate = format(bitRate,Mpeg2BitRates);
            }
        }

        //临时用的pcm文件
        String pcm = Environment.getExternalStorageDirectory().getAbsolutePath()+"/HMSDK/"+System.currentTimeMillis()+".pcm";
        List<String> mp3s = new ArrayList<>();
        //总时长，用来计算进度用的
        long duration = 0;
        for (AudioHolder holder :list){
            //只有1个音频的时候直接转mp3
            String mp3;
            if(list.size() == 1){
                mp3 = path;
                decoderHolder = null;
            }else{
                decoderHolder = holder;
                mp3 = Environment.getExternalStorageDirectory().getAbsolutePath()+"/HMSDK/"+System.currentTimeMillis()+".mp3";
            }
            //将音频解码成pcm文件
            duration += decoderPCM(holder,pcm);
            //把pcm文件转成mp3
            SimpleLame.convert(this,pcm,mp3
                    ,holder.getSampleRate(),
                    channelCount,sampleRate,bitRate,
                    1
            );
            mp3s.add(mp3);
        }
        //只有一个音频就完成操作
        if(list.size() == 1){
            if(encoderListener != null){
                encoderListener.onOver(path);
            }
            return;
        }
        //以下可换成其他代码，比如用MediaCodec转成aac，因为采样率,比特率和声道都是一样的文件
        decoderHolder = null;
        File f = new File(pcm);
        if(f.exists()){
            f.delete();
        }
        OutputStream pcmos = null;
        try {
            pcmos = new FileOutputStream(pcm);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //文件总大小
        long total = 0;
        for (String mp3 : mp3s){
            //将mp3转成pcm文件返回转换数据的大小
            total += encoderMP3(mp3,pcmos,total,duration);
        }
        try {
            pcmos.flush();
            pcmos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //把pcm文件转成mp3
        SimpleLame.convert(this,pcm,path
                ,sampleRate,
                channelCount,sampleRate,bitRate,
                1
        );
        if(encoderListener != null){
            encoderListener.onOver(path);
        }

    }
    /**
     * 进行解码
     */
    private long decoderPCM(AudioHolder holder,String pcm){
        long startTime = (long) (holder.getStart()*1000*1000);
        long endTime = (long) (holder.getEnd()*1000*1000);
        //初始化MediaExtractor和MediaCodec
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaCodec audioDecoder = null;
        try {
            audioExtractor.setDataSource(holder.getFile());
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat format = audioExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith(AUDIO)){
                    audioExtractor.selectTrack(i);
                    audioTrackIndex = i;
                    if(startTime != 0){
                        audioExtractor.seekTo(startTime,audioTrackIndex);
                    }
                    audioDecoder = MediaCodec.createDecoderByType(mime);
                    audioDecoder.configure(format, null, null, 0);
                    audioDecoder.start();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        File f = new File(pcm);
        if(f.exists()){
            f.delete();
        }
        //pcm文件
        OutputStream pcmos = null;
        try {
            pcmos = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //这段音频的时长
        long duration = endTime - startTime;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (true) {
            extractorInputBuffer(audioExtractor, audioDecoder);
            int outIndex = audioDecoder.dequeueOutputBuffer(info, 50000);
            if (outIndex >= 0) {
                ByteBuffer data = audioDecoder.getOutputBuffer(outIndex);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    info.size = 0;
                }
                if (info.size != 0) {
                    //判断解码出来的数据是否在截取的范围内
                    if(info.presentationTimeUs >= startTime && info.presentationTimeUs <= endTime){
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes,0,bytes.length);
                        data.clear();
                        //写入pcm文件
                        try {
                            pcmos.write(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //进度条
                        if(encoderListener != null){
                            int progress = (int) (((info.presentationTimeUs - startTime)*50)/duration);
                            if(decoderHolder == null){
                                encoderListener.onEncoder(progress);
                            }else{
                                encoderListener.onDecoder(decoderHolder,progress);
                            }
                        }
                    }
                }
                audioDecoder.releaseOutputBuffer(outIndex, false);
                //超过截取时间结束解码
                if(info.presentationTimeUs >= endTime){
                    break;
                }
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break;
            }
        }
        try {
            pcmos.flush();
            pcmos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioDecoder.stop();
        audioDecoder.release();
        audioExtractor.release();
        return duration;
    }
    /**
     * mp3转pcm
     */
    private long encoderMP3(String mp3,OutputStream pcmos,long startTime,long duration){
        long d = 0;
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaCodec audioDecoder = null;
        try {
            audioExtractor.setDataSource(mp3);
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat format = audioExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith(AUDIO)){
                    d = format.getLong(MediaFormat.KEY_DURATION);
                    audioExtractor.selectTrack(i);
                    audioDecoder = MediaCodec.createDecoderByType(mime);
                    audioDecoder.configure(format, null, null, 0);
                    audioDecoder.start();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (true) {
            extractorInputBuffer(audioExtractor, audioDecoder);
            int outIndex = audioDecoder.dequeueOutputBuffer(info, 50000);
            if (outIndex >= 0) {
                ByteBuffer data = audioDecoder.getOutputBuffer(outIndex);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    info.size = 0;
                }
                if (info.size != 0) {
                    byte[] bytes = new byte[data.remaining()];
                    data.get(bytes,0,bytes.length);
                    data.clear();
                    try {
                        pcmos.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(encoderListener != null){
                        int progress = (int) (((info.presentationTimeUs + startTime)*50)/duration);
                        encoderListener.onEncoder(progress);
                    }
                }
                audioDecoder.releaseOutputBuffer(outIndex, false);
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break;
            }
        }
        audioDecoder.stop();
        audioDecoder.release();
        audioExtractor.release();
        return d;
    }
    private void extractorInputBuffer(MediaExtractor mediaExtractor, MediaCodec mediaCodec) {
        int inputIndex = mediaCodec.dequeueInputBuffer(50000);
        if (inputIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
            long sampleTime = mediaExtractor.getSampleTime();
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (mediaExtractor.advance()) {
                mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);
            } else {
                if (sampleSize > 0) {
                    mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
            }
        }
    }
    private int format(int f,int[] fs){
        if(f >= fs[0]){
            return fs[0];
        }else if(f <= fs[fs.length - 1]){
            return fs[fs.length - 1];
        }else{
            for (int i = 1; i < fs.length;i++){
                if(f >= fs[i]){
                    return fs[i];
                }
            }
        }
        return -1;
    }
    /**
     * jni回调的进度条函数，进度条以解码占50，pcm转mp3占50
     */
    public void setProgress(long size,long total){
        if(encoderListener != null){
            int progress = 50 + (int) ((total*50)/size);
            if(decoderHolder == null){
                encoderListener.onEncoder(progress);
            }else{
                encoderListener.onDecoder(decoderHolder,progress);
            }
        }
    }

    public interface OnAudioEncoderListener{
        void onDecoder(AudioHolder decoderHolder,int progress);
        void onEncoder(int progress);
        void onOver(String path);
    }

}
