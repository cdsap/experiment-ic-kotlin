package com.awesomeapp.cart

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Entity6_1::class], version = 1, exportSchema = false)
abstract class Database6_3 : RoomDatabase() {
    abstract fun dao(): Dao6_4
}