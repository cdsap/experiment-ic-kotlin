package com.awesomeapp.share

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase16_6 @Inject constructor(
    private val repository: Repository16_5
) {
    operator fun invoke(): Flow<List<Model16_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}