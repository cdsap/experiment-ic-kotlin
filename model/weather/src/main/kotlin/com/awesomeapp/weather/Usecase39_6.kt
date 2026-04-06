package com.awesomeapp.weather

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase39_6 @Inject constructor(
    private val repository: Repository39_5
) {
    operator fun invoke(): Flow<List<Model39_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}