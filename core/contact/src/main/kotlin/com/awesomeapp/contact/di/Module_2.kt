package com.awesomeapp.contact.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.contact.Database2_3
import com.awesomeapp.contact.Dao2_4

@Module
@InstallIn(SingletonComponent::class)
object Module_2 {
    @Provides
    @Singleton
    fun provideDatabase2_3(@ApplicationContext context: Context): Database2_3 {
        return Room.databaseBuilder(context, Database2_3::class.java, "contact.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao2_4(db: Database2_3): Dao2_4 {
        return db.dao()
    }
}