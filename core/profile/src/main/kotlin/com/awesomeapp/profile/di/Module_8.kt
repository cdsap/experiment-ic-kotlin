package com.awesomeapp.profile.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.profile.Database8_3
import com.awesomeapp.profile.Dao8_4

@Module
@InstallIn(SingletonComponent::class)
object Module_8 {
    @Provides
    @Singleton
    fun provideDatabase8_3(@ApplicationContext context: Context): Database8_3 {
        return Room.databaseBuilder(context, Database8_3::class.java, "profile.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao8_4(db: Database8_3): Dao8_4 {
        return db.dao()
    }
}