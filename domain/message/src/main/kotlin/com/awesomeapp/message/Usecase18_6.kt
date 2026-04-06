package com.awesomeapp.message

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase18_6 @Inject constructor(
    private val repository: Repository18_5
) {
    operator fun invoke(): Flow<List<Model18_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}