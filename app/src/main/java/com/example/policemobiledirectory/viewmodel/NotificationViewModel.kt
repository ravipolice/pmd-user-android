package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.model.AppNotification
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.NotificationTarget
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel responsible for notification operations:
 * - Admin notifications
 * - User notifications
 * - Real-time notification listeners
 * - Sending notifications
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val userNotifications: StateFlow<List<AppNotification>> = _userNotifications.asStateFlow()

    private val _userNotificationsLastSeen = MutableStateFlow(0L)
    val userNotificationsLastSeen = _userNotificationsLastSeen.asStateFlow()

    private var userNotificationsListener: ListenerRegistration? = null
    private var userNotificationsListenerKgid: String? = null

    init {
        // Observe notification seen timestamps
        viewModelScope.launch {
            sessionManager.userNotificationsSeenAt.collect { lastSeen ->
                _userNotificationsLastSeen.value = lastSeen
            }
        }
    }

    /**
     * Update user notification listener based on current user
     */
    fun updateUserNotificationListener(user: Employee?) {
        val kgid = user?.kgid

        if (userNotificationsListenerKgid == kgid) return

        userNotificationsListener?.remove()
        userNotificationsListener = null
        userNotificationsListenerKgid = kgid

        if (user == null || kgid.isNullOrBlank()) {
            _userNotifications.value = emptyList()
            return
        }

        userNotificationsListener = firestore.collection("notifications_queue")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UserNotifications", "❌ Failed to fetch: ${e.message}")
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                val notifications = docs.mapNotNull { doc ->
                    val notification = doc.data?.toAppNotification(doc.id) ?: return@mapNotNull null
                    if (shouldDeliverNotification(notification, user)) notification else null
                }
                _userNotifications.value = notifications
            }
    }

    private fun shouldDeliverNotification(notification: AppNotification, user: Employee): Boolean {
        fun matches(lhs: String?, rhs: String?): Boolean =
            lhs != null && rhs != null && lhs.equals(rhs, ignoreCase = true)

        return when (notification.targetType) {
            NotificationTarget.ALL -> true
            NotificationTarget.SINGLE -> matches(notification.targetKgid, user.kgid)
            NotificationTarget.DISTRICT -> matches(notification.targetDistrict, user.district)
            NotificationTarget.STATION -> matches(notification.targetDistrict, user.district) &&
                    matches(notification.targetStation, user.station)
            NotificationTarget.ADMIN -> false // User app never sees admin notifications
        }
    }

    fun markNotificationsRead() {
        // Just mark current time as seen for user notifications
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            if (now > _userNotificationsLastSeen.value) {
                sessionManager.setUserNotificationsSeen(now)
            }
        }
    }

    private fun Map<String, Any>.toAppNotification(id: String): AppNotification? {
        val title = this["title"] as? String ?: "Notification"
        val body = this["body"] as? String ?: "You have a new message."
        val timestamp = (this["timestamp"] as? Number)?.toLong()
        val targetType = (this["targetType"] as? String)?.runCatching {
            NotificationTarget.valueOf(this.uppercase())
        }?.getOrNull() ?: NotificationTarget.ALL
        val targetKgid = this["targetKgid"] as? String
        val targetDistrict = this["targetDistrict"] as? String
        val targetStation = this["targetStation"] as? String

        return AppNotification(
            id = id,
            title = title,
            body = body,
            timestamp = timestamp,
            targetType = targetType,
            targetKgid = targetKgid,
            targetDistrict = targetDistrict,
            targetStation = targetStation
        )
    }

    override fun onCleared() {
        super.onCleared()
        userNotificationsListener?.remove()
        userNotificationsListener = null
    }
}



