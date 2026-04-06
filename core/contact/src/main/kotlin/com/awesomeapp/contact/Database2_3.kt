package com.awesomeapp.contact

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity2_1::class], version = 1, exportSchema = false)
abstract class Database2_3 : RoomDatabase() {
    abstract fun dao(): Dao2_4
}