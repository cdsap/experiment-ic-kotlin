package com.awesomeapp.user

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Repository7_5 @Inject constructor(
    private val dao: Dao7_4
) {
    fun observeItems(): Flow<List<Model7_2>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.upsertAll(
                listOf(
                    Entity7_1(id = 1, title = "Welcome", updatedAt = System.currentTimeMillis()),
                    Entity7_1(id = 2, title = "Getting started", updatedAt = System.currentTimeMillis())
                )
            )
        }
    }

    private fun Entity7_1.toModel(): Model7_2 {
        return Model7_2(id = id, title = title)
    }
}