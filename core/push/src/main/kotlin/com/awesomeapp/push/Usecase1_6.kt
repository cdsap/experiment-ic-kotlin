package com.awesomeapp.push

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase1_6 @Inject constructor(
    private val repository: Repository1_5
) {
    operator fun invoke(): Flow<List<Model1_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}