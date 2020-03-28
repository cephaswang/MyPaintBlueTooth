package com.example.mypaint;

// https://youtu.be/HW-NpG0uKm4

// https://www.ssaurel.com/blog/learn-to-create-a-paint-application-for-android/
// https://www.youtube.com/watch?v=uJGcmGXaQ0o
// Learn to create a Paint Application with Android Studio

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Stack;


public class PaintView extends View {

    // 调试信息
    private static final String TAG = "PaintView";

    public  static int BRUSH_SIZE = 20;
    public  int DEFAULT_COLOR    = Color.RED;
    public  static int DEFAULT_BG_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    private float  mX, mY;
    private Path   mPath;
    private Paint  mPaint;

    private Handler mHandler;

    // 單一條線
    private ArrayList<FingerPath> paths = new ArrayList<>();

   // public Stack< ArrayList<FingerPath> >  myLines = new Stack< ArrayList<FingerPath> >();

    public  int currentColor   = Color.RED;
    private int backgroundColor = Color.WHITE;
    public  int strokeWidth;
    private boolean emboss;
    private boolean blur;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    private boolean fromRemove = false;
    float MY_height = 0;
    float MY_width  = 0;
    // private static String filePath = Context.getExternalFilesDir(null).getAbsolutePath();

    public void setHandler(Handler handler){
        mHandler = handler;
    }

    public void setStrokeWidth(int brush_SIZE){
        BRUSH_SIZE  = brush_SIZE + 1;
        strokeWidth = BRUSH_SIZE;
    }

    public void setStrokeColor(int default_COLOR){
        DEFAULT_COLOR = default_COLOR;
        currentColor  = DEFAULT_COLOR;
    }


    public PaintView(Context context) {
        this(context, null);
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);

        mEmboss = new EmbossMaskFilter(new float[] {1, 1, 1}, 0.4f, 6, 3.5f);
        mBlur   = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);
    }



    public void init(DisplayMetrics metrics) {
        int height = metrics.heightPixels;
        int width  = metrics.widthPixels;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = DEFAULT_COLOR;
        strokeWidth  = BRUSH_SIZE;
    }

    // 1
    public void normal() {
        emboss = false;
        blur   = false;
    }

    // 2
    public void emboss() {
        emboss = true;
        blur   = false;
    }

    // 3
    public void blur() {
        emboss = false;
        blur   = true;
    }

    // 4
    public void clear() {
        backgroundColor = DEFAULT_BG_COLOR;
        paths.clear();

       // myLines = new Stack< ArrayList<FingerPath> >();

        normal();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        oneDraw(mCanvas);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }

    protected void oneDraw(Canvas canvas){

        for (FingerPath fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);

            if (fp.emboss)
                mPaint.setMaskFilter(mEmboss);
            else if (fp.blur)
                mPaint.setMaskFilter(mBlur);

            canvas.drawPath(fp.path, mPaint);
        }
    }


    public void onBack (View view) {
        if (paths.size()<1) return;

        paths.remove(paths.size() -1);

        normal();
        invalidate();
    }

    void SET_FromRemove(boolean isRemove) {
        fromRemove = isRemove;
    }

    // 起點
    void touchStart(float x, float y) {
        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, emboss, blur, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;

        if(! fromRemove){
            return;
        }

        String path = String.valueOf(mX/MY_width) + "," + String.valueOf(mY/MY_height);
        mHandler.obtainMessage(MainActivity.MESSAGE_PATH_START,0,0,path).sendToTarget();
    }

    // 移動
    void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        Log.d(TAG, "touchMove  x:" + x +" y:" + y);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;

            if(! fromRemove){
                return;
            }

            String path = String.valueOf(mX/MY_width) + "," + String.valueOf(mY/MY_height);
            mHandler.obtainMessage(MainActivity.MESSAGE_PATH_MOVE,0,0,path).sendToTarget();
        }
    }

    // 線段結束
    void touchUp() {
        mPath.lineTo(mX, mY);

        if(! fromRemove){
            return;
        }

        // String path = String.valueOf(mX) + "," + String.valueOf(mY);
        mHandler.obtainMessage(MainActivity.MESSAGE_PATH_ENDS,0,0,"").sendToTarget();
    }

    // 手繪事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // System.out.println("onTouchEvent: (" + event.getX() +"," + event.getY() + ")");
        Log.i("onTouchEvent","(" + event.getX() +"," + event.getY() + ")" );

        // 取得座標
        float x = event.getX();
        float y = event.getY();

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN : // 0
                touchStart(x, y);
                invalidate();
                break;

            case MotionEvent.ACTION_UP : // 1
                touchUp();
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE : // 2
                touchMove(x, y);
                invalidate();
                break;

        }

        return true;
    }

    // https://stackoverflow.com/questions/11360664/how-to-capture-screen-in-android-and-covert-it-to-image
    public void saveImage(Bitmap bmScreen2) {
        // TODO Auto-generated method stub

        File saved_image_file = new File(
                Environment.getExternalStorageDirectory()
                        + "/captured_Bitmap.png");

        System.out.print(Environment.getExternalStorageDirectory()
                + "/captured_Bitmap.png");

        if (saved_image_file.exists())
            saved_image_file.delete();
        try {
            FileOutputStream out = new FileOutputStream(saved_image_file);
            bmScreen2.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}