package com.awesomeapp.sync.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.sync.Database15_3
import com.awesomeapp.sync.Dao15_4

@Module
@InstallIn(SingletonComponent::class)
object Module_15 {
    @Provides
    @Singleton
    fun provideDatabase15_3(@ApplicationContext context: Context): Database15_3 {
        return Room.databaseBuilder(context, Database15_3::class.java, "sync.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao15_4(db: Database15_3): Dao15_4 {
        return db.dao()
    }
}