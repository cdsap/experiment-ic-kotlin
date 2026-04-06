package com.awesomeapp.task

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase28_6 @Inject constructor(
    private val repository: Repository28_5
) {
    operator fun invoke(): Flow<List<Model28_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}