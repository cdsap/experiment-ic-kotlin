package com.awesomeapp.session.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.session.Database22_3
import com.awesomeapp.session.Dao22_4

@Module
@InstallIn(SingletonComponent::class)
object Module_22 {
    @Provides
    @Singleton
    fun provideDatabase22_3(@ApplicationContext context: Context): Database22_3 {
        return Room.databaseBuilder(context, Database22_3::class.java, "session.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao22_4(db: Database22_3): Dao22_4 {
        return db.dao()
    }
}