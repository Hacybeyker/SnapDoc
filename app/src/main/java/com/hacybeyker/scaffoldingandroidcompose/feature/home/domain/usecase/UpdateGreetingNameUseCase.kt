package com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.usecase

import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.GreetingRepository
import javax.inject.Inject

/**
 * Business rule of the sample slice: names are trimmed and a blank input keeps the
 * previous greeting untouched.
 */
class UpdateGreetingNameUseCase @Inject constructor(private val repository: GreetingRepository) {
    suspend operator fun invoke(name: String) {
        val sanitized = name.trim()
        if (sanitized.isNotEmpty()) {
            repository.updateName(sanitized)
        }
    }
}
