package com.awesomeapp.network.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.network.Database14_3
import com.awesomeapp.network.Dao14_4

@Module
@InstallIn(SingletonComponent::class)
object Module_14 {
    @Provides
    @Singleton
    fun provideDatabase14_3(@ApplicationContext context: Context): Database14_3 {
        return Room.databaseBuilder(context, Database14_3::class.java, "network.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao14_4(db: Database14_3): Dao14_4 {
        return db.dao()
    }
}