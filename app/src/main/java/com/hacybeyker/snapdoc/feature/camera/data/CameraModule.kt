package com.hacybeyker.snapdoc.feature.camera.data

import com.hacybeyker.snapdoc.feature.camera.domain.CameraPermissionRepository
import com.hacybeyker.snapdoc.feature.camera.domain.PhotoStorageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId

@Module
@InstallIn(SingletonComponent::class)
interface CameraModule {

    @Binds
    fun bindCameraPermissionRepository(repository: AndroidCameraPermissionRepository): CameraPermissionRepository

    @Binds
    fun bindPhotoStorageRepository(repository: InternalStoragePhotoRepository): PhotoStorageRepository

    companion object {

        @Provides
        fun provideZoneId(): ZoneId = ZoneId.systemDefault()
    }
}
