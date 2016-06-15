package com.bankeys.anychat;

public class QueueInfoEntity {
	public int userType;          // 用户类型：普通客户、座席两种
	public int CurrentAreaId;     // 当前营业厅的Id
	public int CurrentQueueId;    // 当前队列Id
	public String selfUserName;   // 本地用户名字
	public String targetUserName; // 对方用户名字
	public int RoomId;            // 进入房间号
	public int TargetUserId;      // 对方用户Id
	public int mUserID;           //
	/** 服务器地址 */
	public  String mStrIP = "10.7.7.111";
	/** 服务器端口 */
	public int mSPort    = 8906;

	private QueueInfoEntity() {
	}

	private static QueueInfoEntity entity;

	public static QueueInfoEntity getQueueInfoEntity() {
		if(entity == null){
			entity = new QueueInfoEntity();
		}
		return entity;
	}

}
