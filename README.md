# MonoDim AR测量仪

一个基于ARCore的极简AR测距工具，打开即用，点哪量哪。

## 功能

- **即时测量**：无需等待平面检测，打开APP直接测量
- **极简操作**：点击放置第一个点，再点击放置第二个点，实时显示距离
- **任意表面**：支持地板、桌面、墙面、树木、石头等任意有纹理的表面
- **厘米级精度**：基于ARCore视觉惯性里程计(VIO)技术

## 使用说明

1. 打开APP，对准要测量的物体
2. 将准星对准起始位置，点击 **+** 放置第一个点
3. 移动手机，将准星对准终点位置，再次点击 **+** 放置第二个点
4. 屏幕顶部显示两点间的直线距离（单位：厘米）
5. 点击 **重置** 开始新的测量

## 系统要求

- Android 7.0+ (API 24+)
- 支持ARCore的设备
- 摄像头权限

## 技术栈

- **语言**：Kotlin
- **AR框架**：Google ARCore
- **UI**：原生Android View系统
- **构建工具**：Gradle + Kotlin DSL

## 项目结构

```
app/src/main/java/com/example/monodim/
├── MainActivity.kt          # 主界面和测量逻辑
└── ar/
    ├── ARSessionManager.kt  # AR会话管理
    └── BackgroundRenderer.kt # 相机背景渲染
```

## 构建

```bash
./gradlew assembleDebug
```

## 许可证

GNU GPL v3.0 with Additional IP Protection Terms
