package com.awesomeapp.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.app.Database41_3
import com.awesomeapp.app.Dao41_4

@Module
@InstallIn(SingletonComponent::class)
object Module_41 {
    @Provides
    @Singleton
    fun provideDatabase41_3(@ApplicationContext context: Context): Database41_3 {
        return Room.databaseBuilder(context, Database41_3::class.java, "app.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao41_4(db: Database41_3): Dao41_4 {
        return db.dao()
    }
}