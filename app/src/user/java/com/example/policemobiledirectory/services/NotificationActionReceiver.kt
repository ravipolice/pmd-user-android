package com.example.policemobiledirectory.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * Handles notification action buttons (Approve / Reject) for pending registrations.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val kgid = intent.getStringExtra("KGID") ?: return

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val pendingRef = db.collection("pending_registrations").document(kgid)
                val snapshot = pendingRef.get().await()

                if (!snapshot.exists()) {
                    Log.w("NotifActionReceiver", "Pending registration $kgid not found")
                    return@launch
                }

                val data = snapshot.data ?: emptyMap<String, Any>()

                when (action) {
                    "APPROVE", "REJECT" -> {
                        // These actions are not supported in the User app
                        Log.w("NotifActionReceiver", "Action $action is not supported in the User app.")
                    }

                    else -> Log.w("NotifActionReceiver", "Unknown action $action")
                }
            } catch (e: Exception) {
                Log.e("NotifActionReceiver", "Error handling action for KGID=$kgid", e)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
