package com.awesomeapp.user.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.user.Database7_3
import com.awesomeapp.user.Dao7_4

@Module
@InstallIn(SingletonComponent::class)
object Module_7 {
    @Provides
    @Singleton
    fun provideDatabase7_3(@ApplicationContext context: Context): Database7_3 {
        return Room.databaseBuilder(context, Database7_3::class.java, "user.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao7_4(db: Database7_3): Dao7_4 {
        return db.dao()
    }
}