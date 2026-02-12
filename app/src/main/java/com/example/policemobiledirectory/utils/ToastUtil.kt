package com.example.policemobiledirectory.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * Utility to safely show Toasts with logging.
 * Helps debug issues where Toasts might not be visible or called from wrong threads.
 */
object ToastUtil {
    private const val TAG = "ToastUtil"

    fun showToast(context: Context, message: String, length: Int = Toast.LENGTH_SHORT) {
        // 1. Log the attempt
        Log.d(TAG, "Attempting to show toast: '$message'")

        // 2. Ensure Main Thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            show(context, message, length)
        } else {
            Log.w(TAG, "Toast called from background thread. Posting to Main Thread.")
            Handler(Looper.getMainLooper()).post {
                show(context, message, length)
            }
        }
    }

    private fun show(context: Context, message: String, length: Int) {
        try {
            // Use application context to avoid memory leaks if activity is destroyed,
            // though Activity context is better for themes. We'll use what's passed 
            // but wrap in try-catch for safety.
            Toast.makeText(context, message, length).show()
            Log.d(TAG, "Toast.show() called successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show toast", e)
        }
    }
}
