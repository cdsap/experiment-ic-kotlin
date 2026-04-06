package com.awesomeapp.note

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase35_6 @Inject constructor(
    private val repository: Repository35_5
) {
    operator fun invoke(): Flow<List<Model35_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}