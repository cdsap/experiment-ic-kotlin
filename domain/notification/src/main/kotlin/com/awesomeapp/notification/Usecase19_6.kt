package com.awesomeapp.notification

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase19_6 @Inject constructor(
    private val repository: Repository19_5
) {
    operator fun invoke(): Flow<List<Model19_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}