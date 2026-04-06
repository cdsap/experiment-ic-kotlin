package com.awesomeapp.list.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.list.Database37_3
import com.awesomeapp.list.Dao37_4

@Module
@InstallIn(SingletonComponent::class)
object Module_37 {
    @Provides
    @Singleton
    fun provideDatabase37_3(@ApplicationContext context: Context): Database37_3 {
        return Room.databaseBuilder(context, Database37_3::class.java, "list.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao37_4(db: Database37_3): Dao37_4 {
        return db.dao()
    }
}