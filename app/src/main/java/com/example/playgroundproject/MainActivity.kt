package com.example.playgroundproject

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.Shader.TileMode
import android.graphics.drawable.*
import android.graphics.drawable.ShapeDrawable.ShaderFactory
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.example.playgroundproject.MainActivity.ColorPropertyName.BackgroundColor
import com.example.playgroundproject.MainActivity.ColorPropertyName.TextColor
import com.example.playgroundproject.ViewUtils.generateBackgroundWithShadow
import com.example.playgroundproject.ViewUtils.setShadow
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val color = ContextCompat.getColor(this, R.color.colorAccent)
        val color1 = ContextCompat.getColor(this, android.R.color.holo_red_dark)
        val color2 = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        val colorBlack = ContextCompat.getColor(this, R.color.colorBlack)

        val accent = ColorDrawable(color)
        val primary = ColorDrawable(color1)
        val primaryDark = ColorDrawable(color2)

        testTextView.setShadow(
            R.color.colorBlack,
            5f,
            2
        )

        val drawable = generateBackgroundWithShadow(
            this,
            R.color.colorWhite,
            6f,
            R.color.colorBlack,
            5,
            Gravity.BOTTOM
        )

        val drawable2 = generateBackgroundWithShadow(
            this,
            R.color.colorPrimary,
            6f,
            R.color.colorPrimaryDark,
            3,
            Gravity.CENTER
        )


//        view2.setShadow(
//            R.color.colorBlack,
//            3f,
//            10
//        )
        val create = DrawableBuilder(this)
            .create()

        view2.background = create
        view1.background = drawable
        view3.background = drawable2
    }

    var status = true

    enum class ColorPropertyName(name: String) {
        TextColor("textColor"),
        BackgroundColor("backgroundColor"),
    }

    private fun View.getBackgroundColor(): ColorDrawable {
        val viewBg = this.background

        return if (viewBg is TransitionDrawable) {
            viewBg.getDrawable(viewBg.numberOfLayers - 1) as ColorDrawable
        } else {
            viewBg as ColorDrawable
        }
    }

    private fun View.setColorAnimate(
        @ColorInt to: Int,
        propertyName: String,
        @ColorInt from: Int? = null,
        duration: Long = 300,
        extra: (ObjectAnimator.() -> Unit)? = null,
        useArgbEvaluator: Boolean = false
    ) {

        val fromColor: Int = from ?: when (propertyName) {
            TextColor.name -> getBackgroundColor().color
            BackgroundColor.name -> getBackgroundColor().color
            else -> throw NullPointerException("field -from- cannot be null")
        }

        val colorAnim: ObjectAnimator = ObjectAnimator.ofInt(
            this, propertyName,
            fromColor, to
        )

        if (useArgbEvaluator) {
            colorAnim.setEvaluator(ArgbEvaluator())
        } else {
            colorAnim.setEvaluator(GammaEvaluator())
        }
        colorAnim.duration = duration
        extra?.invoke(colorAnim)

        colorAnim.start()
    }

    private fun View.animateCrossFade(
        to: Drawable,
        duration: Int = 400,
        onEnd: (() -> Unit)? = null
    ) {
        val viewBg = this.background
        val currentDrawable: Drawable

        currentDrawable = when (viewBg) {
            is TransitionDrawable -> {
                viewBg.getDrawable(viewBg.numberOfLayers - 1)
            }
            else -> viewBg
        }

        transitionDrawable = TransitionDrawable(arrayOf(currentDrawable, to))
        this.background = transitionDrawable
        transitionDrawable!!.isCrossFadeEnabled = true
        transitionDrawable!!.startTransition(duration)

        onEnd?.let {
            Handler().postDelayed(onEnd, duration.toLong() + 1)
        }
    }

    private lateinit var backgroundsDrawableArrayForTransition: Array<Drawable>
    private var transitionDrawable: TransitionDrawable? = null

    private fun backgroundAnimTransAction() {
        transitionDrawable = TransitionDrawable(backgroundsDrawableArrayForTransition)

        transitionDrawable!!.startTransition(4000)
        transitionDrawable!!.isCrossFadeEnabled = false // call public methods
    }

    fun animate(
        colorDrawable: ColorDrawable,
        @ColorInt colorFrom: Int,
        @ColorInt colorTo: Int
    ) {
        val valueAnimator = ValueAnimator()
        valueAnimator.addUpdateListener { animation ->
            colorDrawable.color =
                (animation.animatedValue as Int)
        }
        valueAnimator.setIntValues(colorFrom, colorTo)
        valueAnimator.setEvaluator(GammaEvaluator.getInstance())
        valueAnimator.start()
    }

    fun createCircleDrawable(color: Int, strokeWidth: Float): Drawable? {

        val color = ContextCompat.getColor(this, R.color.colorAccent)
        val color1 = ContextCompat.getColor(this, android.R.color.holo_red_dark)
        val color2 = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        val alpha = Color.alpha(color)
        val opaqueColor: Int = ContextCompat.getColor(this, R.color.colorAccent)
        val fillDrawable = ShapeDrawable(OvalShape())
        val paint: Paint = fillDrawable.paint
        paint.isAntiAlias = true
        paint.color = opaqueColor
        val elements = createInnerStrokesDrawable(opaqueColor, strokeWidth)
        val layers = arrayOf<Drawable>(
            fillDrawable,
            elements
        )
        val drawable = LayerDrawable(layers)

        val halfStrokeWidth = (strokeWidth / 2f).toInt()
        drawable.setLayerInset(
            1,
            halfStrokeWidth,
            halfStrokeWidth,
            halfStrokeWidth,
            halfStrokeWidth
        )
        return drawable
    }

    var mColorNormal = 0
    var mColorPressed = 0
    var mColorDisabled = 0
    var mTitle: String? = null

    @DrawableRes
    private val mIcon = 0
    private val mIconDrawable: Drawable? = null
    private val mSize = 0

    private val mCircleSize = 0f
    private var mShadowRadius = 0f
    private val mShadowOffset = 0f
    private val mDrawableSize = 0
    var mStrokeVisible = false

    fun createShadowShapeDrawable(
        context: Context,
        circleLoadingView: View,
        shadowColor: Int
    ): Drawable? {
        val color = ContextCompat.getColor(this, R.color.colorAccent)
        val color1 = ContextCompat.getColor(this, android.R.color.holo_red_dark)
        val color2 = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        val blackColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)


        val density: Float = context.resources.displayMetrics.density
        mShadowRadius = (density * 5f)
        val diameter = (10 * density * 2) as Int
        val shadowYOffset = (density * 5) as Int
        val shadowXOffset = (density * 5) as Int
        val oval: OvalShape = OvalShadow(mShadowRadius.toInt(), diameter)
        val circle = ShapeDrawable(oval)
        ViewCompat.setLayerType(
            circleLoadingView,
            View.LAYER_TYPE_SOFTWARE,
            circle.paint
        )
        circle.paint.setShadowLayer(
            mShadowRadius, shadowXOffset.toFloat(), shadowYOffset.toFloat(),
            blackColor
        )
        val padding: Int = mShadowRadius.toInt()
        // set padding so the inner image sits correctly within the shadow.
        circleLoadingView.setPadding(padding, padding, padding, padding)
        return circle
    }

    private fun createInnerStrokesDrawable(color: Int, strokeWidth: Float): Drawable {
        val color = ContextCompat.getColor(this, R.color.colorAccent)
        val color1 = ContextCompat.getColor(this, android.R.color.holo_red_dark)
        val color2 = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        if (!mStrokeVisible) {
            return ColorDrawable(Color.TRANSPARENT)
        }
        val shapeDrawable = ShapeDrawable(OvalShape())
        val bottomStrokeColor: Int = darkenColor(color)
        val bottomStrokeColorHalfTransparent: Int = halfTransparent(bottomStrokeColor)
        val topStrokeColor: Int = lightenColor(color)
        val topStrokeColorHalfTransparent: Int = halfTransparent(topStrokeColor)
        val paint = shapeDrawable.paint
        paint.isAntiAlias = true
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
        shapeDrawable.shaderFactory = object : ShaderFactory() {
            override fun resize(width: Int, height: Int): Shader {
                return LinearGradient(
                    width / 2f,
                    0f,
                    width / 2f,
                    height.toFloat(),
                    intArrayOf(
                        topStrokeColor,
                        topStrokeColorHalfTransparent,
                        color,
                        bottomStrokeColorHalfTransparent,
                        bottomStrokeColor
                    ),
                    floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f),
                    TileMode.CLAMP
                )
            }
        }
        return shapeDrawable
    }

    private fun halfTransparent(argb: Int): Int {
        return Color.argb(
            Color.alpha(argb) / 2,
            Color.red(argb),
            Color.green(argb),
            Color.blue(argb)
        )
    }

    private fun opaque(argb: Int): Int {
        return Color.rgb(
            Color.red(argb),
            Color.green(argb),
            Color.blue(argb)
        )
    }

    private fun opacityToAlpha(opacity: Float): Int {
        return (255f * opacity).toInt()
    }

    private fun darkenColor(argb: Int): Int {
        return adjustColorBrightness(argb, 0.9f)
    }

    private fun lightenColor(argb: Int): Int {
        return adjustColorBrightness(argb, 1.1f)
    }

    private fun adjustColorBrightness(argb: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb, hsv)
        hsv[2] = min(hsv[2] * factor, 1f)
        return Color.HSVToColor(Color.alpha(argb), hsv)
    }

    inner class OvalShadow internal constructor(shadowRadius: Int, circleDiameter: Int) :
        OvalShape() {
        private val mRadialGradient: RadialGradient
        private val mShadowPaint: Paint
        private val mCircleDiameter: Int
        override fun draw(canvas: Canvas, paint: Paint?) {
            val viewWidth = width
            val viewHeight = height
            canvas.drawCircle(
                viewWidth / 2, viewHeight / 2, mCircleDiameter / 2 + mShadowRadius,
                mShadowPaint
            )
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, mCircleDiameter / 2f, paint!!)
        }

        init {
            mShadowPaint = Paint()
            mShadowRadius = shadowRadius.toFloat()
            mCircleDiameter = circleDiameter
            mRadialGradient = RadialGradient(
                mCircleDiameter / 2f, mCircleDiameter / 2f,
                mShadowRadius, intArrayOf(
                    Color.BLACK, Color.TRANSPARENT
                ), null, TileMode.CLAMP
            )
            mShadowPaint.shader = mRadialGradient
        }
    }
}


object utils {
    var screenWidth = 0
    var screenHeight = 0
}

