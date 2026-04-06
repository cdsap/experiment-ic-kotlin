package com.awesomeapp.metric

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity27_1::class], version = 1, exportSchema = false)
abstract class Database27_3 : RoomDatabase() {
    abstract fun dao(): Dao27_4
}