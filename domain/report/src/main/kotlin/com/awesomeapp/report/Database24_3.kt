package com.awesomeapp.report

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity24_1::class], version = 1, exportSchema = false)
abstract class Database24_3 : RoomDatabase() {
    abstract fun dao(): Dao24_4
}