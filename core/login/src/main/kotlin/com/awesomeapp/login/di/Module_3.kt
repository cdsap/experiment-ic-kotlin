package com.awesomeapp.login.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.login.Database3_3
import com.awesomeapp.login.Dao3_4

@Module
@InstallIn(SingletonComponent::class)
object Module_3 {
    @Provides
    @Singleton
    fun provideDatabase3_3(@ApplicationContext context: Context): Database3_3 {
        return Room.databaseBuilder(context, Database3_3::class.java, "login.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao3_4(db: Database3_3): Dao3_4 {
        return db.dao()
    }
}