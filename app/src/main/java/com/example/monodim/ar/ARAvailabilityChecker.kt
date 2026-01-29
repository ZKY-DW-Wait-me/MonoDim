/*
 * Project: MonoDim
 * Author: ZKY-DW-Wait-me
 * Copyright (c) 2024 ZKY-DW-Wait-me. All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 * 
 * 法律声明：严禁未经授权将此代码用于软件著作权、专利等知识产权申请。
 */

package com.example.monodim.ar

import android.app.Activity
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

/**
 * AR 可用性检查器
 * 
 * 负责：
 * 1. 检查设备是否支持 ARCore
 * 2. 检查/请求 Camera 权限
 * 3. 引导用户安装 Google Play Services for AR
 */
class ARAvailabilityChecker(private val activity: Activity) {

    companion object {
        const val CAMERA_PERMISSION_CODE = 1001
    }

    /**
     * 检查 ARCore 是否可用
     * 
     * @return Availability 状态枚举
     */
    fun checkARCoreAvailability(): ArCoreApk.Availability {
        return ArCoreApk.getInstance().checkAvailability(activity)
    }

    /**
     * 检查 ARCore 是否已安装，如未安装则请求安装
     * 
     * 需要在 Activity 的 onResume() 中调用
     * 
     * @return true 表示 ARCore 已就绪或正在安装，false 表示用户拒绝安装或设备不兼容
     */
    fun requestInstall(): Boolean {
        return try {
            when (ArCoreApk.getInstance().requestInstall(activity, true)) {
                ArCoreApk.InstallStatus.INSTALLED -> true
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // 用户被提示安装，等待结果
                    false
                }
                else -> false
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            // 用户拒绝了安装
            false
        } catch (e: UnavailableDeviceNotCompatibleException) {
            // 设备不兼容
            false
        } catch (e: Exception) {
            // 其他异常
            false
        }
    }

    /**
     * 检查 Camera 权限是否已授予
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求 Camera 权限
     */
    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    /**
     * 检查是否所有前提条件都已满足（权限 + ARCore 安装）
     */
    fun isReady(): Boolean {
        if (!hasCameraPermission()) return false
        
        val availability = checkARCoreAvailability()
        return availability.isSupported && 
               ArCoreApk.getInstance().requestInstall(activity, false) == 
               ArCoreApk.InstallStatus.INSTALLED
    }
}
