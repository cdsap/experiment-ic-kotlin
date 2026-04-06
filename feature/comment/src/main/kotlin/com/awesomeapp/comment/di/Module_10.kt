package com.awesomeapp.comment.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.comment.Database10_3
import com.awesomeapp.comment.Dao10_4

@Module
@InstallIn(SingletonComponent::class)
object Module_10 {
    @Provides
    @Singleton
    fun provideDatabase10_3(@ApplicationContext context: Context): Database10_3 {
        return Room.databaseBuilder(context, Database10_3::class.java, "comment.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao10_4(db: Database10_3): Dao10_4 {
        return db.dao()
    }
}