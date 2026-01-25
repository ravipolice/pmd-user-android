package com.example.policemobiledirectory.di

import android.content.Context
import com.example.policemobiledirectory.data.local.AppDatabase
import com.example.policemobiledirectory.data.local.EmployeeDao
import com.example.policemobiledirectory.data.local.PendingRegistrationDao
import com.example.policemobiledirectory.data.local.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideEmployeeDao(database: AppDatabase): EmployeeDao =
        database.employeeDao()

    @Provides
    @Singleton
    fun providePendingRegistrationDao(database: AppDatabase): PendingRegistrationDao =
        database.pendingRegistrationDao()
        
    @Provides
    @Singleton
    fun provideNotificationDao(database: AppDatabase): NotificationDao =
        database.notificationDao()
}
