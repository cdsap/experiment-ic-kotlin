package com.awesomeapp.app

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase41_6 @Inject constructor(
    private val repository: Repository41_5
) {
    operator fun invoke(): Flow<List<Model41_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}