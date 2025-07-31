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

abstract class SwipeToEditCallback(context: Context, private val listener: SwipeToEditCallbackListener) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

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
        fun onSwiped(position: Int)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        listener.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onSwiped(viewHolder.adapterPosition)
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

        if (dX < 0) { // Swiping left
            // Draw the red delete background
            background.color = Color.parseColor("#f44336")
            val deleteBackground = RectF(
                itemView.right.toFloat() + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            c.drawRect(deleteBackground, background)

            // Calculate position of delete icon
            val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
            val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
            val deleteIconRight = itemView.right - deleteIconMargin
            val deleteIconBottom = deleteIconTop + intrinsicHeight

            // Draw the delete icon
            deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
            deleteIcon.draw(c)
        } else { // Swiping right
            // Draw the green edit background
            background.color = Color.parseColor("#4CAF50")
            val editBackground = RectF(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.left.toFloat() + dX,
                itemView.bottom.toFloat()
            )
            c.drawRect(editBackground, background)

            // Calculate position of edit icon
            val editIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val editIconMargin = (itemHeight - intrinsicHeight) / 2
            val editIconLeft = itemView.left + editIconMargin
            val editIconRight = itemView.left + editIconMargin + intrinsicWidth
            val editIconBottom = editIconTop + intrinsicHeight

            // Draw the edit icon
            editIcon.setBounds(editIconLeft, editIconTop, editIconRight, editIconBottom)
            editIcon.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
