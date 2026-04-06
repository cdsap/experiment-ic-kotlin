package com.awesomeapp.session

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase22_6 @Inject constructor(
    private val repository: Repository22_5
) {
    operator fun invoke(): Flow<List<Model22_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}