package com.awesomeapp.identity

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase4_6 @Inject constructor(
    private val repository: Repository4_5
) {
    operator fun invoke(): Flow<List<Model4_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}