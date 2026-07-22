package com.hacybeyker.scaffoldingandroidcompose.feature.home.data

import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.GreetingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDataModule {

    @Binds
    abstract fun bindGreetingRepository(impl: InMemoryGreetingRepository): GreetingRepository
}
