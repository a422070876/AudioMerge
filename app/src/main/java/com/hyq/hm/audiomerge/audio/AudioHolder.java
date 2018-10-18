package com.hyq.hm.audiomerge.audio;

/**
 * Audio属性类
 * Created by 海米 on 2018/10/16.
 */

public class AudioHolder {
    private String file;
    private String name;
    private double start;
    private double end;

    private int sampleRate;
    private int channelCount;
    private int bitRate;

    private String mp3;

    public void setMp3(String mp3) {
        this.mp3 = mp3;
    }

    public String getMp3() {
        return mp3;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }
}
