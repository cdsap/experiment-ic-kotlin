package com.awesomeapp.note.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.note.Database35_3
import com.awesomeapp.note.Dao35_4

@Module
@InstallIn(SingletonComponent::class)
object Module_35 {
    @Provides
    @Singleton
    fun provideDatabase35_3(@ApplicationContext context: Context): Database35_3 {
        return Room.databaseBuilder(context, Database35_3::class.java, "note.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao35_4(db: Database35_3): Dao35_4 {
        return db.dao()
    }
}