package com.awesomeapp.report

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase24_6 @Inject constructor(
    private val repository: Repository24_5
) {
    operator fun invoke(): Flow<List<Model24_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}