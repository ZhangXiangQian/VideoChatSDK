package com.bankeys.anychat.test;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import com.bankeys.anychat.AnyChatVideCallIml;
import com.bankeys.anychat.AnyChat_VideoCall;


public class VideoCallActivity extends AppCompatActivity {
    private AnyChat_VideoCall videoCall;
    private SurfaceView mSurfaceViewSelf,mSurfaceViewRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_OK);
        setContentView(R.layout.activity_video_call);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSurfaceViewRemote = (SurfaceView) findViewById(R.id.mSurfaceViewRemote);
        mSurfaceViewSelf  = (SurfaceView) findViewById(R.id.mSurfaceViewSelf);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        videoCall = new AnyChat_VideoCall(this,mSurfaceViewSelf,mSurfaceViewRemote);
        videoCall.setAnyChatVideCallIml(new AnyChatVideCallIml() {
            @Override
            public void onCloseViewCall() {
                VideoCallActivity.this.finish();
            }

            @Override
            public void onUserSpeakVolume(int self, int remote) {

            }

            @Override
            public void onTextMessage(int dwFromUserid, int dwToUserid, boolean bSecret, String message) {
                Log.i("VideoCallActivity" , "dwFromUserid:" + dwFromUserid + ";dwToUserid:" + dwToUserid + ";MSG:" + message);
            }
        });

    }

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

}
