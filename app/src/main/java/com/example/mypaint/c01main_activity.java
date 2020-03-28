package com.example.mypaint;


/*

Android Bluetooth 蓝牙基本操作
https://www.jianshu.com/p/cbf11be82f3a

Android6.0源码分析之蓝牙显示接收到的文件
https://cloud.tencent.com/developer/article/1028790

*/



import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bluetooth.BluetoothChatService;
import com.example.bluetooth.DeviceListAdapter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class c01main_activity extends AppCompatActivity implements View.OnTouchListener, AdapterView.OnItemClickListener {

    // 调试信息
    private static final String TAG = "c01main_activity";
    private static final boolean bDebug = true;

    // 本地蓝牙设备适配器
    private BluetoothAdapter mBluetoothAdapter = null;

    private Button button_on;
    private Button button_visible;
    private Button button_action_found;
    private Button button_admin;
    private boolean isMaster = false;

    private PaintView paintView;


    // Layout 视图
    private TextView mTitle;
    private ListView mConversationView;
    private static EditText mOutEditText;
    private Button mSendButton;

    /*
    private ArrayList<DetailEntity> conversationList = null;		// 对话列表
    private DetailEntity mMsgEntity = null;							// 消息实体类对象
    private DetailAdapter mConversationAdapter = null;				// 适配器对象
    */

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    public DeviceListAdapter mDeviceListAdapter;
    private  BluetoothDevice mBTDevice = null;

    ListView lvNewDevices;

    // 从对方发来的消息类型.
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // 从 BluetoothChatService接收到的设备名称
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "TOAST";

    private String imagePath = "NULL";								// 图像路径

    // Intent请求码
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int PICK_PICTURE = 3;
    // 选择图片的CODE

    // 已连接的设备名称
    private String mConnectedDeviceName = null;

    // BluetoothChatService的对象
    private static BluetoothChatService mChatService = null;

    // 输出信息的StringBuffer
    private StringBuffer mOutStringBuffer;

    // Constants that indicate the current connection state
    public static final int MESSAGE_FONT_SIZE  = 1;       // we're doing nothing
    public static final int MESSAGE_FONT_COLOR = 2;     // now listening for incoming connections
    public static final int MESSAGE_PATH_START = 3; // now initiating an outgoing connection
    public static final int MESSAGE_PATH_MOVE  = 4;  // now connected to a remote device
    public static final int MESSAGE_PATH_ENDS  = 5;  // now connected to a remote device
    public static final int MESSAGE_ACTS_UNDO  = 6;  // now connected to a remote device
    public static final int MESSAGE_ACTS_CLEAR = 7;  // now connected to a remote device

    View    screen;
    Button  buttonBack;

    private PaintHandler mPaintHandler;
    private DrawHandler  mDrawHandler;
    private SeekBar sb_normal;

    float MY_height = 0;
    float MY_width  = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p00main);

        screen = (View) findViewById(R.id.paintView);
        buttonBack = (Button) findViewById(R.id.buttonBack);
        button_admin = (Button) findViewById(R.id.button_admin);

        sb_normal   = (SeekBar) findViewById(R.id.seekBar);

        sb_normal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int my_progress;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // txt_cur.setText("当前进度值:" + progress + "  / 100 ");
                paintView.setStrokeWidth(progress + 1);
                buttonBack.setText( String.valueOf(progress + 1) );
                my_progress = progress + 1;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Toast.makeText(mContext, "触碰SeekBar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Toast.makeText(mContext, "放开SeekBar", Toast.LENGTH_SHORT).show();
                mPaintHandler.obtainMessage(MESSAGE_FONT_SIZE, my_progress ,0,"").sendToTarget();
                buttonBack.setText("Undo");
            }
        });



        paintView = (PaintView) findViewById(R.id.paintView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);





        mPaintHandler = new PaintHandler();
        mDrawHandler  = new DrawHandler();

        paintView.setHandler(mPaintHandler);
        paintView.SET_FromRemove(isMaster);


        button_on           = findViewById(R.id.button_on);
        button_visible      = findViewById(R.id.button_visible);
        button_action_found = findViewById(R.id.button_action_found);
     //   mConversationView   = findViewById(R.id.conversationXMLList);

        // 设置自定义的标题栏
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText("蓝牙");

        // 获取默认的蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 如果适配器为Null，则提示不可用
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        lvNewDevices.setOnItemClickListener(this);
        mBTDevices = new ArrayList<>();

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(B01_statusReceiver, filter1);

        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(B02_scan_mode, filter2);

        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(B03_action_scan, filter3);


        initScale();

    }



    @Override
    protected void onPause() {
        super.onPause();
        initScale();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initScale();
    }


    public void initScale(){
        paintView.post(new Runnable() {
            @Override
            public void run() {
                int height = paintView.getHeight();
                int width  = paintView.getWidth();
                Log.d(TAG, "metrics  getHeight:" + height +" getWidth:" + width);

                paintView.MY_height = paintView.getHeight();
                paintView.MY_width  = paintView.getWidth();

                MY_height = height;
                MY_width  = width;
            }
        });
    }


    // 步骤1：自定义Handler子类（继承Handler类） & 复写handleMessage（）方法
    public class PaintHandler extends Handler {

        // 通过复写handlerMessage() 从而确定更新UI的操作
        @Override
        public void handleMessage(Message msg) {

            String messageStr = "";

            switch (msg.what) {



                case MESSAGE_FONT_SIZE:
                    int font_size = Integer.valueOf(msg.arg1);
                    messageStr = String.valueOf(MESSAGE_FONT_SIZE) + "," + msg.arg1;

                    c01main_activity.sendMessage( messageStr );
                    Log.d(TAG, "font_size" + String.valueOf(font_size));
                    break;
                case MESSAGE_FONT_COLOR:
                    int font_color = Integer.valueOf(msg.arg1);
                    Log.d(TAG, "font_color" + String.valueOf(font_color));

                    messageStr = String.valueOf(MESSAGE_FONT_COLOR) + "," + msg.arg1;
                    c01main_activity.sendMessage( messageStr );
                    break;
                case MESSAGE_PATH_START:
                    Log.d(TAG, "path_Start" + msg.obj);

                    messageStr = String.valueOf(MESSAGE_PATH_START) + "," + msg.obj;
                    c01main_activity.sendMessage( messageStr );
                    break;
                case MESSAGE_PATH_MOVE:
                    Log.d(TAG, "path_Move" + msg.obj);

                    messageStr = String.valueOf(MESSAGE_PATH_MOVE) + "," + msg.obj;
                    c01main_activity.sendMessage( messageStr );
                    break;
                case MESSAGE_PATH_ENDS:
                    Log.d(TAG, "path_Ends" + msg.obj);

                    messageStr = String.valueOf(MESSAGE_PATH_ENDS) + "," + msg.obj;
                    c01main_activity.sendMessage( messageStr );
                    break;
                case MESSAGE_ACTS_UNDO:
                    Log.d(TAG, "acts_undo");

                    messageStr = String.valueOf(MESSAGE_ACTS_UNDO) + "," + msg.obj;
                    c01main_activity.sendMessage( messageStr );
                    break;
                case MESSAGE_ACTS_CLEAR:
                    Log.d(TAG, "acts_clear");

                    messageStr = String.valueOf(MESSAGE_ACTS_CLEAR) + "," + msg.obj;
                    c01main_activity.sendMessage( messageStr );
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
                    sb_normal.setProgress(font_size -1);
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
                    paintView.touchStart(Float.parseFloat(Points[0]) * MY_width , Float.parseFloat(Points[1]) * MY_height);
                    paintView.invalidate();
                    Log.d(TAG, "path_Start x:" + Float.parseFloat(Points[0]) * MY_width + "y:" + Float.parseFloat(Points[1]) * MY_height);
                }
                break;
                case MESSAGE_PATH_MOVE:
                {
                    Log.d(TAG, "path_Move" + msg.obj);
                    String pathStr = (String) msg.obj;
                    String[] Points = pathStr.split(",");
                    if ( Points.length == 2 ) {
                        paintView.touchMove(Float.parseFloat(Points[0]) * MY_width, Float.parseFloat(Points[1]) * MY_height);
                        Log.d(TAG, "path_Move x:" + Float.parseFloat(Points[0]) * MY_width + "y:" + Float.parseFloat(Points[1]) * MY_height);
                        paintView.invalidate();
                    }
                }
                break;
                case MESSAGE_PATH_ENDS:
                    Log.d(TAG, "path_Ends" + msg.obj);
                    paintView.touchUp();
                    paintView.invalidate();
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

    /**
     *
     * @Method: sendMessage
     * @Description: 通过蓝牙发送 文字消息
     * @param message
     * @return void  返回类型
     * @throws
     */
    private static void sendMessage(String message) {

        // 判断蓝牙状态,没有连接则退出
        if ( mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            //  Toast.makeText(this, "R.string.not_connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // 判断消息的长度
        if (message.length() > 0) {

            byte[] byteData = message.getBytes();	// 转换成字节流
            mChatService.write( byteData );			// 发送消息

            // 重置StringBuffer对象
           // mOutStringBuffer.setLength(0);
       //     mOutEditText.setText("");

            addConversationMsg("我说: ", message, 1);// 添加到聊天列表
        }

        // 初始化输出信息的StringBuffer为空
        // mOutStringBuffer = new StringBuffer("");
    }

    /**
     *  功能： 将消息添加到气泡列表
     *
     */
    private static void addConversationMsg(String name, String content, int layoutId)
    {
        // 在这里获取系统时间
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());		//获取当前时间
        String date = formatter.format(curDate);
        Log.d(TAG, "时间 : " + date);

        Log.d(TAG, "content : " + content);

        /*
        // 我自己发出去的消息
        if ( layoutId == 1)
        {
            // 添加对话
            mMsgEntity = new DetailEntity(name, date,content, R.layout.list_say_me_item);

        }
        else
        {
            mMsgEntity = new DetailEntity(name, date, content, R.layout.list_say_he_item);
        }

        conversationList.add( mMsgEntity );
        mConversationView.setAdapter(new DetailAdapter(this, conversationList));
        */

    }


    /**
     * 功能： activity Onstart
     *
     */
    @Override
    public void onStart() {
        super.onStart();
        if(bDebug) Log.e(TAG, "++ ON START ++");

        // 如果蓝牙没有启动,则设置蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else {
            if (mChatService == null)
                setupChat();
        }
    }

    /**
     * 功能： 初始化蓝牙设备
     *
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
       // mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        // mConversationView.setAdapter(mConversationAdapter);

        // 初始化消息编辑器,并且设置监听器
      //  mOutEditText = (EditText) findViewById(R.id.edit_text_out);
     //   mOutEditText.setOnEditorActionListener(mWriteListener);

       /*
        // 初始化发送按钮
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mSendButton : ");
                // 发送消息给对方
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });
*/
        // 初始化BluetoothChatService对象来执行蓝牙连接
        mChatService = new BluetoothChatService(this, mHandler);
    }

    /**
     * 功能 ：初始化聊天的气泡消息列表和适配器等
     *
     */
    private void initConversationList()
    {
        // 在这里获取系统时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());		//获取当前时间
        String date = formatter.format(curDate);
        Log.d(TAG, "时间 : " + date);

        /*
        conversationList  = new ArrayList<DetailEntity>();

        DetailEntity tips = new DetailEntity("提示信息","2010-04-26","已连接,可进行聊天.",
                R.layout.list_say_me_item);
        conversationList.add(tips);

        // 适配器
        mConversationAdapter = new DetailAdapter(this, conversationList);
        mConversationView = (ListView)findViewById(R.id.conversationXMLList); 	// 蓝牙聊天里面的布局
        mConversationView.setAdapter(mConversationAdapter);
        // 设置列表的触摸事件监听器,用于手势发送图片.
        mConversationView.setOnTouchListener(this);
        */

    }

    float downx = 0;		// 按下时的X坐标
    float downy = 0;		// 按下时的Y坐标
    float upx = 0;			// 抬起时的X坐标
    float upy = 0;			// 抬起时的Y坐标
    /**
     * 功能： 通过屏幕上方划动手指,将选中的相片分享给已经连接的蓝牙设备
     *  触摸事件(non-Javadoc)
     * @see View.OnTouchListener#onTouch(View, MotionEvent)
     *
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downx = event.getX();
                downy = event.getY();
                Log.v(TAG, "downy : " + downy);
                break;

            case MotionEvent.ACTION_UP:
                upx = event.getX();
                upy = event.getY();
                Log.v(TAG, "upy : " + upy);
                if(downy - upy > 250 && imagePath != "NULL")
                {
                    Log.v(TAG, "Touch and Send image." + (downy - upy));
                    // sendPictureByBluetooth();							// 向对方分享照片
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return true;
    }


    /**
     *  功能: 消息编辑器的监听,可按回车发送.
     *
     */
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // 按回车时发送消息
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message);			// 发送消息
                    }
                    if(bDebug) Log.i(TAG, "END onEditorAction");
                    return true;
                }
            };


    /**
     * 功能：页面销毁,停止蓝牙服务线程
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null)
            mChatService.stop();
        if(bDebug)
            Log.e(TAG, "--- ON DESTROY ---");
    }

    /**
     * 功能： onResume
     *
     */
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(bDebug) Log.e(TAG, "+ ON RESUME +");

        // 如果BluetoothChatService对象不为空,则获取BluetoothChatService对象的状态
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {

                // 启动BluetoothChatService对象服务
                mChatService.start();
            }
        }
    }


    // https://mcl-123.github.io/2019/03/16/%E8%93%9D%E7%89%99%E5%9F%BA%E6%9C%AC%E5%8A%9F%E8%83%BD%E5%AE%9E%E7%8E%B0/
    // 蓝牙状态广播接收者
    private final BroadcastReceiver B01_statusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int status = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch (status) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "蓝牙已关闭");
                        button_on.setBackgroundColor(Color.GRAY);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.e(TAG, "蓝牙已打开");
                        button_on.setBackgroundColor(Color.GREEN);
                        // startDiscovery();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e(TAG, "蓝牙关闭中...");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "蓝牙打开中...");
                        break;
                    default:
                        break;
                }
            }
        }
    };

    // 以可以调用cancelDiscovery()方法来停止搜索
    // 蓝牙状态广播接收者
    private final BroadcastReceiver B02_scan_mode = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Log.e(TAG, "设备可见监听");
                    button_visible.setBackgroundColor(Color.GREEN);
                } else {
                    Log.e(TAG, "设备不可见监听");
                    button_visible.setBackgroundColor(Color.GRAY);
                }
            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver B03_action_scan = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            // mBTDevices = new ArrayList<>();

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);

                lvNewDevices.setVisibility(View.VISIBLE);

                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    public void btn_On_Bluetooth(View view) {
        // 如果蓝牙没有启动,则设置蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

        } else {
            mBluetoothAdapter.disable();
        }
    }

    public void btn_On_Visibility(View view) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void btn_On_scan(View view) {

        lvNewDevices.setVisibility(View.INVISIBLE);

        mBTDevices = new ArrayList<>();

        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
        }

        //check BT permissions in manifest
        checkBTPermissions();

        mBluetoothAdapter.startDiscovery();
    }

    /**
     *  功能： 利用多线程进行蓝牙操作，从BluetoothChatService中返回蓝牙信息
     *
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_STATE_CHANGE:								// 蓝牙状态改变
                    if(bDebug)
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);

                    switch (msg.arg1) {

                        case BluetoothChatService.STATE_CONNECTED:		// 蓝牙已经连接
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);

                            Log.d(TAG, "蓝牙已经连接");
                            // mConversationAdapter.clear();				// 先清空原来的消息列表
                            initConversationList();						// 蓝牙连接后初始化信息列表等

                            break;

                        case BluetoothChatService.STATE_CONNECTING:		// 蓝牙正在连接
                            mTitle.setText(R.string.title_connecting);
                            break;

                        case BluetoothChatService.STATE_LISTEN:			// 蓝牙正在监听
                        case BluetoothChatService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;

                case MESSAGE_WRITE:										// 蓝牙的写操作,即传送字节流,发送消息
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String (writeBuf );

                    break;

                case MESSAGE_READ:										// 蓝牙的读操作,即接收信息
                    byte[] readBuf = (byte[]) msg.obj;					// 从Handler提交来的消息中读取消息.转换成字节流


                    // 从buffer里构造一个字符串
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    addConversationMsg(mConnectedDeviceName + " 说 : ", readMessage, 2);

                    String[] readSTR = readMessage.split(",");
                    int sendCode = Integer.valueOf(readSTR[0]);

                    Log.d(TAG, " 说 : " + sendCode );

                    int arg1 = 0;
                    if ( readSTR.length == 2 ){
                        arg1 = Integer.valueOf(readSTR[1]);
                    }




                    switch (sendCode){
                        case MESSAGE_FONT_SIZE:
                            mDrawHandler.obtainMessage( MESSAGE_FONT_SIZE,arg1,0,"").sendToTarget();
                            break;
                        case MESSAGE_FONT_COLOR:
                            mDrawHandler.obtainMessage( MESSAGE_FONT_COLOR,arg1,0,"").sendToTarget();
                            break;
                        case MESSAGE_PATH_START: {
                            if ( readSTR.length > 3 ) {
                                break;
                            }
                            String path = readSTR[1] + "," + readSTR[2];
                            mDrawHandler.obtainMessage(MainActivity.MESSAGE_PATH_START, 0, 0, path).sendToTarget();
                        }
                            break;
                        case MESSAGE_PATH_MOVE: {
                            if ( readSTR.length > 3 ) {
                                break;
                            }
                            String path = readSTR[1] + "," + readSTR[2];
                            mDrawHandler.obtainMessage(MainActivity.MESSAGE_PATH_MOVE, 0, 0, path).sendToTarget();
                        }
                            break;
                        case MESSAGE_PATH_ENDS:
                            mDrawHandler.obtainMessage(MainActivity.MESSAGE_PATH_ENDS,0,0,"").sendToTarget();
                            break;
                        case MESSAGE_ACTS_UNDO:
                            mDrawHandler.obtainMessage(MESSAGE_ACTS_UNDO,0,0,"").sendToTarget();
                            break;
                        case MESSAGE_ACTS_CLEAR:
                            mDrawHandler.obtainMessage(MESSAGE_ACTS_CLEAR,0,0,"").sendToTarget();
                            break;
                        default:
                            break;
                    }

                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;

                case MESSAGE_DEVICE_NAME:

                    // 保存已经连接的设备名
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "已连接到 "
                            + mConnectedDeviceName + "!", Toast.LENGTH_SHORT).show();

                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){

            int permissionCheck  = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck     += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (permissionCheck != 0) {
                this.requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }, 1001); //Any number
            }

        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName    = mBTDevices.get(position).getName();
        String deviceAddress = mBTDevices.get(position).getAddress();


        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        // 只有選取，沒有連線。
        // create the bond.
        // NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(position).createBond();
        }


        mBTDevice = mBTDevices.get(position);

        // 選定要連線的裝置，接續，
        // 02 啟動連線服務
        mChatService = new BluetoothChatService(this , mHandler);

        // 获取远程蓝牙设备的地址
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        // 连接远程蓝牙设备
        mChatService.connect(device , true);

    }

    public void btn_Master(View view) {

        isMaster = !isMaster;

        if (! isMaster) {
            paintView.SET_FromRemove(isMaster);
            button_admin.setBackgroundColor(Color.GRAY);
            button_admin.setText("RECEVICE");
        } else {
            paintView.SET_FromRemove(isMaster);
            button_admin.setBackgroundColor(Color.GREEN);
            button_admin.setText(" MASTER ");

            mPaintHandler.obtainMessage(MESSAGE_FONT_COLOR, paintView.currentColor,0,"").sendToTarget();

            mPaintHandler.obtainMessage(MESSAGE_FONT_SIZE, paintView.strokeWidth ,0,"").sendToTarget();
        }

    }

    public void btn_clear(View view) {
        mPaintHandler.obtainMessage(MESSAGE_ACTS_CLEAR,0,0,"").sendToTarget();
        paintView.clear();
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
