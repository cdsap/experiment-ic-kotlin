package com.awesomeapp.alarm.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.alarm.Database31_3
import com.awesomeapp.alarm.Dao31_4

@Module
@InstallIn(SingletonComponent::class)
object Module_31 {
    @Provides
    @Singleton
    fun provideDatabase31_3(@ApplicationContext context: Context): Database31_3 {
        return Room.databaseBuilder(context, Database31_3::class.java, "alarm.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao31_4(db: Database31_3): Dao31_4 {
        return db.dao()
    }
}