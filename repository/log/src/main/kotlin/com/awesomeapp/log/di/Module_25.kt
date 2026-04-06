package com.awesomeapp.log.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.log.Database25_3
import com.awesomeapp.log.Dao25_4

@Module
@InstallIn(SingletonComponent::class)
object Module_25 {
    @Provides
    @Singleton
    fun provideDatabase25_3(@ApplicationContext context: Context): Database25_3 {
        return Room.databaseBuilder(context, Database25_3::class.java, "log.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao25_4(db: Database25_3): Dao25_4 {
        return db.dao()
    }
}