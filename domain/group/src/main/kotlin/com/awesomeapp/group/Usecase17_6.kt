package com.awesomeapp.group

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Usecase17_6 @Inject constructor(
    private val repository: Repository17_5
) {
    operator fun invoke(): Flow<List<Model17_2>> = repository.observeItems()

    suspend fun seedIfEmpty() = repository.seedIfEmpty()
}