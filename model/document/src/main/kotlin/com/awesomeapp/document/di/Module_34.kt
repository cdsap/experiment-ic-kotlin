package com.awesomeapp.document.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.document.Database34_3
import com.awesomeapp.document.Dao34_4

@Module
@InstallIn(SingletonComponent::class)
object Module_34 {
    @Provides
    @Singleton
    fun provideDatabase34_3(@ApplicationContext context: Context): Database34_3 {
        return Room.databaseBuilder(context, Database34_3::class.java, "document.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao34_4(db: Database34_3): Dao34_4 {
        return db.dao()
    }
}