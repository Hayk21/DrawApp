package com.myprojects.drawapp.model

import android.graphics.PointF

data class PictureTapData(
    val pixels: List<PointF>,
    val brushSize: Float,
    val color: Int
)
