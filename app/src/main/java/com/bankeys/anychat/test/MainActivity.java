package com.bankeys.anychat.test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatObjectDefine;
import com.bankeys.anychat.AnyChatCoreIml;
import com.bankeys.anychat.AnyChat_Login;
import com.bankeys.anychat.BussinessCenter;
import com.bankeys.anychat.LoginStatus;
import com.bankeys.anychat.QueueInfoEntity;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private QueueInfoEntity entity;
    private AnyChat_Login login;
    private TextView showTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QueueInfoEntity.getQueueInfoEntity().mStrIP = "demo.anychat.cn";
        QueueInfoEntity.getQueueInfoEntity().mSPort = 8906;
        startLogin();
    }

    private void startLogin() {

        entity = QueueInfoEntity.getQueueInfoEntity();
        login = new AnyChat_Login(this, "android1", "");
        showTextView = (TextView) findViewById(R.id.txtShowInfo);

        login.setCallback(new AnyChatCoreIml() {
            @Override
            public void getAnyChat_GetFunRoom(List<Integer> list) {
                //这里反馈的是对应的 空闲客服列表
                AnyChatCoreSDK.ObjectControl(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_AREA, list.get(0),
                        AnyChatObjectDefine.ANYCHAT_AREA_CTRL_USERENTER, 0, 0, 0, 0, "");
            }

            @Override
            public void getLoginStatus(LoginStatus status, int dwUserId) {
                //这里反馈登录状态，比如添加对话框操作等等
            }

            @Override
            public void onUpdateQueueInfo(int dwObjectId) {
                if (entity.CurrentQueueId == dwObjectId) {
                    int length = AnyChatCoreSDK.ObjectGetIntValue(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE,
                            dwObjectId, AnyChatObjectDefine.ANYCHAT_QUEUE_INFO_LENGTH);
                    int mbefore = AnyChatCoreSDK.ObjectGetIntValue(AnyChatObjectDefine.ANYCHAT_OBJECT_TYPE_QUEUE,
                            dwObjectId, AnyChatObjectDefine.ANYCHAT_QUEUE_INFO_BEFOREUSERNUM);
                    showTextView.setText("当前排队人数共:" + length + "人,您现在排在第 " + (mbefore + 1) + " 位");
                }
            }

            @Override
            public void onAnyChat_VieoResult(int obj) {
                //这里表示已成功连接至服务器并得到客服的回应
                Intent intent = new Intent(MainActivity.this,VideoCallActivity.class);
                startActivityForResult(intent,0);
            }
        });
        login.loginAnyChat();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            finish();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        login.onRestart();
    }

    @Override
    protected void onResume() {
        BussinessCenter.mContext = this;
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        login.onDestroy();
        BussinessCenter.getBussinessCenter().realse();
    }
}
