package com.awesomeapp.push.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.push.Database1_3
import com.awesomeapp.push.Dao1_4

@Module
@InstallIn(SingletonComponent::class)
object Module_1 {
    @Provides
    @Singleton
    fun provideDatabase1_3(@ApplicationContext context: Context): Database1_3 {
        return Room.databaseBuilder(context, Database1_3::class.java, "push.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao1_4(db: Database1_3): Dao1_4 {
        return db.dao()
    }
}