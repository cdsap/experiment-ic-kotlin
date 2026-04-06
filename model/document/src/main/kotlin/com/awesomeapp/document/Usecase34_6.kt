package com.awesomeapp.document

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase34_6 @Inject constructor(
    private val repository: Repository34_5
) {
    operator fun invoke(): Flow<List<Model34_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}