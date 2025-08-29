[根目录](../CLAUDE.md) > **app**

# app 模块文档

## 模块职责

app 模块是 RefTime Android 的示例应用，展示如何使用 library 模块提供的现代化时间同步功能。使用 Jetpack Compose 构建现代化 UI，演示响应式时间管理和状态处理。

## 入口与启动

### 主要组件
- **App**: 应用类，初始化 RefTime 实例
- **SampleActivity**: 主界面 Activity，展示时间同步功能
- **RefTimeApp**: Compose 应用入口

### 应用初始化
```kotlin
class App : Application() {
    val refTime: RefTime = RefTime {
        ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org", "time.cloudflare.com")
        timeout(Duration.parse("PT30S"))
        retries(3)
        retryDelay(base = Duration.parse("PT1S"), max = Duration.parse("PT30S"))
        debug(BuildConfig.DEBUG)
        cacheValid(Duration.parse("PT1H"))
    }
}
```

## 对外接口

### UI 组件
- **RefTimeStateCard**: 显示同步状态卡片
- **RefTimeDisplayCard**: 显示当前时间卡片
- **RefTimeControlsCard**: 显示控制按钮卡片
- **RefTimeDebugCard**: 显示调试信息卡片

### 功能特性
- 实时同步状态显示
- 响应式时间更新
- 手动同步控制
- 调试信息查看

## 关键依赖与配置

### 依赖库
```kotlin
dependencies {
    implementation(project(":library"))
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    
    // Compose Core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.7.2")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
}
```

### Android 配置
```kotlin
android {
    namespace = "com.milo.sample"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.instacart.truetime"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

## 数据模型

### 状态管理
使用 Compose 的 State 和 Flow 进行响应式状态管理：

```kotlin
val state by refTime.state.collectAsStateWithLifecycle()
val currentTime by refTime.timeUpdates.collectAsStateWithLifecycle(initialValue = null)
```

### UI 状态
- 同步状态显示（未初始化、同步中、可用、失败）
- 当前时间显示
- 控制按钮状态
- 调试信息显示

## 测试与质量

### 测试策略
- **UI 测试**: Compose 界面测试
- **集成测试**: 与 library 模块的集成测试
- **功能测试**: 时间同步功能测试

### 质量保证
- 使用 Material Design 3 组件
- 响应式布局设计
- 无障碍支持
- 深色主题支持

## 常见问题 (FAQ)

### Q: 如何在自己的应用中使用这个库？
A: 参考 app 模块中的代码，首先初始化 RefTime 实例，然后通过 state 流和时间更新流来监听状态和时间变化。

### Q: 应用启动时是否自动同步时间？
A: 当前示例中应用启动时不自动同步，需要用户手动点击"同步时间"按钮。可以根据需要修改 App 类的 onCreate 方法。

### Q: 如何处理同步失败的情况？
A: 通过监听 state 流，当状态变为 Failed 时显示错误信息，并提供重试按钮。

## 相关文件清单

### 核心文件
- `src/main/java/com/milo/sample/App.kt` - 应用类，RefTime 初始化
- `src/main/java/com/milo/sample/SampleActivity.kt` - 主界面 Activity

### UI 组件
- `src/main/java/com/milo/sample/SampleActivity.kt` - 包含所有 Compose 组件

### 资源配置
- `src/main/res/values/strings.xml` - 字符串资源
- `src/main/res/values/colors.xml` - 颜色资源
- `src/main/res/values/styles.xml` - 样式资源

### Manifest
- `src/main/AndroidManifest.xml` - Android 应用配置

## 变更记录 (Changelog)

### 2025-08-28 16:52:33
- 创建模块级 CLAUDE.md 文档
- 记录示例应用架构和使用方式
- 添加关键文件清单

---

*本文件由 Claude Code 自动生成，最后更新于 2025-08-28 16:52:33*