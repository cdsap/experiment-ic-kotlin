package com.awesomeapp.map

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase38_6 @Inject constructor(
    private val repository: Repository38_5
) {
    operator fun invoke(): Flow<List<Model38_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}