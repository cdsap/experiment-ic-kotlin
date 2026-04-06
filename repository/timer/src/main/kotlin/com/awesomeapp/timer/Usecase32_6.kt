package com.awesomeapp.timer

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase32_6 @Inject constructor(
    private val repository: Repository32_5
) {
    operator fun invoke(): Flow<List<Model32_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}