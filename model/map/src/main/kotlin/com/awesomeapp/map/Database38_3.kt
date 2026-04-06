package com.awesomeapp.map

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity38_1::class], version = 1, exportSchema = false)
abstract class Database38_3 : RoomDatabase() {
    abstract fun dao(): Dao38_4
}