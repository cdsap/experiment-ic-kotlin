package com.awesomeapp.checkout

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity5_1::class], version = 1, exportSchema = false)
abstract class Database5_3 : RoomDatabase() {
    abstract fun dao(): Dao5_4
}