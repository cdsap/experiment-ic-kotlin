package com.awesomeapp.share.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.share.Database16_3
import com.awesomeapp.share.Dao16_4

@Module
@InstallIn(SingletonComponent::class)
object Module_16 {
    @Provides
    @Singleton
    fun provideDatabase16_3(@ApplicationContext context: Context): Database16_3 {
        return Room.databaseBuilder(context, Database16_3::class.java, "share.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao16_4(db: Database16_3): Dao16_4 {
        return db.dao()
    }
}