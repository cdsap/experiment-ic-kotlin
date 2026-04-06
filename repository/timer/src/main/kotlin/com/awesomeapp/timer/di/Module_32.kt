package com.awesomeapp.timer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.timer.Database32_3
import com.awesomeapp.timer.Dao32_4

@Module
@InstallIn(SingletonComponent::class)
object Module_32 {
    @Provides
    @Singleton
    fun provideDatabase32_3(@ApplicationContext context: Context): Database32_3 {
        return Room.databaseBuilder(context, Database32_3::class.java, "timer.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao32_4(db: Database32_3): Dao32_4 {
        return db.dao()
    }
}