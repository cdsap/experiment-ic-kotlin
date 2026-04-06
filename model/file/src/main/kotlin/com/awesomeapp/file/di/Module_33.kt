package com.awesomeapp.file.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.file.Database33_3
import com.awesomeapp.file.Dao33_4

@Module
@InstallIn(SingletonComponent::class)
object Module_33 {
    @Provides
    @Singleton
    fun provideDatabase33_3(@ApplicationContext context: Context): Database33_3 {
        return Room.databaseBuilder(context, Database33_3::class.java, "file.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao33_4(db: Database33_3): Dao33_4 {
        return db.dao()
    }
}