package com.example.policemobiledirectory.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.AppDatabase
import com.example.policemobiledirectory.data.local.NotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val notificationDao = AppDatabase.getInstance(context).notificationDao()

    val notifications: StateFlow<List<NotificationEntity>> = notificationDao.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationDao.clearAllNotifications()
        }
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            notificationDao.markAsRead(id)
        }
    }
    
    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            notificationDao.deleteNotification(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationDao.markAllAsRead()
        }
    }
}
