package com.awesomeapp.profile

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity8_1::class], version = 1, exportSchema = false)
abstract class Database8_3 : RoomDatabase() {
    abstract fun dao(): Dao8_4
}