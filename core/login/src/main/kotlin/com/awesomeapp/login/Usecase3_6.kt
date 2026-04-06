package com.awesomeapp.login

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase3_6 @Inject constructor(
    private val repository: Repository3_5
) {
    operator fun invoke(): Flow<List<Model3_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}