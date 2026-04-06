package com.awesomeapp.user

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase7_6 @Inject constructor(
    private val repository: Repository7_5
) {
    operator fun invoke(): Flow<List<Model7_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}