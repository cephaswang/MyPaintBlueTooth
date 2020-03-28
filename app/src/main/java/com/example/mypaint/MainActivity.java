package com.example.mypaint;

// https://www.ssaurel.com/blog/learn-to-create-a-paint-application-for-android/
// https://www.youtube.com/watch?v=uJGcmGXaQ0o
// Learn to create a Paint Application with Android Studio

//  Swift 4.2 Draw Something with CGContext and Canvas View
//  https://www.youtube.com/watch?v=E2NTCmEsdSE
//  https://www.youtube.com/watch?v=7vDfL0K6Jm8

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.HashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private SeekBar sb_normal;
    // 调试信息
    private static final String TAG = "MainActivity";

    private PaintView paintView;
    private final static int REQ_PERMISSIONS = 0;
    Button  buttonBack;

    private PaintHandler mPaintHandler;
    private DrawHandler  mDrawHandler;

    Bitmap  bmScreen;
    View    screen;

    // Constants that indicate the current connection state
    public static final int MESSAGE_FONT_SIZE  = 1;       // we're doing nothing
    public static final int MESSAGE_FONT_COLOR = 2;     // now listening for incoming connections
    public static final int MESSAGE_PATH_START = 3; // now initiating an outgoing connection
    public static final int MESSAGE_PATH_MOVE  = 4;  // now connected to a remote device
    public static final int MESSAGE_PATH_ENDS  = 5;  // now connected to a remote device
    public static final int MESSAGE_ACTS_UNDO  = 6;  // now connected to a remote device
    public static final int MESSAGE_ACTS_CLEAR = 7;  // now connected to a remote device

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        paintView = (PaintView) findViewById(R.id.paintView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);

        mPaintHandler = new PaintHandler();
        mDrawHandler  = new DrawHandler();

        paintView.setHandler(mPaintHandler);
        paintView.init(metrics);

        screen = (View) findViewById(R.id.paintView);
        sb_normal  = (SeekBar) findViewById(R.id.seekBar);
        buttonBack = (Button) findViewById(R.id.buttonBack);

        sb_normal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // txt_cur.setText("当前进度值:" + progress + "  / 100 ");
                paintView.setStrokeWidth(progress + 1);
                buttonBack.setText( String.valueOf(progress + 1) );
                mPaintHandler.obtainMessage(MESSAGE_FONT_SIZE,(progress + 1),0,"").sendToTarget();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
               // Toast.makeText(mContext, "触碰SeekBar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
               // Toast.makeText(mContext, "放开SeekBar", Toast.LENGTH_SHORT).show();
                buttonBack.setText("Undo");
            }
        });

        askPermissions();

        drawAct();
    }

    public void drawAct(){

        paintView.SET_FromRemove(true);

        mDrawHandler.obtainMessage(MESSAGE_FONT_SIZE,(20),0,"").sendToTarget();

        mDrawHandler.obtainMessage(MESSAGE_FONT_COLOR,Color.RED,0,"").sendToTarget();

        String path = "404.96704,488.96484";
        mDrawHandler.obtainMessage(MainActivity.MESSAGE_PATH_START,0,0,path).sendToTarget();

        path = "410.46716,510.96484";
        mDrawHandler.obtainMessage(MainActivity.MESSAGE_PATH_MOVE,0,0,path).sendToTarget();

        path = "310.46716,410.96484";
        mDrawHandler.obtainMessage(MainActivity.MESSAGE_PATH_MOVE,0,0,path).sendToTarget();

        path = "410.46716,510.96484";
        mDrawHandler.obtainMessage(MainActivity.MESSAGE_PATH_ENDS,0,0,"").sendToTarget();

    //    mDrawHandler.obtainMessage(MESSAGE_ACTS_CLEAR,0,0,"").sendToTarget();
     //   mDrawHandler.obtainMessage(MESSAGE_ACTS_UNDO,0,0,"").sendToTarget();


    }
    /**
     * 方式1：新建Handler子类（内部类）
     */

    // 步骤1：自定义Handler子类（继承Handler类） & 复写handleMessage（）方法
    public class PaintHandler extends Handler {

        // 通过复写handlerMessage() 从而确定更新UI的操作
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case MESSAGE_FONT_SIZE:
                    int font_size = Integer.valueOf(msg.arg1);
                    Log.d(TAG, "font_size" + String.valueOf(font_size));
                    break;
                case MESSAGE_FONT_COLOR:
                    int font_color = Integer.valueOf(msg.arg1);
                    Log.d(TAG, "font_color" + String.valueOf(font_color));
                    break;
                case MESSAGE_PATH_START:
                    Log.d(TAG, "path_Start" + msg.obj);
                    break;
                case MESSAGE_PATH_MOVE:
                    Log.d(TAG, "path_Move" + msg.obj);
                    break;
                case MESSAGE_PATH_ENDS:
                    Log.d(TAG, "path_Ends" + msg.obj);
                    break;
                case MESSAGE_ACTS_UNDO:
                    Log.d(TAG, "acts_undo");
                    break;
                case MESSAGE_ACTS_CLEAR:
                    Log.d(TAG, "acts_clear");
                    break;

            }

        }
    }

    // 步骤1：自定义Handler子类（继承Handler类） & 复写handleMessage（）方法
    public class DrawHandler extends Handler {
        // 通过复写handlerMessage() 从而确定更新UI的操作
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case MESSAGE_FONT_SIZE:
                    int font_size = Integer.valueOf(msg.arg1);
                    Log.d(TAG, "font_size" + String.valueOf(font_size));
                    paintView.setStrokeWidth(font_size);
                    break;

                case MESSAGE_FONT_COLOR: {
                    int font_color = Integer.valueOf(msg.arg1);
                    paintView.setStrokeColor(font_color);
                }
                    break;

                case MESSAGE_PATH_START: {
                    Log.d(TAG, "path_Start" + msg.obj);
                    String pathStr = (String) msg.obj;
                    String[] Points = pathStr.split(",");
                    paintView.touchStart(Float.parseFloat(Points[0]), Float.parseFloat(Points[1]));
                }
                    break;
                case MESSAGE_PATH_MOVE:
                {
                    Log.d(TAG, "path_Move" + msg.obj);
                    String pathStr = (String) msg.obj;
                    String[] Points = pathStr.split(",");
                    paintView.touchMove(Float.parseFloat(Points[0]), Float.parseFloat(Points[1]));
                }
                    break;
                case MESSAGE_PATH_ENDS:
                    Log.d(TAG, "path_Ends" + msg.obj);
                    paintView.touchUp();
                    break;
                case MESSAGE_ACTS_UNDO:
                    Log.d(TAG, "acts_undo");
                    paintView.onBack(screen);
                    break;

                case MESSAGE_ACTS_CLEAR:
                    Log.d(TAG, "acts_clear");
                    paintView.clear();
                    break;

            }
        }

    }

/*

————————————————
版权声明：本文为CSDN博主「Carson_Ho」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/carson_ho/java/article/details/80305411

*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.normal:
                paintView.normal();
                return true;
            case R.id.emboss:
                paintView.emboss();
                return true;
            case R.id.blur:
                paintView.blur();
                return true;
            case R.id.clear:
                paintView.clear();
                mPaintHandler.obtainMessage(MESSAGE_ACTS_CLEAR,0,0,"").sendToTarget();
                return true;
            case R.id.save:

                screen.setDrawingCacheEnabled(true);
                bmScreen = screen.getDrawingCache();
                paintView.saveImage(bmScreen);

                screen.setDrawingCacheEnabled(false);
                bmScreen = null;
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void askPermissions() {

        String[] PERMISSION_S = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        };


        Set<String> permissionsRequest = new HashSet<>();
        for (String permission : PERMISSION_S) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsRequest.add(permission);
            }
        }

        if (!permissionsRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsRequest.toArray(new String[permissionsRequest.size()]),
                    REQ_PERMISSIONS);
        }
        System.out.println("permissionsRequest:" + permissionsRequest.size());
    }

    @SuppressLint("Override")
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_PERMISSIONS:
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                       // String text = getString( R.string.text_ShouldGrant) + " : "+ result ;
                       // Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                       // handler.postDelayed(GotoMenu, 4000);
                        return;
                    }
                }
                break;
        }
    }


    public void onBack(View view) {
        mPaintHandler.obtainMessage(MESSAGE_ACTS_UNDO,0,0,"").sendToTarget();
        paintView.onBack(screen);
    }

    public void onColorYellow (View view){
        mPaintHandler.obtainMessage(MESSAGE_FONT_COLOR,Color.YELLOW,0,"").sendToTarget();
        paintView.setStrokeColor(Color.YELLOW);
    }
    public void onColorRed (View view){
        mPaintHandler.obtainMessage(MESSAGE_FONT_COLOR,Color.RED,0,"").sendToTarget();
        paintView.setStrokeColor(Color.RED);
    }
    public void onColorBlue (View view){
        mPaintHandler.obtainMessage(MESSAGE_FONT_COLOR,Color.BLUE,0,"").sendToTarget();
        paintView.setStrokeColor(Color.BLUE);
    }
    public void onColorBlack (View view){
        mPaintHandler.obtainMessage(MESSAGE_FONT_COLOR,Color.BLACK,0,"").sendToTarget();
        paintView.setStrokeColor(Color.BLACK);
    }

}