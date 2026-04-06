package com.awesomeapp.setting

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity20_1::class], version = 1, exportSchema = false)
abstract class Database20_3 : RoomDatabase() {
    abstract fun dao(): Dao20_4
}