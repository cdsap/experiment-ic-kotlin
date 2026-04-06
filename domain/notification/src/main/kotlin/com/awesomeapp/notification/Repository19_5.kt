package com.awesomeapp.notification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Repository19_5 @Inject constructor(
    private val dao: Dao19_4
) {
    fun observeItems(): Flow<List<Model19_2>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.upsertAll(
                listOf(
                    Entity19_1(id = 1, title = "Welcome", updatedAt = System.currentTimeMillis()),
                    Entity19_1(id = 2, title = "Getting started", updatedAt = System.currentTimeMillis())
                )
            )
        }
    }

    private fun Entity19_1.toModel(): Model19_2 {
        return Model19_2(id = id, title = title)
    }
}