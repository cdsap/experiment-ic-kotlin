package com.awesomeapp.identity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Repository4_5 @Inject constructor(
    private val dao: Dao4_4
) {
    fun observeItems(): Flow<List<Model4_2>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.upsertAll(
                listOf(
                    Entity4_1(id = 1, title = "Welcome", updatedAt = System.currentTimeMillis()),
                    Entity4_1(id = 2, title = "Getting started", updatedAt = System.currentTimeMillis())
                )
            )
        }
    }

    private fun Entity4_1.toModel(): Model4_2 {
        return Model4_2(id = id, title = title)
    }
}