package com.awesomeapp.setting

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase20_6 @Inject constructor(
    private val repository: Repository20_5
) {
    operator fun invoke(): Flow<List<Model20_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}