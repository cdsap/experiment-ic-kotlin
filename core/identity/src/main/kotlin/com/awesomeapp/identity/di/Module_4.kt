package com.awesomeapp.identity.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.identity.Database4_3
import com.awesomeapp.identity.Dao4_4

@Module
@InstallIn(SingletonComponent::class)
object Module_4 {
    @Provides
    @Singleton
    fun provideDatabase4_3(@ApplicationContext context: Context): Database4_3 {
        return Room.databaseBuilder(context, Database4_3::class.java, "identity.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao4_4(db: Database4_3): Dao4_4 {
        return db.dao()
    }
}