package com.awesomeapp.analytics.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.analytics.Database23_3
import com.awesomeapp.analytics.Dao23_4

@Module
@InstallIn(SingletonComponent::class)
object Module_23 {
    @Provides
    @Singleton
    fun provideDatabase23_3(@ApplicationContext context: Context): Database23_3 {
        return Room.databaseBuilder(context, Database23_3::class.java, "analytics.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao23_4(db: Database23_3): Dao23_4 {
        return db.dao()
    }
}