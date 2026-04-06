package com.awesomeapp.push

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity1_1::class], version = 1, exportSchema = false)
abstract class Database1_3 : RoomDatabase() {
    abstract fun dao(): Dao1_4
}