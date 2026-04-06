package com.awesomeapp.map.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.map.Database38_3
import com.awesomeapp.map.Dao38_4

@Module
@InstallIn(SingletonComponent::class)
object Module_38 {
    @Provides
    @Singleton
    fun provideDatabase38_3(@ApplicationContext context: Context): Database38_3 {
        return Room.databaseBuilder(context, Database38_3::class.java, "map.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao38_4(db: Database38_3): Dao38_4 {
        return db.dao()
    }
}