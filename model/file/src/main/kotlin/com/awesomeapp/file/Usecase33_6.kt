package com.awesomeapp.file

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase33_6 @Inject constructor(
    private val repository: Repository33_5
) {
    operator fun invoke(): Flow<List<Model33_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}