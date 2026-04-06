package com.awesomeapp.metric

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase27_6 @Inject constructor(
    private val repository: Repository27_5
) {
    operator fun invoke(): Flow<List<Model27_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}