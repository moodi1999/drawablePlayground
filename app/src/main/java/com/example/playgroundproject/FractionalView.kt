package com.example.playgroundproject

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi

class SquareLayout : LinearLayout {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec((utils.screenWidth * 0.85).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((utils.screenHeight * 0.28).toInt() , MeasureSpec.EXACTLY)
        )
    }
}