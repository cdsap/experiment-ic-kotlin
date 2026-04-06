package com.awesomeapp.cart.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.awesomeapp.cart.Database6_3
import com.awesomeapp.cart.Dao6_4

@Module
@InstallIn(SingletonComponent::class)
object Module_6 {
    @Provides
    @Singleton
    fun provideDatabase6_3(@ApplicationContext context: Context): Database6_3 {
        return Room.databaseBuilder(context, Database6_3::class.java, "cart.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDao6_4(db: Database6_3): Dao6_4 {
        return db.dao()
    }
}