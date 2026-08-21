package com.hacybeyker.snapdoc.feature.camera.data

import com.hacybeyker.snapdoc.feature.camera.domain.CameraPermissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CameraPermissionModule {

    @Binds
    fun bindCameraPermissionRepository(repository: AndroidCameraPermissionRepository): CameraPermissionRepository
}
