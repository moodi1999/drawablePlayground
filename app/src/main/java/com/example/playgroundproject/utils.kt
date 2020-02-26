package com.example.playgroundproject

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.View.LAYER_TYPE_SOFTWARE
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.utils.backgroundColorRes
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.roundedCornersDp
import com.mikepenz.iconics.utils.sizeDp

class DrawableBuilder(val context: Context) {

    fun create(): Drawable {
        return IconicsDrawable(context)
            .sizeDp(28)
            .shadow(
                IconicsSize.px(40),
                IconicsSize.px(5),
                IconicsSize.px(5),
                IconicsColor.colorRes(R.color.colorBlack)
            )
            .backgroundColorRes(R.color.colorWhite)
            .roundedCornersDp(5)
            .paddingDp(4)
    }

}

object ViewUtils {
    fun generateBackgroundWithShadow(
        context: Context,
        @ColorRes backgroundColor: Int,
        cornerRadius: Float,
        @ColorRes shadowColor: Int,
        elevation: Int,
        shadowGravity: Int
    ): Drawable {

        val cornerRadiusValue: Float = cornerRadius
        val elevationValue = elevation
        val shadowColorValue = ContextCompat.getColor(context, shadowColor)
        val backgroundColorValue = ContextCompat.getColor(context, backgroundColor)

        val outerRadius = FloatArray(8) {
            cornerRadiusValue
        }

        val backgroundPaint = Paint()
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.setShadowLayer(cornerRadiusValue, 0f, 0f, 0)

        val shapeDrawablePadding = Rect()
        shapeDrawablePadding.left = elevationValue
        shapeDrawablePadding.right = elevationValue
        val DY: Int
        when (shadowGravity) {
            Gravity.CENTER -> {
                shapeDrawablePadding.top = elevationValue
                shapeDrawablePadding.bottom = elevationValue
                DY = 0
            }
            Gravity.TOP -> {
                shapeDrawablePadding.top = elevationValue * 2
                shapeDrawablePadding.bottom = elevationValue
                DY = -1 * elevationValue / 3
            }
            else -> {
                shapeDrawablePadding.top = elevationValue
                shapeDrawablePadding.bottom = elevationValue * 2
                DY = elevationValue / 3
            }
        }

        val shapeDrawable = ShapeDrawable()
        shapeDrawable.setPadding(shapeDrawablePadding)
        shapeDrawable.paint.color = backgroundColorValue
        shapeDrawable.paint.setShadowLayer(
            cornerRadiusValue / 3,
            0f,
            DY.toFloat(),
            shadowColorValue
        )

//        val view: View? = null
//        view!!.setLayerType(LAYER_TYPE_SOFTWARE, shapeDrawable.paint)

        shapeDrawable.shape = RoundRectShape(outerRadius, null, null)
        val drawable = LayerDrawable(arrayOf<Drawable>(shapeDrawable))
        drawable.setLayerInset(
            0,
            elevationValue,
            elevationValue * 2,
            elevationValue,
            elevationValue * 2
        )
        return drawable
    }

    fun View.setShadow(
        @ColorRes shadowColor: Int,
        cornerRadius: Float,
        elevation: Int,
        shadowGravity: Int = Gravity.BOTTOM,
        @ColorRes backgroundColorResource: Int = 0
    ) {
        val resource = context.resources
        val firstLayer = 0
        val ratioTopBottom = 3
        val defaultRatio = 2

        if (background == null && backgroundColorResource == 0) {
            throw RuntimeException("Pass backgroundColorResource or use setBackground")
        }

        if (background != null && background !is ColorDrawable) {
            throw RuntimeException(
                "${background::class.java.name} " +
                        "is not supported, set background as " +
                        "ColorDrawable or pass background as a resource"
            )
        }

        val cornerRadiusValue = cornerRadius
        val elevationValue = elevation
        val shadowColorValue = ContextCompat.getColor(context, shadowColor)

        val backgroundColor = if (backgroundColorResource != 0) {
            ContextCompat.getColor(context, backgroundColorResource)
        } else {
            (background as ColorDrawable).color
        }

        val outerRadius = FloatArray(8) { cornerRadiusValue }

        val directionOfY = when (shadowGravity) {
            Gravity.CENTER -> 0
            Gravity.TOP -> -1 * elevationValue / ratioTopBottom
            Gravity.BOTTOM -> elevationValue / ratioTopBottom
            else -> elevationValue / defaultRatio // Gravity.LEFT & Gravity.RIGHT
        }

        val directionOfX = when (shadowGravity) {
            Gravity.LEFT -> -1 * elevationValue / ratioTopBottom
            Gravity.RIGHT -> elevationValue / ratioTopBottom
            else -> 0
        }

        val shapeDrawable = ShapeDrawable()
        shapeDrawable.paint.color = backgroundColor
        shapeDrawable.paint.setShadowLayer(
            cornerRadiusValue / ratioTopBottom,
            directionOfX.toFloat(),
            directionOfY.toFloat(),
            shadowColorValue
        )
        shapeDrawable.shape = RoundRectShape(outerRadius, null, null)

        when (Build.VERSION.SDK_INT) {
            in Build.VERSION_CODES.BASE..Build.VERSION_CODES.O_MR1 -> setLayerType(
                LAYER_TYPE_SOFTWARE,
                shapeDrawable.paint
            )
        }

        val drawable = LayerDrawable(arrayOf(shapeDrawable))
        drawable.setLayerInset(
            firstLayer,
            elevationValue,
            elevationValue * defaultRatio,
            elevationValue,
            elevationValue * defaultRatio
        )

        background = drawable
    }
}