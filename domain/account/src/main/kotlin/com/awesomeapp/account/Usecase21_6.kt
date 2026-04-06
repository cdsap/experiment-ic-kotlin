package com.awesomeapp.account

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase21_6 @Inject constructor(
    private val repository: Repository21_5
) {
    operator fun invoke(): Flow<List<Model21_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}