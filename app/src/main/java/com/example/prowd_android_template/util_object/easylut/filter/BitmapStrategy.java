package com.example.prowd_android_template.util_object.easylut.filter;


import android.graphics.Bitmap;
import com.example.prowd_android_template.util_object.easylut.lutimage.LUTImage;

public interface BitmapStrategy {
    Bitmap applyLut(Bitmap src, LUTImage lutImage);

    enum Type{
        APPLY_ON_ORIGINAL_BITMAP, CREATING_NEW_BITMAP
    }
}
