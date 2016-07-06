# VideoChatSDK

 AnychatSDK远程视频
 使用AnyChatSDK向用户提供远程客服功能（视频）,这里是对原开发包做的二次开发，提供用户的登录、排队、视频功能
### 登录
 类 AnyChat_Login 实现 AnyChatBaseEvent, AnyChatObjectEvent,AnyChatVideoCallEvent三个接口有兴趣的可以直接打开源码，这里就不做详细介绍
 * 服务器配置
```java
  //服务器地址
  QueueInfoEntity.getQueueInfoEntity().mStrIP = "";
  //服务器端口
  QueueInfoEntity.getQueueInfoEntity().mSPort = 8080;
```
 * 初始化
```java
 /**
	 * 
	 * @param context
	 *            上下文环境
	 * @param termId
	 *            用户名
	 * @param applyId
	 *            密码
	 */
  AnyChat_Login login = new AnyChat_Login(this,"","");
```
 * 设置监听
```java
  login.setCallback(new AnyChatCoreIml() {
            @Override
            public void getAnyChat_GetFunRoom(List<Integer> list) {
                //这里反馈的是用户当期的可提供服务的业务列表
                // 这里我让用户直接进入第一个房间，如果需要可以将List在页面上展示出来让用户手动选择，之后再调用下面的方法
                //记得用 用户选择的业务代码  替换list.get(0)
                 AnyChatCoreSDK.ObjectControl(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_AREA, list.get(0),
                        AnyChatObjectDefine.ANYCHAT_AREA_CTRL_USERENTER, 0, 0, 0, 0, "");
            }

            @Override
            public void getLoginStatus(LoginStatus status, int dwUserId) {
                //这里反馈登录状态，比如对话框的显示和关闭等等
            }

            @Override
            public void onUpdateQueueInfo(int dwObjectId) {
                if(entity.CurrentQueueId == dwObjectId){
                    //当前队列中的人数
                    int length = AnyChatCoreSDK.ObjectGetIntValue(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE,
                            dwObjectId, AnyChatObjectDefine.ANYCHAT_QUEUE_INFO_LENGTH);
                    //用户在队伍中的位置
                    int mbefore = AnyChatCoreSDK.ObjectGetIntValue(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE,
                            dwObjectId, AnyChatObjectDefine.ANYCHAT_QUEUE_INFO_BEFOREUSERNUM);
                    //上面两条数据实时更新
                    showTextView.setText("当前排队人数共:" + length + "人,您现在排在第 " + (mbefore + 1) + " 位");
                }
            }

            @Override
            public void onAnyChat_VieoResult(int obj) {
                    //这里表示已成功连接至服务器并得到客服的回应，这里我让用户直接进入视频页面
                    Intent intent = new Intent(VideoQueueActivity.this, VideoCallActivity.class);
		    startActivityForResult(intent, 0);
            }
        });
```
 * 其余配置
```java
  @Override
	protected void onDestroy() {
		super.onDestroy();
		//一旦调用 onDestroy方法就会将AnyChatSDK的所有服务关掉，
		login.onDestroy();
		BussinessCenter.getBussinessCenter().realse();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		login.onRestart();
	}
```
### 视频聊天

 这部分的局限性比较大，需要在进行视频的Activity中放入两个SurfaceView控件来展示我的视频和对方的视频（也可以加入两个ProgressBar来展示双方的音量），当然如果不满意可以直接参考AnychatSDK的原声DEMO
 
 类 AnyChat_VideoCall 实现AnyChatBaseEvent, AnyChatObjectEvent, AnyChatVideoCallEvent, AnyChatTextMsgEvent（接收文字消息）

 * 初始化
 
 ```java
    AnyChat_VideoCall videoCall = new AnyChat_VideoCall(this,mSurfaceViewSelf,mSurfaceViewRemote);
 ```
 * 设置监听
 ```java
       videoCall.setAnyChatVideCallIml(new AnyChatVideCallIml() {
            @Override
            public void onCloseViewCall() {
            	//代表视频结束
                VideoCallActivity.this.finish();
            }

            @Override
            public void onUserSpeakVolume(int self, int remote) {
	        //我的语音音量和对方的语音音量
            }

            @Override
            public void onTextMessage(int dwFromUserid, int dwToUserid, boolean bSecret, String message) {
               //接收到的文本消息
                Log.i("VideoCallActivity" , "dwFromUserid:" + dwFromUserid + ";dwToUserid:" + dwToUserid + ";MSG:" + message);
            }
        });
 ```
 * 其它
 ```java
      @Override
    protected void onResume() {
        super.onResume();
        videoCall.onResume(this);
    }

    @Override
    protected void onRestart() {
        videoCall.addOnListener();
        super.onRestart();
        videoCall.onBindVideoUser();
    }

    @Override
    protected void onDestroy() {
        videoCall.onDestory();
        super.onDestroy();
    }
 ```
 * 另外需要注意的是一定要屏蔽返回按钮，即使不屏蔽也要弹出对话框提示用户手动关闭并调用下面的方法
 ```java
      videoCall.closeViewCall();
 ```


