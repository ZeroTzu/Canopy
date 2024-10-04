package com.example.polaris.ui

import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class HapticTouchListener : View.OnTouchListener {
    override fun onTouch(view : View, event : MotionEvent) : Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->{
                val hapticSuccess = view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                Log.i("HapticTouchListener", "Action Down")
                Log.i("HapticTouchListener", hapticSuccess.toString())

            }
            MotionEvent.ACTION_UP ->{
                val hapticSuccess = view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                view.performClick()
                Log.i("HapticTouchListener", "Action Up")
                Log.i("HapticTouchListener", hapticSuccess.toString())

            }
        }
        return true
    }

}