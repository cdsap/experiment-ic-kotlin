package com.awesomeapp.checkout.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.checkout.Database5_3
import com.awesomeapp.checkout.Dao5_4

@Module
@InstallIn(SingletonComponent::class)
object Module_5 {
    @Provides
    @Singleton
    fun provideDatabase5_3(@ApplicationContext context: Context): Database5_3 {
        return Room.databaseBuilder(context, Database5_3::class.java, "checkout.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao5_4(db: Database5_3): Dao5_4 {
        return db.dao()
    }
}