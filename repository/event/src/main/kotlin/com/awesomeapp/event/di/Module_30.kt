package com.awesomeapp.event.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.event.Database30_3
import com.awesomeapp.event.Dao30_4

@Module
@InstallIn(SingletonComponent::class)
object Module_30 {
    @Provides
    @Singleton
    fun provideDatabase30_3(@ApplicationContext context: Context): Database30_3 {
        return Room.databaseBuilder(context, Database30_3::class.java, "event.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao30_4(db: Database30_3): Dao30_4 {
        return db.dao()
    }
}