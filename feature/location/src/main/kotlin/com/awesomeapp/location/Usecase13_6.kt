package com.awesomeapp.location

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase13_6 @Inject constructor(
    private val repository: Repository13_5
) {
    operator fun invoke(): Flow<List<Model13_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}