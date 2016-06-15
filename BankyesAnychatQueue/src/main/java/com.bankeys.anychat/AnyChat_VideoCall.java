package com.bankeys.anychat;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.bairuitech.anychat.AnyChatObjectDefine;
import com.bairuitech.anychat.AnyChatObjectEvent;
import com.bairuitech.anychat.AnyChatTextMsgEvent;
import com.bairuitech.anychat.AnyChatVideoCallEvent;
import com.bairuitech.common.ConfigEntity;
import com.bairuitech.common.ConfigService;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zhang on 2016/6/13.
 */
public class AnyChat_VideoCall implements AnyChatBaseEvent, AnyChatObjectEvent, AnyChatVideoCallEvent, AnyChatTextMsgEvent {

    /**
     * 对象自己的摄像头
     */
    private SurfaceView mSurfaceSelf;
    /**
     * 对方的摄像头
     */
    private SurfaceView mSurfaceRemote;

    private AnyChatCoreSDK anyChatCoreSDK;
    private AnyChatVideCallIml anyChatVideCallIml;

    private ConfigEntity mConfigEntity;
    private Timer mTimerCheckAv;
    private TimerTask mTimerTask;
    private Timer mTimerShowVideoTime;
    private Handler mHandler;

    private Activity context;
    private boolean bSelfVideoOpened = false;
    private boolean bOtherVideoOpened = false;
    private boolean bVideoViewLoaded = true;
    private int dwTargetUserId;
    private int videoIndex = 0;
    private int videocallSeconds = 0;


    public static final String TAG             = "AnyChat_VideoCall";
    public static final int USERTYPE_CUSTOM    = 0;
    public static final int USERTYPE_AGENT     = 2;
    public static final int MSG_CHECKAV        = 1;
    public static final int MSG_TIMEUPDATE     = 2;
    public static final int PROGRESSBAR_HEIGHT = 5;

    public AnyChat_VideoCall(Activity context, SurfaceView mSurfaceSelf, SurfaceView mSurfaceRemote) {
        this.context = context;
        this.mSurfaceRemote = mSurfaceRemote;
        this.mSurfaceSelf = mSurfaceSelf;
        dwTargetUserId = QueueInfoEntity.getQueueInfoEntity().TargetUserId;
        initSDK();
        initView();
        updateAV();
    }

    public void setAnyChatVideCallIml(AnyChatVideCallIml anyChatVideCallIml) {
        this.anyChatVideCallIml = anyChatVideCallIml;
    }

    public void onBindVideoUser() {
        if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
            videoIndex = anyChatCoreSDK.mVideoHelper.bindVideo(mSurfaceRemote.getHolder());
            anyChatCoreSDK.mVideoHelper.SetVideoUser(videoIndex, dwTargetUserId);
        }
    }

    public void addOnListener() {
        if (anyChatCoreSDK == null) {
            anyChatCoreSDK = new AnyChatCoreSDK();
        }
        anyChatCoreSDK.SetBaseEvent(this);
        anyChatCoreSDK.SetVideoCallEvent(this);
        anyChatCoreSDK.SetObjectEvent(this);
    }

    public void onResume(Activity context) {
        BussinessCenter.mContext = context;
    }

    /**
     * 退出队列并关闭当前视频聊天
     */
    public void closeViewCall() {
        //退出队列
        AnyChatCoreSDK.ObjectControl(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE, QueueInfoEntity.getQueueInfoEntity().CurrentQueueId, AnyChatObjectDefine.ANYCHAT_QUEUE_CTRL_USERLEAVE, 0, 0, 0, 0, "");
        //调用内核方法执行；
        BussinessCenter.VideoCallControl(AnyChatDefine.BRAC_VIDEOCALL_EVENT_FINISH, dwTargetUserId, 0,
                0, BussinessCenter.selfUserId, "");
    }

    public void onDestory() {
        anyChatCoreSDK.UserCameraControl(-1, 0);
        anyChatCoreSDK.UserSpeakControl(-1, 0);
        anyChatCoreSDK.UserSpeakControl(dwTargetUserId, 0);
        anyChatCoreSDK.UserCameraControl(dwTargetUserId, 0);
        mTimerCheckAv.cancel();
        mTimerShowVideoTime.cancel();
        anyChatCoreSDK.LeaveRoom(-1);
    }

    private void initSDK() {
        addOnListener();
        anyChatCoreSDK.SetTextMessageEvent(this);
        anyChatCoreSDK.mSensorHelper.InitSensor(context);
        AnyChatCoreSDK.mCameraHelper.SetContext(context);
        //进入房间
        anyChatCoreSDK.EnterRoom(QueueInfoEntity.getQueueInfoEntity().RoomId, "");
    }

    private void initView() {
        mConfigEntity = ConfigService.LoadConfig(context);
        if (mSurfaceRemote != null) {
            mSurfaceRemote.setTag(dwTargetUserId);
        }

        if (mSurfaceSelf != null) {
            mSurfaceSelf.setZOrderOnTop(true);
            if (mConfigEntity.videoOverlay != 0) {
                mSurfaceSelf.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        }

        // 视频如果是采用java采集
        if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
            mSurfaceSelf.getHolder().addCallback(AnyChatCoreSDK.mCameraHelper);
            Log.i("ANYCHAT", "VIDEOCAPTRUE---" + "JAVA");
        }

        // 视频显示如果是采用java采集，SurfacecallBack
        if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
            videoIndex = anyChatCoreSDK.mVideoHelper.bindVideo(mSurfaceRemote.getHolder());
            anyChatCoreSDK.mVideoHelper.SetVideoUser(videoIndex, dwTargetUserId);
            Log.i("ANYCHAT", "VIDEOSHOW---" + "JAVA");
        }

        if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
            if (AnyChatCoreSDK.mCameraHelper.GetCameraNumber() > 1) {
                AnyChatCoreSDK.mCameraHelper
                        .SelectVideoCapture(AnyChatCoreSDK.mCameraHelper.CAMERA_FACING_FRONT);
            }
        } else {
            String[] strVideoCaptures = anyChatCoreSDK.EnumVideoCapture();
            if (strVideoCaptures != null && strVideoCaptures.length > 1) {
                //
                for (int i = 0; i < strVideoCaptures.length; i++) {
                    String strDevices = strVideoCaptures[i];
                    if (strDevices.indexOf("Front") >= 0) {
                        anyChatCoreSDK.SelectVideoCapture(strDevices);
                        break;
                    }
                }
            }
        }
        Util.openSpeaker(context);
    }

    private void updateAV() {
        mHandler = new Handler(new Handler.Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CHECKAV:
                        // 实时视频刷新
                        CheckVideoStatus();
                        // 实时音频数据
                        updateVolume();
                        break;
                    case MSG_TIMEUPDATE:
                        // mTxtTime.setText(BaseMethod.getTimeShowString(videocallSeconds++));
                        break;
                }
                return false;
            }
        });
        initTimerCheckAv();
        initTimerShowTime();
    }

    private void CheckVideoStatus() {
        if (!bOtherVideoOpened) {

            if (anyChatCoreSDK.GetCameraState(dwTargetUserId) == 2
                    && anyChatCoreSDK.GetUserVideoWidth(dwTargetUserId) != 0) {
                SurfaceHolder holder = mSurfaceRemote.getHolder();

                if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) != AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
                    holder.setFormat(PixelFormat.RGB_565);
                    holder.setFixedSize(anyChatCoreSDK.GetUserVideoWidth(-1),
                            anyChatCoreSDK.GetUserVideoHeight(-1));
                }
                Surface s = holder.getSurface();
                if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
                    anyChatCoreSDK.mVideoHelper.SetVideoUser(videoIndex, dwTargetUserId);
                } else
                    anyChatCoreSDK.SetVideoPos(dwTargetUserId, s, 0, 0, 0, 0);
                bOtherVideoOpened = true;
            }
        }

        if (!bSelfVideoOpened) {
            if (anyChatCoreSDK.GetCameraState(-1) == 2 && anyChatCoreSDK.GetUserVideoWidth(-1) != 0) {
                SurfaceHolder holder = mSurfaceSelf.getHolder();

                if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) != AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
                    holder.setFormat(PixelFormat.RGB_565);
                    holder.setFixedSize(anyChatCoreSDK.GetUserVideoWidth(-1),
                            anyChatCoreSDK.GetUserVideoHeight(-1));
                }

                Surface s = holder.getSurface();
                anyChatCoreSDK.SetVideoPos(-1, s, 0, 0, 0, 0);
                bSelfVideoOpened = true;
            }
        }
    }

    private void updateVolume() {
        anyChatVideCallIml.onUserSpeakVolume(anyChatCoreSDK.GetUserSpeakVolume(-1),anyChatCoreSDK.GetUserSpeakVolume(dwTargetUserId));
    }

    private void initTimerCheckAv() {
        if (mTimerCheckAv == null)
            mTimerCheckAv = new Timer();
        mTimerTask = new TimerTask() {

            @Override
            public void run() {
                mHandler.sendEmptyMessage(MSG_CHECKAV);
            }
        };
        mTimerCheckAv.schedule(mTimerTask, 1000, 100);
    }

    private void initTimerShowTime() {
        if (mTimerShowVideoTime == null)
            mTimerShowVideoTime = new Timer();
        mTimerTask = new TimerTask() {

            @Override
            public void run() {
                mHandler.sendEmptyMessage(MSG_TIMEUPDATE);
            }
        };
        mTimerShowVideoTime.schedule(mTimerTask, 100, 1000);
    }

    @Override
    public void OnAnyChatConnectMessage(boolean bSuccess) {

    }

    @Override
    public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {

    }

    @Override
    public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {
        Log.i(TAG, "OnAnyChatEnterRoomMessag" + dwRoomId + " enter " + dwErrorCode);

        if (dwErrorCode == 0) {
            anyChatCoreSDK.UserCameraControl(-1, 1);
            anyChatCoreSDK.UserSpeakControl(-1, 1);
            bSelfVideoOpened = false;
        }
    }

    @Override
    public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId) {
        Log.i(TAG, "OnAnyChatOnlineUserMessage:" + dwUserNum + " enter " + dwRoomId);
        anyChatCoreSDK.UserCameraControl(dwTargetUserId, 1);
        anyChatCoreSDK.UserSpeakControl(dwTargetUserId, 1);
        bOtherVideoOpened = false;
    }

    @Override
    public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {
        Log.i(TAG, "OnAnyChatUserAtRoomMessage:" + dwUserId + " enter " + bEnter);
        anyChatCoreSDK.UserCameraControl(dwTargetUserId, 1);
        anyChatCoreSDK.UserSpeakControl(dwTargetUserId, 1);
        bOtherVideoOpened = false;
    }

    @Override
    public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
        Log.i(TAG, "OnAnyChatLinkCloseMessage:" + dwErrorCode);
        anyChatCoreSDK.UserCameraControl(-1, 0);
        anyChatCoreSDK.UserSpeakControl(-1, 0);
        anyChatCoreSDK.UserSpeakControl(dwTargetUserId, 0);
        anyChatCoreSDK.UserCameraControl(dwTargetUserId, 0);
    }

    @Override
    public void OnAnyChatObjectEvent(int dwObjectType, int dwObjectId, int dwEventType, int dwParam1, int dwParam2, int dwParam3, int dwParam4, String strParam) {

    }

    @Override
    public void OnAnyChatTextMessage(int dwFromUserid, int dwToUserid, boolean bSecret, String message) {
        Log.i(TAG, " 来自服务器的文本消息， dwToUserId" + dwToUserid + ";msg:" + message);
    }

    @Override
    public void OnAnyChatVideoCallEvent(int dwEventType, int dwUserId, int dwErrorCode, int dwFlags, int dwParam, String userStr) {
        Log.i(TAG, "dwUserId:" + dwUserId + "userStr:" + userStr);
        switch (dwEventType) {
            case AnyChatDefine.BRAC_VIDEOCALL_EVENT_FINISH:
                Log.i(TAG, "视频通话已结束");
                if (QueueInfoEntity.getQueueInfoEntity().userType == USERTYPE_AGENT) {
                    AnyChatCoreSDK.ObjectControl(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_AGENT,
                            QueueInfoEntity.getQueueInfoEntity().mUserID,
                            AnyChatObjectDefine.ANYCHAT_AGENT_CTRL_FINISHSERVICE, 0, 0, 0, 0, "");
                }
                if (anyChatVideCallIml != null)
                    anyChatVideCallIml.onCloseViewCall();
                break;
        }
    }
}
