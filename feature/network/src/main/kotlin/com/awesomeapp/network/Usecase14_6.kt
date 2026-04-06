package com.awesomeapp.network

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase14_6 @Inject constructor(
    private val repository: Repository14_5
) {
    operator fun invoke(): Flow<List<Model14_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}