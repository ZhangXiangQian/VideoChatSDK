package com.bankeys.anychat;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.bairuitech.anychat.AnyChatObjectDefine;
import com.bairuitech.anychat.AnyChatObjectEvent;
import com.bairuitech.anychat.AnyChatVideoCallEvent;
import com.bairuitech.common.ConfigEntity;
import com.bairuitech.common.ConfigService;
import com.bairuitech.common.DialogFactory;
import com.bairuitech.common.ScreenInfo;


public class AnyChat_Login implements AnyChatBaseEvent, AnyChatObjectEvent,
		AnyChatVideoCallEvent {
	private static final String TAG    = "AnyChat_Login";
	private String mStrName            = "Tom";
	private String passWord;
	private List<Integer> mobject = new ArrayList<Integer>(); // 装载营业厅ID
	private int[] queueIds;

	private int USER_TYPE_ID = 0 ; // 0代表是进入客户界面，2代表是接入座席界面

	public AnyChatCoreSDK anyChatSDK;
	private AnyChatCoreIml callback;
	private Activity context;
	private QueueInfoEntity entity;

	/**
	 * 
	 * @param context
	 *            上下文环境
	 * @param termId
	 *            终端号 （用户名）
	 * @param applyId
	 *            业务ID（Password）
	 */
	public AnyChat_Login(Activity context, String termId, String applyId) {
		this.context = context;
		this.mStrName = termId;
		this.passWord = applyId;
		entity = QueueInfoEntity.getQueueInfoEntity();
		setDisPlayMetrics();
		ApplyVideoConfig();
		InitSDK();
	}

	public void setCallback(AnyChatCoreIml callback) {
		this.callback = callback;
	}


	// 初始化SDK
	private void InitSDK() {
		if (anyChatSDK == null) {
			anyChatSDK = AnyChatCoreSDK.getInstance(context);
		}
		anyChatSDK.SetBaseEvent(this);// 基本事件
		anyChatSDK.SetObjectEvent(this);// 营业厅排队事件
		anyChatSDK.SetVideoCallEvent(this);
		anyChatSDK.InitSDK(android.os.Build.VERSION.SDK_INT, 0);// 初始化sdk
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_AUTOROTATION, 1);
	}

	public void loginAnyChat() {
		callback.getLoginStatus(LoginStatus.Waiting, -1);
		anyChatSDK.Connect(QueueInfoEntity.getQueueInfoEntity().mStrIP, QueueInfoEntity.getQueueInfoEntity().mSPort);
		// 注册广播
		registerBoradcastReceiver();
	}

	// 对键盘显示进行控制
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm.isActive()) {
			imm.hideSoftInputFromWindow(context.getCurrentFocus()
					.getApplicationWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	/**
	 * 屏幕大小
	 */
	private void setDisPlayMetrics() {
		DisplayMetrics dMetrics = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
		ScreenInfo.WIDTH = dMetrics.widthPixels;
		ScreenInfo.HEIGHT = dMetrics.heightPixels;
	}

	public void onDestroy() {
		anyChatSDK.LeaveRoom(-1);
		anyChatSDK.Logout();
		anyChatSDK.Release();
		context.unregisterReceiver(mBroadcastReceiver);
	}

	public void onRestart() {
		anyChatSDK.SetBaseEvent(this);
		anyChatSDK.SetObjectEvent(this);
	}

	@Override
	public void OnAnyChatConnectMessage(boolean bSuccess) {
		if (!bSuccess) {
			Log.e(TAG, "连接服务器失败，自动重连，请稍后...");
			callback.getLoginStatus(LoginStatus.Reconnection, -1);
		}
		anyChatSDK.Login(mStrName, passWord);
	}

	@Override
	public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
		// 连接成功

		if (dwErrorCode == 0) {
			// hideKeyboard();
			// 保存用户id和用户角色信息
			entity.selfUserName = mStrName;
			entity.mUserID = dwUserId;
			entity.userType = USER_TYPE_ID;
			Log.e(TAG, "Connect to the server success");
			// 初始化业务对象属性身份
			InitClientObjectInfo(dwUserId);
			callback.getLoginStatus(LoginStatus.Success, dwUserId);

		} else {
			Log.e(TAG, "登录失败，errorCode：" + dwErrorCode);
			callback.getLoginStatus(LoginStatus.Failure, -1);
		}
	}

	// 初始化服务对象事件；触发回调OnAnyChatObjectEvent函数
	private void InitClientObjectInfo(int dwUserId) {
		// 业务对象身份初始化；0代表普通客户，2是代表座席 (USER_TYPE_ID)
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_OBJECT_INITFLAGS,
				USER_TYPE_ID);
		// 业务对象优先级设定；
		int dwPriority = 10;
		AnyChatCoreSDK.ObjectSetIntValue(
				AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_CLIENTUSER, dwUserId,
				AnyChatObjectDefine.ANYCHAT_OBJECT_INFO_PRIORITY, dwPriority);
		// 业务对象属性设定,必须是-1；
		int dwAttribute = -1;
		AnyChatCoreSDK.ObjectSetIntValue(
				AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_CLIENTUSER, dwUserId,
				AnyChatObjectDefine.ANYCHAT_OBJECT_INFO_ATTRIBUTE, dwAttribute);
		// 向服务器发送数据同步请求指令
		AnyChatCoreSDK.ObjectControl(
				AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_AREA,
				AnyChatObjectDefine.ANYCHAT_INVALID_OBJECT_ID,
				AnyChatObjectDefine.ANYCHAT_OBJECT_CTRL_SYNCDATA, dwUserId, 0,
				0, 0, "");
	}

	@Override
	public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {

	}

	@Override
	public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId) {

	}

	@Override
	public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {

	}

	// 网络端口
	@Override
	public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
		anyChatSDK.LeaveRoom(-1);
		anyChatSDK.Logout();
		Log.e(TAG, "连接关闭，errorCode：" + dwErrorCode);
		callback.getLoginStatus(LoginStatus.UnKnownErr, -1);

	}

	// 广播
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("NetworkDiscon")) {
				anyChatSDK.LeaveRoom(-1);
				anyChatSDK.Logout();
				callback.getLoginStatus(LoginStatus.NoNet, -1);
			}
		}
	};

	private void registerBoradcastReceiver() {
		IntentFilter myIntentFilter = new IntentFilter();
		myIntentFilter.addAction("NetworkDiscon");
		// 注册广播
		context.registerReceiver(mBroadcastReceiver, myIntentFilter);
	}

	@Override
	public void OnAnyChatObjectEvent(int dwObjectType, int dwObjectId,
			int dwEventType, int dwParam1, int dwParam2, int dwParam3,
			int dwParam4, String strParam) {
		Log.e("OnAnyChatObjectEvent", "----------------");
		switch (dwEventType) {

		// 营业厅数据同步，回调一次返回一个营业厅对象id（有多少营业厅回调多少次）
		case AnyChatObjectDefine.ANYCHAT_OBJECT_EVENT_UPDATE:
			// 装入集合
			mobject.add(dwObjectId);

			break;

		// 进入营业厅结果回调
		case AnyChatObjectDefine.ANYCHAT_AREA_EVENT_ENTERRESULT:

			AnyChatEnterAreaResult(dwObjectType, dwObjectId, dwParam1);
			break;

		case AnyChatObjectDefine.ANYCHAT_AREA_EVENT_LEAVERESULT:
			Log.e(TAG, "退出营业厅");
			context.finish();
			break;
		// 数据同步完成回调
		case AnyChatObjectDefine.ANYCHAT_OBJECT_EVENT_SYNCDATAFINISH:

			DataFinshed(dwObjectType);
			break;
		case AnyChatObjectDefine.ANYCHAT_QUEUE_EVENT_ENTERRESULT:
			break;
		case AnyChatObjectDefine.ANYCHAT_QUEUE_EVENT_STATUSCHANGE:
			callback.onUpdateQueueInfo(dwObjectId);
			break;
		}
	}

	// 进入营业厅的回调，跳转到业务列表
	private void AnyChatEnterAreaResult(int dwObjectType, int dwObjectId,
			int dwParam1) {
		// dwParam1 进入营业厅返回结果，0表示进入营业厅成功
		Log.e(TAG, "TAG:" + "dwObjectType:" + dwObjectType + ";dwObjectId:"
				+ dwObjectId + ";dwParam1:" + dwParam1);
		// 客户角色
		if (entity.userType == 0) {
			startDeal();
			// 座席角色
		}
	}

	private void DataFinshed(int dwObjectType) {
		Log.e("TAG", "dwObjectType:" + dwObjectType);
		if (dwObjectType == AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_AREA) {
			// 适配器的数据获取
			List<String> list = new ArrayList<String>();
			for (int index = 0; index < mobject.size(); index++) {
				String name = AnyChatCoreSDK.ObjectGetStringValue(
						AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_AREA,
						mobject.get(index),
						AnyChatObjectDefine.ANYCHAT_OBJECT_INFO_NAME);
				list.add(name);
				Log.e("TAG", "TAG:" + index + "; name:" + name);
			}
			callback.getAnyChat_GetFunRoom(mobject);
		}
	}

	/**
	 * 根据配置文件配置视频参数
	 */
	private void ApplyVideoConfig() {
		ConfigEntity configEntity = ConfigService.LoadConfig(context);
		if (configEntity.configMode == 1) // 自定义视频参数配置
		{
			// 设置本地视频编码的码率（如果码率为0，则表示使用质量优先模式）
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_BITRATECTRL,
					configEntity.videoBitrate);
			if (configEntity.videoBitrate == 0) {
				// 设置本地视频编码的质量
				AnyChatCoreSDK.SetSDKOptionInt(
						AnyChatDefine.BRAC_SO_LOCALVIDEO_QUALITYCTRL,
						configEntity.videoQuality);
			}
			// 设置本地视频编码的帧率
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_FPSCTRL,
					configEntity.videoFps);
			// 设置本地视频编码的关键帧间隔
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_GOPCTRL,
					configEntity.videoFps * 4);
			// 设置本地视频采集分辨率
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL,
					configEntity.resolution_width);
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL,
					configEntity.resolution_height);
			// 设置视频编码预设参数（值越大，编码质量越高，占用CPU资源也会越高）
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_PRESETCTRL,
					configEntity.videoPreset);
		}
		// 让视频参数生效
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_APPLYPARAM,
				configEntity.configMode);
		// P2P设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_NETWORK_P2PPOLITIC,
				configEntity.enableP2P);
		// 本地视频Overlay模式设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_OVERLAY,
				configEntity.videoOverlay);
		// 回音消除设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_AUDIO_ECHOCTRL,
				configEntity.enableAEC);
		// 平台硬件编码设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_CORESDK_USEHWCODEC,
				configEntity.useHWCodec);
		// 视频旋转模式设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_ROTATECTRL,
				configEntity.videorotatemode);
		// 本地视频采集偏色修正设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_FIXCOLORDEVIA,
				configEntity.fixcolordeviation);
		// 视频GPU渲染设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_VIDEOSHOW_GPUDIRECTRENDER,
				configEntity.videoShowGPURender);
		// 本地视频自动旋转设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_AUTOROTATION,
				configEntity.videoAutoRotation);
	}

	@Override
	public void OnAnyChatVideoCallEvent(int dwEventType, int dwUserId,
			int dwErrorCode, int dwFlags, int dwParam, String userStr) {
		Log.e("OnAnyChatVideoCallEvent", "dwEventType:" + dwEventType
				+ ";dwErrorCode:" + dwErrorCode);
		switch (dwEventType) {

		case AnyChatDefine.BRAC_VIDEOCALL_EVENT_REQUEST:
			// 呼叫请求事件
			BussinessCenter.getBussinessCenter().onVideoCallRequest(dwUserId,
					dwFlags, dwParam, userStr);
			entity.targetUserName = anyChatSDK.GetUserName(dwUserId);
			Dialog dialog = DialogFactory.getDialog(DialogFactory.DIALOGID_REQUEST,
					dwUserId, context,entity);
			dialog.show();
			break;

		case AnyChatDefine.BRAC_VIDEOCALL_EVENT_REPLY:
			// 呼叫成功的时候的所做出的反应；
			Log.e("queueactivity", "呼叫成功等待对方反应的回调");
			BussinessCenter.getBussinessCenter().onVideoCallReply(dwUserId,
					dwErrorCode, dwFlags, dwParam, userStr, callback);
			if (dwErrorCode == AnyChatDefine.BRAC_ERRORCODE_SUCCESS) {

			} else {
			}
			break;

		case AnyChatDefine.BRAC_VIDEOCALL_EVENT_START:
			Log.e("queueactivity", "会话开始回调");
			entity.TargetUserId = dwUserId;
			entity.RoomId = dwParam;
			BussinessCenter.getBussinessCenter().stopSessionMusic();
			callback.onAnyChat_VieoResult(dwUserId);
			break;
		}
	}


	/**
	 * 立即办理
	 */
	public void startDeal() {
		queueIds = AnyChatCoreSDK
				.ObjectGetIdList(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE);
		for (int i = 0; i < queueIds.length; i++) {
			String name = AnyChatCoreSDK.ObjectGetStringValue(
					AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE, queueIds[i],
					AnyChatObjectDefine.ANYCHAT_OBJECT_INFO_NAME);
			int number = AnyChatCoreSDK.ObjectGetIntValue(
					AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE, queueIds[i],
					AnyChatObjectDefine.ANYCHAT_QUEUE_INFO_LENGTH);
			Log.e("TAG", "业务字符名称:" + name + ";业务排队人数:" + number);
		}
		if (queueIds != null && queueIds.length != 0) {
			entity.CurrentQueueId = (queueIds[0]);
			// 进入队列的控制指令
			AnyChatCoreSDK.ObjectControl(
					AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE, queueIds[0],
					AnyChatObjectDefine.ANYCHAT_QUEUE_CTRL_USERENTER, 0, 0, 0,
					0, "");
		} else {
			entity.CurrentQueueId = 0;
			// 进入队列的控制指令
			AnyChatCoreSDK.ObjectControl(
					AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE, 0,
					AnyChatObjectDefine.ANYCHAT_QUEUE_CTRL_USERENTER, 0, 0, 0,
					0, "");
		}
	}
}
