package com.myprojects.drawapp.extension

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

fun Drawable.convertToBitmap(widthPixels: Int, heightPixels: Int): Bitmap{
    val mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mutableBitmap)
    this.setBounds(0, 0, widthPixels, heightPixels)
    this.draw(canvas)

    return mutableBitmap
}