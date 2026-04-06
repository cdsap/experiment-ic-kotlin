package com.awesomeapp.post.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.post.Database11_3
import com.awesomeapp.post.Dao11_4

@Module
@InstallIn(SingletonComponent::class)
object Module_11 {
    @Provides
    @Singleton
    fun provideDatabase11_3(@ApplicationContext context: Context): Database11_3 {
        return Room.databaseBuilder(context, Database11_3::class.java, "post.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao11_4(db: Database11_3): Dao11_4 {
        return db.dao()
    }
}