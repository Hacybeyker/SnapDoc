package com.hacybeyker.snapdoc.feature.camera.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.hacybeyker.snapdoc.feature.camera.domain.CameraPermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidCameraPermissionRepository @Inject constructor(@ApplicationContext private val context: Context) :
    CameraPermissionRepository {

    override fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
}
