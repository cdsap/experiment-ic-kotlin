package com.awesomeapp.message.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.message.Database18_3
import com.awesomeapp.message.Dao18_4

@Module
@InstallIn(SingletonComponent::class)
object Module_18 {
    @Provides
    @Singleton
    fun provideDatabase18_3(@ApplicationContext context: Context): Database18_3 {
        return Room.databaseBuilder(context, Database18_3::class.java, "message.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao18_4(db: Database18_3): Dao18_4 {
        return db.dao()
    }
}