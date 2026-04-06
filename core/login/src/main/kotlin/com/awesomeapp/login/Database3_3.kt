package com.awesomeapp.login

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity3_1::class], version = 1, exportSchema = false)
abstract class Database3_3 : RoomDatabase() {
    abstract fun dao(): Dao3_4
}