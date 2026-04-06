package com.awesomeapp.alarm

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase31_6 @Inject constructor(
    private val repository: Repository31_5
) {
    operator fun invoke(): Flow<List<Model31_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}