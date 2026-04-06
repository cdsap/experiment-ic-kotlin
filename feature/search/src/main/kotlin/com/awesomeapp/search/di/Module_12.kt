package com.awesomeapp.search.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.search.Database12_3
import com.awesomeapp.search.Dao12_4

@Module
@InstallIn(SingletonComponent::class)
object Module_12 {
    @Provides
    @Singleton
    fun provideDatabase12_3(@ApplicationContext context: Context): Database12_3 {
        return Room.databaseBuilder(context, Database12_3::class.java, "search.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao12_4(db: Database12_3): Dao12_4 {
        return db.dao()
    }
}