package com.awesomeapp.forecast

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase40_6 @Inject constructor(
    private val repository: Repository40_5
) {
    operator fun invoke(): Flow<List<Model40_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}