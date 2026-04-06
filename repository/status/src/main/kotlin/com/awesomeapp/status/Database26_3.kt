package com.awesomeapp.status

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity26_1::class], version = 1, exportSchema = false)
abstract class Database26_3 : RoomDatabase() {
    abstract fun dao(): Dao26_4
}