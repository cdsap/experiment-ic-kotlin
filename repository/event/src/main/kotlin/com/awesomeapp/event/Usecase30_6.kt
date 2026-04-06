package com.awesomeapp.event

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase30_6 @Inject constructor(
    private val repository: Repository30_5
) {
    operator fun invoke(): Flow<List<Model30_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}