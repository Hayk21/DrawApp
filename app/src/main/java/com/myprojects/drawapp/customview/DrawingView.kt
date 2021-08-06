package com.myprojects.drawapp.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.myprojects.drawapp.R
import com.myprojects.drawapp.model.BrushSize
import com.myprojects.drawapp.model.PictureTapData
import kotlin.math.sqrt


class DrawingView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) :
    View(context, attributeSet, defStyleAttr) {

    constructor(context: Context, attributeSet: AttributeSet) :
            this(context, attributeSet, 0)

    constructor(context: Context) :
            this(context, null, 0)

    private var drawPaint: Paint = Paint()
    private var clearPaint: Paint = Paint()
    private var mainColor = resources.getColor(R.color.black, null)
    private var brushSize: Float = BrushSize.MIDDLE.size
    private var isErasing = false
    private val linePositions: MutableList<PointF> = mutableListOf()
    private val pictureHistory: MutableList<PictureTapData> = mutableListOf()
    private val pictureOldHistory: MutableList<PictureTapData> = mutableListOf()
    private var lastXTouch: Float? = null
    private var lastYTouch: Float? = null
    private val stopsGradient = floatArrayOf(0.1f, 0.1f, 1.0f)
    private var colorsGradient = intArrayOf(
        mainColor, mainColor,
        resources.getColor(R.color.transparent, null)
    )
    var backgroundImage: Bitmap? = null
    private var historyListener: DrawHistoryListener? = null
    private val transparentColor = resources.getColor(R.color.transparent, null)

    init {
        initPaint()
    }

    private fun initPaint() {
        drawPaint = Paint()
        drawPaint.color = mainColor
        drawPaint.isAntiAlias = true
        drawPaint.isDither = true
        clearPaint = Paint()
        clearPaint.isAntiAlias = true
        clearPaint.color = transparentColor
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCanvas(canvas)
        for (pos in linePositions) {
            if (!isErasing) {
                drawPaint.shader = RadialGradient(
                    pos.x, pos.y,
                    brushSize, colorsGradient,
                    stopsGradient, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(pos.x, pos.y, brushSize, drawPaint)
            } else {
                canvas.drawCircle(pos.x, pos.y, BrushSize.MIDDLE.size, clearPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastXTouch = touchX
                lastYTouch = touchY
                historyListener?.showHistoryItems(false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!(lastXTouch == touchX && lastYTouch == touchY)) {
                    calculatePosition(touchX, touchY)
                    lastXTouch = touchX
                    lastYTouch = touchY
                }
            }
            MotionEvent.ACTION_UP -> {
                lastXTouch = null
                lastYTouch = null
                if (linePositions.isNotEmpty()) {
                    pictureHistory.add(
                        PictureTapData(
                            linePositions.toList(), brushSize, if (isErasing)
                                transparentColor else mainColor
                        )
                    )
                    historyListener?.historyChanged(
                        pictureHistory.isNotEmpty(),
                        false
                    )
                    pictureOldHistory.clear()
                }
                historyListener?.showHistoryItems(pictureHistory.isNotEmpty() || pictureOldHistory.isNotEmpty())
                linePositions.clear()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    private fun square(num: Float): Float {
        return num * num
    }

    private fun calculatePosition(touchX: Float, touchY: Float) {
        val length = sqrt(
            square(lastXTouch!! - touchX) +
                    square(lastYTouch!! - touchY)
        )
        var count = (length / (brushSize / 3)).toInt()

        if (count < 1) {
            linePositions.add(PointF(touchX, touchY))
        } else {
            val numCount = count
            for (i in 1..numCount) {
                linePositions.add(
                    PointF(
                        ((lastXTouch!! + ((i.toFloat() / count.toFloat()) * touchX)))
                                / (1f + (i.toFloat() / count.toFloat())),
                        ((lastYTouch!! + ((i.toFloat() / count.toFloat()) * touchY)))
                                / (1f + (i.toFloat() / count.toFloat()))
                    )
                )
                count--
            }
        }
    }

    fun setColor(newColor: Int) {
        mainColor = newColor
        drawPaint.color = mainColor
        colorsGradient = intArrayOf(
            mainColor, mainColor,
            resources.getColor(R.color.transparent, null)
        )
    }

    fun setBrushSize(brushSize: BrushSize?) {
        brushSize?.let {
            this.brushSize = it.size
        }
    }

    private fun drawCanvas(canvas: Canvas) {
        for (data in pictureHistory) {
            for (pixel in data.pixels) {
                if (data.color == transparentColor) {
                    canvas.drawCircle(pixel.x, pixel.y, BrushSize.MIDDLE.size, clearPaint)
                } else {
                    drawPaint.color = data.color
                    drawPaint.shader = RadialGradient(
                        pixel.x, pixel.y,
                        data.brushSize,
                        intArrayOf(
                            data.color, data.color,
                            resources.getColor(R.color.transparent, null)
                        ),
                        stopsGradient, Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(pixel.x, pixel.y, data.brushSize, drawPaint)
                }
            }
        }
    }

    fun getImageBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (backgroundImage != null) {
            canvas.drawBitmap(
                backgroundImage!!,
                (width - backgroundImage!!.width) / 2f,
                (height - backgroundImage!!.height) / 2f, null
            )
        }
        drawCanvas(canvas)
        return bitmap
    }

    fun backHistory() {
        if (pictureHistory.isNotEmpty()) {
            pictureOldHistory.add(pictureHistory.last())
            pictureHistory.removeLast()
            invalidate()
            historyListener?.historyChanged(
                pictureHistory.isNotEmpty(),
                pictureOldHistory.isNotEmpty()
            )
        }
    }

    fun nextHistory() {
        if (pictureOldHistory.isNotEmpty()) {
            pictureHistory.add(pictureOldHistory.last())
            pictureOldHistory.removeLast()
            invalidate()
            historyListener?.historyChanged(
                pictureHistory.isNotEmpty(),
                pictureOldHistory.isNotEmpty()
            )
        }
    }

    interface DrawHistoryListener {
        fun historyChanged(backAvailable: Boolean, nextAvailable: Boolean)
        fun showHistoryItems(show: Boolean)
    }

    fun setOnHistoryListener(drawHistoryListener: DrawHistoryListener) {
        historyListener = drawHistoryListener
    }

    fun setErasingMode(isErasing: Boolean) {
        this.isErasing = isErasing
    }
}