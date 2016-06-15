package com.bankeys.anychat;

import java.util.List;

/**
 * Created by zhang on 2016/6/15.
 */
public interface AnyChatCoreIml {

    public void getLoginStatus(LoginStatus loginStatus, int i);

    public void onUpdateQueueInfo(int dwObjectId);

    public void getAnyChat_GetFunRoom(List<Integer> mobject);

    public void onAnyChat_VieoResult(int dwUserId);
}
