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

package com.example.monodim

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.monodim.ar.ARSessionManager
import com.example.monodim.databinding.ActivityMainBinding
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlin.math.sqrt

/**
 * MonoDim AR测量仪 - 极简版本
 * 打开APP → 点第一个点 → 点第二个点 → 显示距离
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MonoDim"
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val PREFS_NAME = "monodim_prefs"
        private const val PREF_UNIT = "measurement_unit"
    }

    enum class MeasurementUnit(val displayName: String, val suffix: String, val fromCm: (Float) -> Float) {
        CM("厘米", "cm", { it }),
        M("米", "m", { it / 100f }),
        INCH("英寸", "in", { it / 2.54f }),
        FEET("英尺", "ft", { it / 30.48f })
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private var arSession: Session? = null
    private var sessionManager: ARSessionManager? = null
    
    private var anchor1: Anchor? = null
    private var anchor2: Anchor? = null
    private val distanceSamples = ArrayDeque<Float>()
    private var distanceUpdater: Runnable? = null
    private val maxSamples = 30
    private var installRequested = false
    private var arInitialized = false
    private var trackingStatusUpdater: Runnable? = null
    private var vibrator: Vibrator? = null
    private var currentUnit: MeasurementUnit = MeasurementUnit.CM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addPointButton.setOnClickListener { onAddPoint() }
        binding.resetButton.setOnClickListener { onReset() }
        binding.distanceText.setOnClickListener { cycleUnit() }

        // 初始化偏好设置
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadUnit()

        // 初始化振动器
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            initAR()
        }
    }

    private fun initAR() {
        if (arInitialized) return

        // 检查ARCore可用性
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            showToast(getString(R.string.error_device_not_supported))
            return
        }

        // 请求安装ARCore
        try {
            val installStatus = ArCoreApk.getInstance().requestInstall(this, !installRequested)
            when (installStatus) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return
                }
                ArCoreApk.InstallStatus.INSTALLED -> { /* 继续初始化 */ }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            showToast(getString(R.string.error_install_arcore))
            return
        } catch (e: UnavailableArcoreNotInstalledException) {
            showToast(getString(R.string.error_install_arcore))
            return
        } catch (e: UnavailableApkTooOldException) {
            showToast(getString(R.string.error_update_arcore))
            return
        } catch (e: UnavailableSdkTooOldException) {
            showToast(getString(R.string.error_update_app))
            return
        } catch (e: UnavailableDeviceNotCompatibleException) {
            showToast(getString(R.string.error_device_not_supported))
            return
        } catch (e: Exception) {
            showToast("ARCore初始化失败: ${e.message}")
            Log.e(TAG, "ARCore安装检查失败", e)
            return
        }

        try {
            arSession = Session(this).apply {
                val config = Config(this).apply {
                    instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    focusMode = Config.FocusMode.AUTO
                    lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                    if (isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        depthMode = Config.DepthMode.AUTOMATIC
                    }
                }
                configure(config)
            }

            sessionManager = ARSessionManager(arSession!!)
            binding.surfaceView.apply {
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                preserveEGLContextOnPause = true
                setRenderer(sessionManager)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
            arInitialized = true

        } catch (e: UnavailableDeviceNotCompatibleException) {
            showToast(getString(R.string.error_device_not_supported))
            Log.e(TAG, "设备不兼容", e)
        } catch (e: Exception) {
            showToast("AR启动失败: ${e.message}")
            Log.e(TAG, "AR会话创建失败", e)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun vibrateSuccess() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(50)
            }
        }
    }

    private fun vibrateError() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 30, 50, 30), -1)
            }
        }
    }

    private fun updateTrackingStatus() {
        val frame = sessionManager?.currentFrame
        if (frame == null) {
            binding.trackingStatus.text = getString(R.string.status_initializing)
            binding.trackingStatus.setTextColor(0xFFFFEB3B.toInt()) // 黄色
            binding.reticle.setBackgroundResource(R.drawable.reticle_inactive)
            return
        }

        when (frame.camera.trackingState) {
            TrackingState.TRACKING -> {
                binding.trackingStatus.text = getString(R.string.status_tracking)
                binding.trackingStatus.setTextColor(0xFF4CAF50.toInt()) // 绿色
                binding.reticle.setBackgroundResource(R.drawable.reticle_active)
            }
            TrackingState.PAUSED -> {
                binding.trackingStatus.text = getString(R.string.status_searching)
                binding.trackingStatus.setTextColor(0xFFFFEB3B.toInt()) // 黄色
                binding.reticle.setBackgroundResource(R.drawable.reticle_inactive)
            }
            TrackingState.STOPPED -> {
                binding.trackingStatus.text = getString(R.string.status_not_tracking)
                binding.trackingStatus.setTextColor(0xFFFF5722.toInt()) // 红色
                binding.reticle.setBackgroundResource(R.drawable.reticle_inactive)
            }
        }
    }

    private fun updateHintText() {
        binding.hintText.text = when {
            anchor1 == null -> getString(R.string.hint_place_first_point)
            anchor2 == null -> getString(R.string.hint_place_second_point)
            else -> getString(R.string.hint_measuring)
        }
        binding.hintText.visibility = if (anchor2 != null) View.GONE else View.VISIBLE
    }

    private fun startTrackingStatusUpdates() {
        val updater = object : Runnable {
            override fun run() {
                updateTrackingStatus()
                binding.surfaceView.postDelayed(this, 200)
            }
        }
        trackingStatusUpdater = updater
        binding.surfaceView.post(updater)
    }

    private fun stopTrackingStatusUpdates() {
        trackingStatusUpdater?.let { binding.surfaceView.removeCallbacks(it) }
        trackingStatusUpdater = null
    }

    private fun loadUnit() {
        val unitName = prefs.getString(PREF_UNIT, MeasurementUnit.CM.name) ?: MeasurementUnit.CM.name
        currentUnit = try {
            MeasurementUnit.valueOf(unitName)
        } catch (e: Exception) {
            MeasurementUnit.CM
        }
        updateDistanceDisplay(0f)
    }

    private fun saveUnit() {
        prefs.edit().putString(PREF_UNIT, currentUnit.name).apply()
    }

    private fun cycleUnit() {
        val units = MeasurementUnit.values()
        val currentIndex = units.indexOf(currentUnit)
        currentUnit = units[(currentIndex + 1) % units.size]
        saveUnit()
        vibrateSuccess()

        // 如果正在测量，更新显示
        if (anchor1 != null && anchor2 != null) {
            val distCm = computeDistanceCm()
            if (distCm != null) {
                updateDistanceDisplay(robustAverage(distanceSamples))
            }
        } else {
            updateDistanceDisplay(0f)
        }
        showToast("单位: ${currentUnit.displayName}")
    }

    private fun updateDistanceDisplay(distanceCm: Float) {
        val converted = currentUnit.fromCm(distanceCm)
        val format = when (currentUnit) {
            MeasurementUnit.CM -> "%.1f"
            MeasurementUnit.M -> "%.2f"
            MeasurementUnit.INCH -> "%.1f"
            MeasurementUnit.FEET -> "%.2f"
        }
        binding.distanceText.text = "$format ${currentUnit.suffix}".format(converted)
    }

    private fun onAddPoint() {
        val session = arSession
        if (session == null) {
            showToast(getString(R.string.error_ar_not_initialized))
            vibrateError()
            return
        }

        val frame = sessionManager?.currentFrame
        if (frame == null) {
            showToast(getString(R.string.error_waiting_ar))
            vibrateError()
            return
        }

        try {
            // 检查相机跟踪状态
            val camera = frame.camera
            if (camera.trackingState != TrackingState.TRACKING) {
                showToast(getString(R.string.error_move_device))
                vibrateError()
                return
            }

            val view = binding.surfaceView
            if (view.width <= 0 || view.height <= 0) return
            val centerX = view.width * 0.5f
            val centerY = view.height * 0.5f

            val hitResults: List<HitResult>
            try {
                hitResults = frame.hitTest(centerX, centerY)
            } catch (e: Exception) {
                Log.w(TAG, "hitTest失败，帧可能已过期", e)
                showToast(getString(R.string.error_aim_at_surface))
                vibrateError()
                return
            }

            val bestHit = selectBestHit(hitResults)
            if (bestHit == null) {
                showToast(getString(R.string.error_aim_at_surface))
                vibrateError()
                return
            }

            val anchor: Anchor
            try {
                anchor = bestHit.createAnchor()
            } catch (e: Exception) {
                Log.w(TAG, "创建锚点失败", e)
                showToast(getString(R.string.error_anchor_failed))
                vibrateError()
                return
            }

            // 验证锚点跟踪状态
            if (anchor.trackingState != TrackingState.TRACKING) {
                anchor.detach()
                showToast(getString(R.string.error_anchor_failed))
                vibrateError()
                return
            }

            if (anchor1 == null) {
                anchor1 = anchor
                binding.point1Indicator.setBackgroundResource(R.drawable.dot_active)
                vibrateSuccess()
                updateHintText()
            } else if (anchor2 == null) {
                anchor2 = anchor
                binding.point2Indicator.setBackgroundResource(R.drawable.dot_active)
                vibrateSuccess()
                updateHintText()
                startDistanceUpdates()
            } else {
                anchor.detach()
            }
        } catch (e: Exception) {
            Log.e(TAG, "放置点失败", e)
            showToast(getString(R.string.error_placement_failed))
            vibrateError()
        }
    }

    private fun startDistanceUpdates() {
        distanceSamples.clear()
        val updater = object : Runnable {
            override fun run() {
                val dist = computeDistanceCm() ?: return
                pushDistanceSample(dist)
                val display = robustAverage(distanceSamples)
                updateDistanceDisplay(display)
                binding.surfaceView.postOnAnimation(this)
            }
        }
        distanceUpdater = updater
        binding.surfaceView.postOnAnimation(updater)
    }

    private fun stopDistanceUpdates() {
        distanceUpdater?.let { binding.surfaceView.removeCallbacks(it) }
        distanceUpdater = null
        distanceSamples.clear()
    }

    private fun computeDistanceCm(): Float? {
        val p1 = anchor1?.pose ?: return null
        val p2 = anchor2?.pose ?: return null
        if (anchor1?.trackingState != TrackingState.TRACKING) return null
        if (anchor2?.trackingState != TrackingState.TRACKING) return null

        val dx = p2.tx() - p1.tx()
        val dy = p2.ty() - p1.ty()
        val dz = p2.tz() - p1.tz()
        val distanceCm = sqrt(dx*dx + dy*dy + dz*dz) * 100f

        // 距离边界检查：1cm到10m范围
        if (distanceCm < 1f || distanceCm > 1000f) return null
        return distanceCm
    }

    private fun pushDistanceSample(value: Float) {
        distanceSamples.addLast(value)
        while (distanceSamples.size > maxSamples) distanceSamples.removeFirst()
    }

    private fun robustAverage(samples: Collection<Float>): Float {
        if (samples.isEmpty()) return 0f
        val sorted = samples.sorted()
        val n = sorted.size

        // 计算中位数
        val median = if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) * 0.5f
        }

        // IQR异常值检测
        val q1Index = n / 4
        val q3Index = (3 * n) / 4
        val q1 = sorted[q1Index]
        val q3 = sorted[q3Index]
        val iqr = q3 - q1

        // 距离自适应容差：近距离用固定值，远距离用百分比
        val baseTolerance = maxOf(1.5f * iqr, 1.0f)
        val percentTolerance = median * 0.03f  // 3%容差
        val tolerance = maxOf(baseTolerance, percentTolerance)

        val filtered = sorted.filter { kotlin.math.abs(it - median) <= tolerance }
        return if (filtered.isNotEmpty()) {
            filtered.average().toFloat()
        } else {
            median
        }
    }

    private fun onReset() {
        anchor1?.detach()
        anchor2?.detach()
        anchor1 = null
        anchor2 = null
        stopDistanceUpdates()
        binding.point1Indicator.setBackgroundResource(R.drawable.dot_inactive)
        binding.point2Indicator.setBackgroundResource(R.drawable.dot_inactive)
        updateDistanceDisplay(0f)
        updateHintText()
        vibrateSuccess()
    }

    override fun onResume() {
        super.onResume()

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // 尝试初始化AR（处理从ARCore安装返回的情况）
        if (!arInitialized) {
            initAR()
        }

        try {
            arSession?.resume()
            binding.surfaceView.onResume()
            startTrackingStatusUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "AR会话恢复失败", e)
            showToast(getString(R.string.error_ar_resume_failed))
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            stopDistanceUpdates()
            stopTrackingStatusUpdates()
            binding.surfaceView.onPause()
            arSession?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "AR会话暂停失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopDistanceUpdates()
            stopTrackingStatusUpdates()
            anchor1?.detach()
            anchor2?.detach()
            anchor1 = null
            anchor2 = null
            arSession?.close()
            arSession = null
            sessionManager = null
            arInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "资源释放失败", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
                initAR()
            } else {
                showToast(getString(R.string.error_camera_permission))
            }
        }
    }
    
    private fun Pose.tx(): Float = translation[0]
    private fun Pose.ty(): Float = translation[1]
    private fun Pose.tz(): Float = translation[2]

    private fun selectBestHit(hitResults: List<HitResult>): HitResult? {
        if (hitResults.isEmpty()) return null

        // 优先级1：已跟踪平面上的命中（放宽边界检查）
        val trackingPlaneHit = hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            trackable is Plane &&
                trackable.trackingState == TrackingState.TRACKING &&
                trackable.isPoseInPolygon(hit.hitPose)
        }
        if (trackingPlaneHit != null) return trackingPlaneHit

        // 优先级2：任意平面命中（不要求在多边形内）
        val anyPlaneHit = hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            trackable is Plane && trackable.trackingState == TrackingState.TRACKING
        }
        if (anyPlaneHit != null) return anyPlaneHit

        // 优先级3：深度点命中
        val depthHit = hitResults.firstOrNull { hit ->
            hit.trackable is DepthPoint
        }
        if (depthHit != null) return depthHit

        // 优先级4：带表面法线的特征点
        val orientedPointHit = hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
        }
        if (orientedPointHit != null) return orientedPointHit

        // 优先级5：任意命中
        return hitResults.firstOrNull()
    }
}
