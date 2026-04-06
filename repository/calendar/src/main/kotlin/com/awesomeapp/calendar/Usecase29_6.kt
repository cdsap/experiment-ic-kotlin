package com.awesomeapp.calendar

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase29_6 @Inject constructor(
    private val repository: Repository29_5
) {
    operator fun invoke(): Flow<List<Model29_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}