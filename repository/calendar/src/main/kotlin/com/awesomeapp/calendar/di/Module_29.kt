package com.awesomeapp.calendar.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.calendar.Database29_3
import com.awesomeapp.calendar.Dao29_4

@Module
@InstallIn(SingletonComponent::class)
object Module_29 {
    @Provides
    @Singleton
    fun provideDatabase29_3(@ApplicationContext context: Context): Database29_3 {
        return Room.databaseBuilder(context, Database29_3::class.java, "calendar.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao29_4(db: Database29_3): Dao29_4 {
        return db.dao()
    }
}