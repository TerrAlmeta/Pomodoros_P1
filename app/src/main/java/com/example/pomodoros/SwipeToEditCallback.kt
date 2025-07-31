package com.example.pomodoros

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

abstract class SwipeToEditCallback(context: Context, private val listener: SwipeToEditCallbackListener) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {

    private val editIcon: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_edit)!!
    private val deleteIcon: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!
    private val intrinsicWidth = editIcon.intrinsicWidth
    private val intrinsicHeight = editIcon.intrinsicHeight
    private val background = Paint()
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // This is where we will handle the click events
            return true
        }
    })

    interface SwipeToEditCallbackListener {
        fun onEditClicked(position: Int)
        fun onDeleteClicked(position: Int)
        fun onItemMove(fromPosition: Int, toPosition: Int)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        listener.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false)
            return
        }

        // Draw the red delete background
        background.color = Color.parseColor("#f44336")
        val deleteBackground = RectF(
            itemView.right.toFloat() + dX,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )
        c.drawRect(deleteBackground, background)

        // Draw the green edit background
        background.color = Color.parseColor("#4CAF50")
        val editBackground = RectF(
            itemView.right.toFloat() + dX - 2 * intrinsicWidth,
            itemView.top.toFloat(),
            itemView.right.toFloat() + dX,
            itemView.bottom.toFloat()
        )
        c.drawRect(editBackground, background)


        // Calculate position of delete icon
        val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
        val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
        val deleteIconRight = itemView.right - deleteIconMargin
        val deleteIconBottom = deleteIconTop + intrinsicHeight

        // Draw the delete icon
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteIcon.draw(c)

        // Calculate position of edit icon
        val editIconLeft = itemView.right - 2 * deleteIconMargin - 2 * intrinsicWidth
        val editIconRight = itemView.right - 2 * deleteIconMargin - intrinsicWidth

        // Draw the edit icon
        editIcon.setBounds(editIconLeft, deleteIconTop, editIconRight, deleteIconBottom)
        editIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder?.itemView
        if (itemView != null) {
            val isCanceled = dX == 0f && !isCurrentlyActive
            if (!isCanceled) {
                recyclerView.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    if (event.action == MotionEvent.ACTION_UP) {
                        if (dX < -2 * intrinsicWidth) {
                            listener.onDeleteClicked(viewHolder.adapterPosition)
                        } else if (dX < -intrinsicWidth) {
                            listener.onEditClicked(viewHolder.adapterPosition)
                        }
                    }
                    false
                }
            }
        }
        super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
