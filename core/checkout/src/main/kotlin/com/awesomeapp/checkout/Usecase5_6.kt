package com.awesomeapp.checkout

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase5_6 @Inject constructor(
    private val repository: Repository5_5
) {
    operator fun invoke(): Flow<List<Model5_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}