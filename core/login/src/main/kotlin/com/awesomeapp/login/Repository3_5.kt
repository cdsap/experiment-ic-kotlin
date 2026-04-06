package com.awesomeapp.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Repository3_5 @Inject constructor(
    private val dao: Dao3_4
) {
    fun observeItems(): Flow<List<Model3_2>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.upsertAll(
                listOf(
                    Entity3_1(id = 1, title = "Welcome", updatedAt = System.currentTimeMillis()),
                    Entity3_1(id = 2, title = "Getting started", updatedAt = System.currentTimeMillis())
                )
            )
        }
    }

    private fun Entity3_1.toModel(): Model3_2 {
        return Model3_2(id = id, title = title)
    }
}