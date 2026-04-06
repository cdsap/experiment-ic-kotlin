package com.awesomeapp.status.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.status.Database26_3
import com.awesomeapp.status.Dao26_4

@Module
@InstallIn(SingletonComponent::class)
object Module_26 {
    @Provides
    @Singleton
    fun provideDatabase26_3(@ApplicationContext context: Context): Database26_3 {
        return Room.databaseBuilder(context, Database26_3::class.java, "status.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao26_4(db: Database26_3): Dao26_4 {
        return db.dao()
    }
}