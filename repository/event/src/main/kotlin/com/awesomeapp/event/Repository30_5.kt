package com.awesomeapp.event

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Repository30_5 @Inject constructor(
    private val dao: Dao30_4
) {
    fun observeItems(): Flow<List<Model30_2>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.upsertAll(
                listOf(
                    Entity30_1(id = 1, title = "Welcome", updatedAt = System.currentTimeMillis()),
                    Entity30_1(id = 2, title = "Getting started", updatedAt = System.currentTimeMillis())
                )
            )
        }
    }

    private fun Entity30_1.toModel(): Model30_2 {
        return Model30_2(id = id, title = title)
    }
}