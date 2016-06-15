package com.bankeys.anychat;

/**
 * Created by zhang on 2016/6/15.
 */
public interface AnyChatVideCallIml {

    public void onCloseViewCall();

    public void onUserSpeakVolume(int self, int remote);

    public void onTextMessage(int dwFromUserid, int dwToUserid, boolean bSecret, String message);
}