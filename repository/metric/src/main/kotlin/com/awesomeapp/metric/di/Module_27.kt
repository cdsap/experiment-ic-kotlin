package com.awesomeapp.metric.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.metric.Database27_3
import com.awesomeapp.metric.Dao27_4

@Module
@InstallIn(SingletonComponent::class)
object Module_27 {
    @Provides
    @Singleton
    fun provideDatabase27_3(@ApplicationContext context: Context): Database27_3 {
        return Room.databaseBuilder(context, Database27_3::class.java, "metric.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao27_4(db: Database27_3): Dao27_4 {
        return db.dao()
    }
}