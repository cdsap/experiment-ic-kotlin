package com.awesomeapp.weather.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.weather.Database39_3
import com.awesomeapp.weather.Dao39_4

@Module
@InstallIn(SingletonComponent::class)
object Module_39 {
    @Provides
    @Singleton
    fun provideDatabase39_3(@ApplicationContext context: Context): Database39_3 {
        return Room.databaseBuilder(context, Database39_3::class.java, "weather.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao39_4(db: Database39_3): Dao39_4 {
        return db.dao()
    }
}