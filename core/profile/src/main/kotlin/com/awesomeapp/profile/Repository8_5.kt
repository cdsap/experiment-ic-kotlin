package com.awesomeapp.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Repository8_5 @Inject constructor(
    private val dao: Dao8_4
) {
    fun observeItems(): Flow<List<Model8_2>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.upsertAll(
                listOf(
                    Entity8_1(id = 1, title = "Welcome", updatedAt = System.currentTimeMillis()),
                    Entity8_1(id = 2, title = "Getting started", updatedAt = System.currentTimeMillis())
                )
            )
        }
    }

    private fun Entity8_1.toModel(): Model8_2 {
        return Model8_2(id = id, title = title)
    }
}