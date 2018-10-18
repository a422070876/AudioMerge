package com.hyq.hm.audiomerge.mediacodec;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;

import com.hyq.hm.audiomerge.audio.AudioHolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 播放代码
 * Created by 海米 on 2018/10/17.
 */

public class AudioPlayer {

    private static final String AUDIO = "audio/";

    private Handler audioHandler;
    private HandlerThread audioThread;

    private AudioTrack audioTrack;

    public AudioPlayer(){
        audioThread = new HandlerThread("AudioPlayer");
        audioThread.start();
        audioHandler = new Handler(audioThread.getLooper());
    }

    public void start(final AudioHolder audioHolder){
        isStop = true;
        int mSampleRate = audioHolder.getSampleRate();
        int mChannels = audioHolder.getChannelCount();
        int bufferSize = AudioTrack.getMinBufferSize(
                mSampleRate,
                mChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        // make sure minBufferSize can contain at least 1 second of audio (16 bits sample).
        if (bufferSize < mChannels * mSampleRate * 2) {
            bufferSize = mChannels * mSampleRate * 2;
        }
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                mSampleRate,
                mChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        audioHandler.post(new Runnable() {
            @Override
            public void run() {
                decoder(audioHolder);
            }
        });
    }
    private boolean isStop = true;
    public void stop(){
        isStop = false;
    }
    public boolean isStop(){
        return audioTrack == null;
    }
    private int audioTrackIndex;
    private void decoder(AudioHolder holder){
        long startTime = (long) (holder.getStart()*1000*1000);
        long endTime = (long) (holder.getEnd()*1000*1000);
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
                    if(info.presentationTimeUs >= startTime && info.presentationTimeUs <= endTime){
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes,0,bytes.length);
                        data.clear();
                        audioTrack.write(bytes,0,bytes.length);
                    }
                }
                audioDecoder.releaseOutputBuffer(outIndex, false);
                if(info.presentationTimeUs >= endTime){
                    break;
                }
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break;
            }
            if(!isStop){
                break;
            }
        }

        audioDecoder.stop();
        audioDecoder.release();
        audioExtractor.release();
        if(isStop){
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        audioTrack.stop();
        audioTrack.flush();
        audioTrack.release();
        audioTrack = null;
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


}
