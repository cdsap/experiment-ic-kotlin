package com.awesomeapp.report.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.report.Database24_3
import com.awesomeapp.report.Dao24_4

@Module
@InstallIn(SingletonComponent::class)
object Module_24 {
    @Provides
    @Singleton
    fun provideDatabase24_3(@ApplicationContext context: Context): Database24_3 {
        return Room.databaseBuilder(context, Database24_3::class.java, "report.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao24_4(db: Database24_3): Dao24_4 {
        return db.dao()
    }
}