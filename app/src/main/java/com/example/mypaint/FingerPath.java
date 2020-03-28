package com.example.mypaint;

// https://www.ssaurel.com/blog/learn-to-create-a-paint-application-for-android/
// https://www.youtube.com/watch?v=uJGcmGXaQ0o
// Learn to create a Paint Application with Android Studio


import android.graphics.Path;



public class FingerPath {

    public int color;
    public boolean emboss;
    public boolean blur;
    public int strokeWidth;
    public Path path;

    public FingerPath(int color, boolean emboss, boolean blur, int strokeWidth, Path path) {
        this.color = color;
        this.emboss = emboss;
        this.blur = blur;
        this.strokeWidth = strokeWidth;
        this.path = path;
    }
}