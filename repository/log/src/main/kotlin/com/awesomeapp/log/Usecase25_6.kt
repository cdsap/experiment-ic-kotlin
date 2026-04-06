package com.awesomeapp.log

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase25_6 @Inject constructor(
    private val repository: Repository25_5
) {
    operator fun invoke(): Flow<List<Model25_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}