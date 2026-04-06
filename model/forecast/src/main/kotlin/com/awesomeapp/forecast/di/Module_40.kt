package com.awesomeapp.forecast.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.forecast.Database40_3
import com.awesomeapp.forecast.Dao40_4

@Module
@InstallIn(SingletonComponent::class)
object Module_40 {
    @Provides
    @Singleton
    fun provideDatabase40_3(@ApplicationContext context: Context): Database40_3 {
        return Room.databaseBuilder(context, Database40_3::class.java, "forecast.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao40_4(db: Database40_3): Dao40_4 {
        return db.dao()
    }
}