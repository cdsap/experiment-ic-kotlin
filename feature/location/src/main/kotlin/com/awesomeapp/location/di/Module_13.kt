package com.awesomeapp.location.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.location.Database13_3
import com.awesomeapp.location.Dao13_4

@Module
@InstallIn(SingletonComponent::class)
object Module_13 {
    @Provides
    @Singleton
    fun provideDatabase13_3(@ApplicationContext context: Context): Database13_3 {
        return Room.databaseBuilder(context, Database13_3::class.java, "location.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao13_4(db: Database13_3): Dao13_4 {
        return db.dao()
    }
}