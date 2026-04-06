package com.awesomeapp.feed.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.feed.Database9_3
import com.awesomeapp.feed.Dao9_4

@Module
@InstallIn(SingletonComponent::class)
object Module_9 {
    @Provides
    @Singleton
    fun provideDatabase9_3(@ApplicationContext context: Context): Database9_3 {
        return Room.databaseBuilder(context, Database9_3::class.java, "feed.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao9_4(db: Database9_3): Dao9_4 {
        return db.dao()
    }
}