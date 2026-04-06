package com.awesomeapp.list

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase37_6 @Inject constructor(
    private val repository: Repository37_5
) {
    operator fun invoke(): Flow<List<Model37_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}