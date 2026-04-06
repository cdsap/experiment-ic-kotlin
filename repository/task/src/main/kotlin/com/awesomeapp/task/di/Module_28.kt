package com.awesomeapp.task.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.task.Database28_3
import com.awesomeapp.task.Dao28_4

@Module
@InstallIn(SingletonComponent::class)
object Module_28 {
    @Provides
    @Singleton
    fun provideDatabase28_3(@ApplicationContext context: Context): Database28_3 {
        return Room.databaseBuilder(context, Database28_3::class.java, "task.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao28_4(db: Database28_3): Dao28_4 {
        return db.dao()
    }
}