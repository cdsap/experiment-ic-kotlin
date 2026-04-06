package com.awesomeapp.search

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase12_6 @Inject constructor(
    private val repository: Repository12_5
) {
    operator fun invoke(): Flow<List<Model12_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}