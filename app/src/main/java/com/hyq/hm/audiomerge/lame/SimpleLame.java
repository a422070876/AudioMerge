package com.hyq.hm.audiomerge.lame;

import com.hyq.hm.audiomerge.mediacodec.AudioEncoder;

/**
 * Created by clam314 on 2017/3/26
 */

public class SimpleLame {
    static {
        System.loadLibrary("native-lib");
    }
    /**
     * pcm文件转换mp3函数
     */
    public static native void convert(AudioEncoder encoder,String jwav, String jmp3,
                                      int inSampleRate, int outChannel, int outSampleRate, int outBitrate,
                                      int quality);
}
