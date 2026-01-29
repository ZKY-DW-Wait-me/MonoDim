package com.example.monodim.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import com.google.ar.core.Frame
import com.google.ar.core.Session
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARSessionManager(
    private val session: Session
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ARSessionManager"
    }

    private val backgroundRenderer = BackgroundRenderer()
    private var displayRotation = Surface.ROTATION_0
    private var viewportWidth = 1
    private var viewportHeight = 1
    
    @Volatile
    var currentFrame: Frame? = null
        private set
    
    @Volatile
    var isReady = false
        private set

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.createOnGlThread()
        val textureId = backgroundRenderer.getTextureId()
        session.setCameraTextureName(textureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height
        updateDisplayGeometry()
    }

    fun setDisplayRotation(rotation: Int) {
        displayRotation = rotation
        updateDisplayGeometry()
    }

    private fun updateDisplayGeometry() {
        try {
            val rotation = when (displayRotation) {
                Surface.ROTATION_90 -> 1
                Surface.ROTATION_180 -> 2
                Surface.ROTATION_270 -> 3
                else -> 0
            }
            session.setDisplayGeometry(rotation, viewportWidth, viewportHeight)
        } catch (e: Exception) {
            Log.e(TAG, "显示几何更新失败", e)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val frame = session.update()
            currentFrame = frame
            backgroundRenderer.draw(frame)
            if (!isReady) isReady = true
        } catch (e: Exception) {
            Log.e(TAG, "渲染帧失败", e)
        }
    }
}
