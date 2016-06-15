package com.bankeys.anychat;

import android.content.Context;
import android.media.AudioManager;

/**
 * Created by zhang on 2016/6/13.
 */
public class Util {

    /**
     * 打开系统扬声器
     */
    public static void openSpeaker(Context context) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setSpeakerphoneOn(true);
    }
}
