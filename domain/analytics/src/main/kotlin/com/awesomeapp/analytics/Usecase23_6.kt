package com.awesomeapp.analytics

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase23_6 @Inject constructor(
    private val repository: Repository23_5
) {
    operator fun invoke(): Flow<List<Model23_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}