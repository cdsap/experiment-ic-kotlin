package com.awesomeapp.account.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.account.Database21_3
import com.awesomeapp.account.Dao21_4

@Module
@InstallIn(SingletonComponent::class)
object Module_21 {
    @Provides
    @Singleton
    fun provideDatabase21_3(@ApplicationContext context: Context): Database21_3 {
        return Room.databaseBuilder(context, Database21_3::class.java, "account.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao21_4(db: Database21_3): Dao21_4 {
        return db.dao()
    }
}