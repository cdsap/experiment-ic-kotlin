package com.awesomeapp.sync

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase15_6 @Inject constructor(
    private val repository: Repository15_5
) {
    operator fun invoke(): Flow<List<Model15_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}