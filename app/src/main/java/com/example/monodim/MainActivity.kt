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
import com.google.ar.core.HitResult
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
                    instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    focusMode = Config.FocusMode.AUTO
                    lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
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
            // 先尝试hitTest，如果没有就强制在相机前方放置
            val hitResults = frame.hitTest(0.5f, 0.5f)
            val anchor = if (hitResults.isNotEmpty()) {
                hitResults.first().createAnchor()
            } else {
                // 强制在相机前方0.8米放置
                val camera = frame.camera
                val pose = camera.pose
                val tx = pose.tx()
                val ty = pose.ty()
                val tz = pose.tz()
                val zAxis = pose.zAxis
                val forwardX = -zAxis[0]
                val forwardY = -zAxis[1]
                val forwardZ = -zAxis[2]
                val targetPose = com.google.ar.core.Pose(
                    floatArrayOf(tx + forwardX * 0.8f, ty + forwardY * 0.8f, tz + forwardZ * 0.8f),
                    pose.rotationQuaternion
                )
                session.createAnchor(targetPose)
            }
            
            if (anchor1 == null) {
                anchor1 = anchor
                binding.point1Indicator.setBackgroundResource(R.drawable.dot_active)
            } else if (anchor2 == null) {
                anchor2 = anchor
                binding.point2Indicator.setBackgroundResource(R.drawable.dot_active)
                showDistance()
            } else {
                anchor.detach()
            }
        } catch (e: Exception) {
            Log.e("MonoDim", "放置失败", e)
        }
    }

    private fun showDistance() {
        val p1 = anchor1?.pose ?: return
        val p2 = anchor2?.pose ?: return
        
        val dx = p2.tx() - p1.tx()
        val dy = p2.ty() - p1.ty()
        val dz = p2.tz() - p1.tz()
        val dist = sqrt(dx*dx + dy*dy + dz*dz) * 100f
        
        binding.distanceText.text = String.format("%.1f cm", dist)
    }

    private fun onReset() {
        anchor1?.detach()
        anchor2?.detach()
        anchor1 = null
        anchor2 = null
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
            binding.surfaceView.onPause()
            arSession?.pause()
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
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
}
