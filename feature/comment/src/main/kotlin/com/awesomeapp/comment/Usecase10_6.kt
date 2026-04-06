package com.awesomeapp.comment

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase10_6 @Inject constructor(
    private val repository: Repository10_5
) {
    operator fun invoke(): Flow<List<Model10_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}