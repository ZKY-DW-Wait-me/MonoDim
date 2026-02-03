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

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 简化版背景渲染器 - 确保相机画面显示
 */
class BackgroundRenderer {

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTexCoord);
            }
        """

        // 全屏四边形
        private val VERTICES = floatArrayOf(
            -1f, -1f,  // 左下
             1f, -1f,  // 右下
            -1f,  1f,  // 左上
             1f,  1f   // 右上
        )

        private val TEX_COORDS = floatArrayOf(
            0f, 0f,  // 左下
            1f, 0f,  // 右下
            0f, 1f,  // 左上
            1f, 1f   // 右上
        )
    }

    private var program = 0
    private var textureId = -1
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var quadCoordsBuffer: FloatBuffer
    private lateinit var transformedTexCoords: FloatBuffer

    fun createOnGlThread() {
        // 创建纹理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        // 配置外部纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // 验证着色器程序链接状态
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program链接失败: $error")
        }
        checkGlError("Program链接")

        // 创建顶点缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(VERTICES); position(0) }

        // quadCoordsBuffer用于存储NDC坐标供transformCoordinates2d使用
        quadCoordsBuffer = ByteBuffer.allocateDirect(VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(VERTICES); position(0) }

        // transformedTexCoords需要初始化默认值
        transformedTexCoords = ByteBuffer.allocateDirect(TEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(TEX_COORDS); position(0) }

        checkGlError("缓冲区初始化")
    }

    fun getTextureId(): Int = textureId

    fun draw(frame: Frame) {
        if (textureId == -1) return

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // 禁用深度测试，让背景在最底层
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        // 使用着色器
        GLES20.glUseProgram(program)

        // 绑定纹理
        val textureLoc = GLES20.glGetUniformLocation(program, "sTexture")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(textureLoc, 0)

        // 变换纹理坐标（处理屏幕旋转）
        quadCoordsBuffer.position(0)
        frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
            quadCoordsBuffer,
            Coordinates2d.TEXTURE_NORMALIZED,
            transformedTexCoords
        )

        // 设置顶点属性
        val posLoc = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(posLoc)

        val texLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        transformedTexCoords.position(0)
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, transformedTexCoords)
        GLES20.glEnableVertexAttribArray(texLoc)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 清理
        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(texLoc)
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader编译失败: $error")
        }
        return shader
    }

    private fun checkGlError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("BackgroundRenderer", "GL错误 [$operation]: $error")
            throw RuntimeException("GL错误 [$operation]: $error")
        }
    }
}
