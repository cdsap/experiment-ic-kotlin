package com.awesomeapp.status

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase26_6 @Inject constructor(
    private val repository: Repository26_5
) {
    operator fun invoke(): Flow<List<Model26_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}