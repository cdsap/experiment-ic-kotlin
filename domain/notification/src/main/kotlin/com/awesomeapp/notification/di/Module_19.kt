package com.awesomeapp.notification.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.notification.Database19_3
import com.awesomeapp.notification.Dao19_4

@Module
@InstallIn(SingletonComponent::class)
object Module_19 {
    @Provides
    @Singleton
    fun provideDatabase19_3(@ApplicationContext context: Context): Database19_3 {
        return Room.databaseBuilder(context, Database19_3::class.java, "notification.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao19_4(db: Database19_3): Dao19_4 {
        return db.dao()
    }
}