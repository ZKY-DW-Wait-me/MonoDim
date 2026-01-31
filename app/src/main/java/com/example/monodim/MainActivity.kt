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
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
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
import kotlin.math.sqrt

/**
 * MonoDim AR测量仪 - 极简版本
 * 打开APP → 点第一个点 → 点第二个点 → 显示距离
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var arSession: Session? = null
    private var sessionManager: ARSessionManager? = null
    
    private var anchor1: Anchor? = null
    private var anchor2: Anchor? = null
    private val distanceSamples = ArrayDeque<Float>()
    private var distanceUpdater: Runnable? = null
    private val maxSamples = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addPointButton.setOnClickListener { onAddPoint() }
        binding.resetButton.setOnClickListener { onReset() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            initAR()
        }
    }

    private fun initAR() {
        try {
            ArCoreApk.getInstance().requestInstall(this, true)
        } catch (e: Exception) {
            Toast.makeText(this, "请安装ARCore", Toast.LENGTH_LONG).show()
            return
        }

        try {
            arSession = Session(this).apply {
                val config = Config(this).apply {
                    instantPlacementMode = Config.InstantPlacementMode.DISABLED
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
                setEGLContextClientVersion(3)
                setRenderer(sessionManager)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }

        } catch (e: Exception) {
            Log.e("MonoDim", "启动失败", e)
            Toast.makeText(this, "AR启动失败", Toast.LENGTH_LONG).show()
        }
    }

    private fun onAddPoint() {
        val session = arSession ?: return
        val frame = sessionManager?.currentFrame ?: return
        
        if (frame.camera.trackingState != TrackingState.TRACKING) return

        try {
            val view = binding.surfaceView
            if (view.width <= 0 || view.height <= 0) return
            val centerX = view.width * 0.5f
            val centerY = view.height * 0.5f
            val hitResults = frame.hitTest(centerX, centerY)
            val bestHit = selectBestHit(hitResults)
            if (bestHit == null) {
                Toast.makeText(this, "请移动设备，等待平面识别", Toast.LENGTH_SHORT).show()
                return
            }
            val anchor = bestHit.createAnchor()
            
            if (anchor1 == null) {
                anchor1 = anchor
                binding.point1Indicator.setBackgroundResource(R.drawable.dot_active)
            } else if (anchor2 == null) {
                anchor2 = anchor
                binding.point2Indicator.setBackgroundResource(R.drawable.dot_active)
                startDistanceUpdates()
            } else {
                anchor.detach()
            }
        } catch (e: Exception) {
            Log.e("MonoDim", "放置失败", e)
        }
    }

    private fun startDistanceUpdates() {
        distanceSamples.clear()
        val updater = object : Runnable {
            override fun run() {
                val dist = computeDistanceCm() ?: return
                pushDistanceSample(dist)
                val avg = distanceSamples.average().toFloat()
                binding.distanceText.text = String.format("%.1f cm", avg)
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
        
        val dx = p2.tx() - p1.tx()
        val dy = p2.ty() - p1.ty()
        val dz = p2.tz() - p1.tz()
        return sqrt(dx*dx + dy*dy + dz*dz) * 100f
    }

    private fun pushDistanceSample(value: Float) {
        distanceSamples.addLast(value)
        while (distanceSamples.size > maxSamples) {
            distanceSamples.removeFirst()
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
        binding.distanceText.text = "0.0 cm"
    }

    override fun onResume() {
        super.onResume()
        try {
            arSession?.resume()
            binding.surfaceView.onResume()
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            stopDistanceUpdates()
            binding.surfaceView.onPause()
            arSession?.pause()
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopDistanceUpdates()
            anchor1?.detach()
            anchor2?.detach()
            arSession?.close()
        } catch (e: Exception) {}
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) initAR()
    }
    
    private fun Pose.tx(): Float = translation[0]
    private fun Pose.ty(): Float = translation[1]
    private fun Pose.tz(): Float = translation[2]

    private fun selectBestHit(hitResults: List<HitResult>): HitResult? {
        if (hitResults.isEmpty()) return null
        val planeHit = hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
        }
        if (planeHit != null) return planeHit

        val depthHit = hitResults.firstOrNull { hit ->
            hit.trackable is DepthPoint
        }
        if (depthHit != null) return depthHit

        val orientedPointHit = hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
        }
        return orientedPointHit ?: hitResults.firstOrNull()
    }
}
