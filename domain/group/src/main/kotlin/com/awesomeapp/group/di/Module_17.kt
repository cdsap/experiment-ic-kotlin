package com.awesomeapp.group.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.group.Database17_3
import com.awesomeapp.group.Dao17_4

@Module
@InstallIn(SingletonComponent::class)
object Module_17 {
    @Provides
    @Singleton
    fun provideDatabase17_3(@ApplicationContext context: Context): Database17_3 {
        return Room.databaseBuilder(context, Database17_3::class.java, "group.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao17_4(db: Database17_3): Dao17_4 {
        return db.dao()
    }
}