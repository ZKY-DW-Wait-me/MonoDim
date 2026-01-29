# MonoDim 开发任务清单

> 基于 AR 的单目测距仪应用开发计划

---

## 阶段一：环境配置 ✅

- [x] **1.1 Gradle 配置**
  - [x] 添加 ARCore SDK 依赖 (`com.google.ar:core:1.46.0`)
  - [x] 确认 minSdk >= 24
  - [x] 启用 ViewBinding

- [x] **1.2 AndroidManifest 配置**
  - [x] 添加 CAMERA 权限
  - [x] 声明 `android.hardware.camera.ar` feature (required=true)
  - [x] 添加 ARCore metadata (`com.google.ar.core` = required)

- [x] **1.3 基础布局**
  - [x] 清理默认 Hello World 布局
  - [x] 添加全屏 GLSurfaceView (id: surfaceView)

---

## 阶段二：AR 基础运行 ✅

- [x] **2.1 AR 可用性检查器 (`ARAvailabilityChecker`)**
  - [x] 实现 `checkARCoreAvailability()`：检测设备是否支持 AR
  - [x] 实现 `requestInstall()`：引导用户安装 Google Play Services for AR
  - [x] 实现 `checkCameraPermission()`：检查并请求 Camera 权限

- [x] **2.2 AR 会话管理器 (`ARSessionManager`)**
  - [x] 创建 `ARSession` 实例（配置自动对焦、深度模式、即时放置）
  - [x] 实现 `onResume()`：恢复 Session 并处理安装异常
  - [x] 实现 `onPause()`：暂停 Session
  - [x] 实现 `onSurfaceCreated/Changed/Draw()`：GL 回调桥接

- [x] **2.3 主 Activity 基础框架**
  - [x] 集成 ARSessionManager 生命周期
  - [x] 实现权限请求回调处理
  - [ ] 验证 AR 能正常启动并显示相机预览

---

## 阶段三：核心测量逻辑 ✅

### 3.0 终极优化版（已完成）

- [x] **准星瞄准模式**
  - [x] 屏幕中心固定准星 UI
  - [x] 每帧自动对中心进行射线检测
  - [x] 大按钮（底部中央）添加测量点
  - [x] 准星颜色指示可用状态（绿色=可用，灰色=不可用）

- [x] **深度检测 API**
  - [x] 检查设备 Depth API 支持
  - [x] 开启 `Config.DepthMode.AUTOMATIC`
  - [x] 优先平面检测，备选深度图定位

- [x] **UI 防抖处理**
  - [x] 500ms 状态缓存
  - [x] 状态本质变化时才更新文字
  - [x] 追踪不稳定时准星变灰，不显示报错

- [x] **数学滤波**
  - [x] 低通滤波平滑坐标（lerp 插值）
  - [x] 防止画面晃动时位置乱跳

- [x] **视觉反馈**
  - [x] 左上角点计数指示器
  - [x] 实时状态提示文字
  - [x] 距离大字显示（绿色高亮）

## 待完善 🔄

- [ ] **3D 锚点可视化** - 添加可见的红色标记点
- [ ] **两点连线渲染** - 空间连线显示测量路径
- [ ] **测量精度校准** - 实际环境测试与调整

---

## 阶段四：UI 完善 ⏳

- [ ] **4.1 视觉准星与状态**
  - [ ] 实现 `MeasurementOverlayView` 自定义 View
  - [ ] 绘制屏幕中心准星（十字/圆形）
  - [ ] 显示实时状态文本（"正在寻找平面..."/"已锁定"/"距离：x.xx m"）

- [ ] **4.2 控制面板**
  - [ ] 设计悬浮操作面板布局
  - [ ] 实现"记录点 A" / "记录点 B" 按钮
  - [ ] 实现"重置"按钮
  - [ ] 实现距离结果文本显示区域

- [ ] **4.3 状态处理**
  - [ ] 处理 `TRACKING_LOST` 状态的 UI 反馈
  - [ ] 处理 `PAUSED` / `STOPPED` 状态
  - [ ] 添加 Toast/Snackbar 错误提示

---

## 阶段五：测试与优化 ⏳

- [ ] **5.1 功能测试**
  - [ ] 验证 AR 启动流程（权限检查 → AR 安装 → 相机预览）
  - [ ] 验证测量精度（对比实际测量值）
  - [ ] 验证各种平面检测场景（桌面、地面、墙面）

- [ ] **5.2 异常处理**
  - [ ] 测试低光照环境下的行为
  - [ ] 测试快速移动时的追踪丢失恢复
  - [ ] 测试后台恢复后的状态一致性

- [ ] **5.3 性能优化**
  - [ ] 优化渲染帧率
  - [ ] 减少内存分配（避免 GC 抖动）
  - [ ] 代码审查：确保无内存泄漏

---

## 附录：技术参考

### ARCore 关键类
- `Session`: AR 会话核心
- `Frame`: 单帧 AR 数据
- `HitResult`: 射线检测结果
- `Anchor`: 现实世界中的固定点
- `Pose`: 6DOF 位置和旋转

### 测量算法公式
```kotlin
// 欧几里得距离（3D空间）
distance = sqrt((x2-x1)² + (y2-y1)² + (z2-z1)²)
```

### 颜色规范
- 准星颜色：`#FFFFFF` (白色)
- 锚点颜色：`#00BCD4` (青色)
- 连线颜色：`#FF9800` (橙色)
- 控制面板背景：`#80000000` (半透明黑)

---

*最后更新：2026-01-29*
