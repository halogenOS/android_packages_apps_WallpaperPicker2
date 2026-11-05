/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.picker.preview.ui.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewParent
import androidx.annotation.IdRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.ancestors
import androidx.core.view.children
import com.android.wallpaper.R

class ClickableMotionLayout(context: Context, attrs: AttributeSet?) : MotionLayout(context, attrs) {

    /** True for this view to intercept all motion events. */
    var shouldInterceptTouch = true

    // Needed for dragging feedback
    private var isDragging = false

    /** lambda to run after the completion of a motion layout transition */
    private var onTransitionCompleted: ((currentId: Int) -> Unit)? = null

    private var startX = 0f
    private var lastX = 0f

    private val TAG = "ClickableMotionLayout"
    private val DEBUG = false

    /** This variable is to track whether an edge transition is in progress */
    private var edgeTransitionInProgress = false

    fun setOnTransitionCompleted(listener: (currentId: Int) -> Unit) {
        onTransitionCompleted = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setupMotionLayoutListener()
    }

    private fun setupMotionLayoutListener() {
        setTransitionListener(
            object : MotionLayout.TransitionListener {
                override fun onTransitionStarted(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                ) {
                    if (DEBUG) {
                        Log.d(TAG, "onTransitionStarted - startId: $startId, endId: $endId")
                    }
                }

                override fun onTransitionChange(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                    progress: Float,
                ) {
                    if (DEBUG) {
                        Log.v(
                            TAG,
                            "onTransitionChange - startId: $startId, endId: $endId, progress: $progress",
                        )
                    }
                }

                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    if (DEBUG) {
                        Log.d(TAG, "onTransitionCompleted - currentId: $currentId")
                    }
                    when (currentId) {
                        R.id.leftEdgeActive,
                        R.id.rightEdgeActive -> {
                            if (DEBUG) {
                                Log.d(TAG, "Edge effect completed, returning to preview state")
                            }
                            // trigger a transition to the appropriate preview after the edge effect
                            // transition state has completed
                            postDelayed({ transitionToStart() }, 100)
                        }
                    }
                    onTransitionCompleted?.invoke(currentId)
                }

                override fun onTransitionTrigger(
                    motionLayout: MotionLayout?,
                    triggerId: Int,
                    positive: Boolean,
                    progress: Float,
                ) {
                    if (DEBUG) {
                        Log.d(
                            TAG,
                            "onTransitionTrigger - triggerId: $triggerId, positive: $positive, progress: $progress",
                        )
                    }
                }
            }
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        singleTapDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // resent tracking variables
                startX = event.x
                lastX = event.x
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - lastX
                lastX = event.x

                isDragging = true

                if (!edgeTransitionInProgress) {
                    // Check for edge overscroll attempts
                    findEdgeTransition(deltaX)?.let { edgeTransitionId ->
                        post {
                            setTransition(edgeTransitionId)
                            transitionToEnd()
                            edgeTransitionInProgress = true
                        }
                    }
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                edgeTransitionInProgress = false
            }
        }

        return true
    }

    /**
     * Edge effect transition of a swipe by [deltaX], when the motion scene starts it at the current
     * state. Swiping right starts the left edge effect, swiping left starts the right one.
     */
    @IdRes
    private fun findEdgeTransition(deltaX: Float): Int? {
        val edgeTransitionId =
            when {
                deltaX > 0 -> R.id.leftEdgeTransition
                deltaX < 0 -> R.id.rightEdgeTransition
                else -> return null
            }
        return edgeTransitionId.takeIf { getTransition(it)?.startConstraintSetId == currentState }
    }

    private val clickableViewIds = mutableSetOf<Int>()
    private val singleTapDetector =
        GestureDetector(
            context,
            object : SimpleOnGestureListener() {
                override fun onSingleTapUp(event: MotionEvent): Boolean {
                    // Check if any immediate child view is clicked
                    children
                        .find {
                            isEventPointerInRect(event, Rect(it.left, it.top, it.right, it.bottom))
                        }
                        ?.let { child ->
                            // Find all the clickable ids in the hierarchy of the clicked view and
                            // perform click on the exact view that should be clicked.
                            clickableViewIds
                                .mapNotNull { child.findViewById(it) }
                                .find { clickableView ->
                                    if (clickableView == child) {
                                        true
                                    } else {
                                        // Find ancestors of this clickable view up until this
                                        // layout and transform coordinates to align with motion
                                        // event.
                                        val ancestors = clickableView.ancestors
                                        var ancestorsLeft = 0
                                        var ancestorsTop = 0
                                        ancestors
                                            .filter {
                                                ancestors.indexOf(it) <=
                                                    ancestors.indexOf(child as ViewParent)
                                            }
                                            .forEach {
                                                it as ViewGroup
                                                ancestorsLeft += it.left
                                                ancestorsTop += it.top
                                            }
                                        isEventPointerInRect(
                                            event,
                                            Rect(
                                                /* left= */ ancestorsLeft + clickableView.left,
                                                /* top= */ ancestorsTop + clickableView.top,
                                                /* right= */ ancestorsLeft + clickableView.right,
                                                /* bottom= */ ancestorsTop + clickableView.bottom,
                                            ),
                                        )
                                    }
                                }
                                ?.performClick()
                        }

                    return true
                }
            },
        )

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // MotionEvent.ACTION_DOWN is the first MotionEvent received and is necessary to detect
        // various gesture, returns true to intercept all event so they are forwarded into
        // onTouchEvent.
        return shouldInterceptTouch
    }

    fun setClickableViewIds(ids: List<Int>) {
        clickableViewIds.apply {
            clear()
            addAll(ids)
        }
    }

    fun addClickableViewId(@IdRes id: Int) {
        clickableViewIds.add(id)
    }

    fun removeClickableViewId(@IdRes id: Int) {
        clickableViewIds.remove(id)
    }

    private fun isEventPointerInRect(e: MotionEvent, rect: Rect): Boolean {
        return e.x >= rect.left && e.x <= rect.right && e.y >= rect.top && e.y <= rect.bottom
    }
}
