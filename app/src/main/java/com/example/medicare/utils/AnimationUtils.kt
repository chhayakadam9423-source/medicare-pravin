package com.example.medicare.utils

import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object AnimationUtils {

    /**
     * Applies interactive press scale (1.0 -> 0.94 -> 1.0) with subtle elevation to any View
     */
    fun applyPressAnimation(view: View, onClick: (() -> Unit)? = null) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .setDuration(120)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                    false
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            onClick?.invoke()
                        }
                        .start()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start()
                    false
                }
                else -> false
            }
        }
    }

    /**
     * Smoothly fades in a view
     */
    fun fadeIn(view: View, duration: Long = 300) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}
