package com.example.playgroundproject

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_playground.*
import kotlinx.android.synthetic.main.playground_item_adapter.view.*
import kotlin.math.max
import kotlin.math.min


class PlaygroundActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val displayMetrics = DisplayMetrics()
        windowManager
            .defaultDisplay
            .getMetrics(displayMetrics)
        utils.screenWidth = displayMetrics.widthPixels
        utils.screenHeight = displayMetrics.heightPixels

        setContentView(R.layout.activity_playground)

        val adapter = ScrollingRecyclerAdapter(
            baseContext,
            List(6) {
                "this is $it"
            }
        )


        val snapHelper: LinearSnapHelper = object : LinearSnapHelper() {
            override fun findTargetSnapPosition(
                layoutManager: RecyclerView.LayoutManager,
                velocityX: Int,
                velocityY: Int
            ): Int {
                val centerView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
                val position = layoutManager.getPosition(centerView)
                var targetPosition = -1
                if (layoutManager.canScrollHorizontally()) {
                    targetPosition = if (velocityX < 0) {
                        position - 1
                    } else {
                        position + 1
                    }
                }
                if (layoutManager.canScrollVertically()) {
                    targetPosition = if (velocityY < 0) {
                        position - 1
                    } else {
                        position + 1
                    }
                }
                val firstItem = 0
                val lastItem = layoutManager.itemCount - 1
                targetPosition = min(lastItem, max(targetPosition, firstItem))
                return targetPosition
            }
        }

//        playgroundRecyclerview.layoutManager = LinearLayoutManager(baseContext, LinearLayoutManager.HORIZONTAL, false)
        snapHelper.attachToRecyclerView(playgroundRecyclerview)
        val centerZoomLayoutManager =
            CenterZoomLayoutManager(baseContext, LinearLayoutManager.HORIZONTAL, false)
        playgroundRecyclerview.layoutManager = centerZoomLayoutManager
        playgroundRecyclerview.adapter = adapter
    }
}

class ScrollingRecyclerAdapter(
    val context: Context,
    val listItem: List<String>
) : RecyclerView.Adapter<ScrollingRecyclerAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder =
        CustomViewHolder(
            view = LayoutInflater.from(context).inflate(
                R.layout.playground_item_adapter,
                parent,
                false
            )
        )

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(h: CustomViewHolder, position: Int) {
        h.textView.text = listItem[position]
    }


    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = itemView.playgroundText
    }

}