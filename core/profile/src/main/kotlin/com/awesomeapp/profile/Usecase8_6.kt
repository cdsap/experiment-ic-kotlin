package com.awesomeapp.profile

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase8_6 @Inject constructor(
    private val repository: Repository8_5
) {
    operator fun invoke(): Flow<List<Model8_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}