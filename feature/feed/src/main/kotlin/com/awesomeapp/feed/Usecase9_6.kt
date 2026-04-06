package com.awesomeapp.feed

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase9_6 @Inject constructor(
    private val repository: Repository9_5
) {
    operator fun invoke(): Flow<List<Model9_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}