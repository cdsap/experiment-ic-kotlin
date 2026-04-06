package com.awesomeapp.setting.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.setting.Database20_3
import com.awesomeapp.setting.Dao20_4

@Module
@InstallIn(SingletonComponent::class)
object Module_20 {
    @Provides
    @Singleton
    fun provideDatabase20_3(@ApplicationContext context: Context): Database20_3 {
        return Room.databaseBuilder(context, Database20_3::class.java, "setting.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao20_4(db: Database20_3): Dao20_4 {
        return db.dao()
    }
}