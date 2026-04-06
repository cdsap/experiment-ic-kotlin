package com.awesomeapp.todo.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.todo.Database36_3
import com.awesomeapp.todo.Dao36_4

@Module
@InstallIn(SingletonComponent::class)
object Module_36 {
    @Provides
    @Singleton
    fun provideDatabase36_3(@ApplicationContext context: Context): Database36_3 {
        return Room.databaseBuilder(context, Database36_3::class.java, "todo.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao36_4(db: Database36_3): Dao36_4 {
        return db.dao()
    }
}