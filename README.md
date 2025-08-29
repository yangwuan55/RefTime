# 🕐 TrueTime Android - 现代化时间同步库

![TrueTime](truetime.png "TrueTime for Android")

**完全现代化的 NTP 客户端库，基于 Kotlin 协程、Flow 和 kotlinx-datetime 构建，为 Android 应用提供准确可靠的网络时间同步功能。**

[![JitPack](https://jitpack.io/v/instacart/truetime-android.svg)](https://jitpack.io/#instacart/truetime-android)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

## ✨ 特性亮点

- **🚀 完全现代化**: 基于 Kotlin 协程、Flow 和 kotlinx-datetime
- **⏱️ 准确可靠**: 提供毫秒级精度的网络时间同步
- **🌊 响应式编程**: 使用 StateFlow 和 Flow 实现响应式状态管理
- **🛡️ 类型安全**: 密封类错误处理和类型安全的 API
- **📱 Compose 友好**: 完美支持 Jetpack Compose
- **🧪 测试完备**: 内置测试工具和模拟支持
- **🔧 灵活配置**: Kotlin DSL 配置，简单易用

## 🎯 为什么需要 TrueTime？

在某些应用中，获取真实准确的日期和时间变得非常重要。大多数设备上，如果时钟被手动更改，`Date()` 实例会受到本地设置的影响。

用户可能出于各种原因更改时间，比如处于不同时区、为了准时而将时钟调快 5-10 分钟等。您的应用或服务可能需要一个不受这些更改影响且可靠的准确时间源。TrueTime 正是为此而生。

## 🚀 快速开始

### 安装依赖

在项目的 `build.gradle.kts` 中添加：

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.instacart:truetime-android:<version>")
}
```

### 基本使用

```kotlin
class MyApp : Application() {
    val refTime = RefTime {
        ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org")
        timeout(Duration.parse("PT30S"))
        retries(3)
        debug(BuildConfig.DEBUG)
    }
    
    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            refTime.sync().onSuccess {
                Log.d("TrueTime", "✅ 时间同步成功")
            }.onFailure { error ->
                Log.e("TrueTime", "❌ 同步失败: ${error.message}")
            }
        }
    }
}
```

### 响应式使用

```kotlin
// 监听状态变化
lifecycleScope.launch {
    refTime.state.collect { state ->
        when (state) {
            RefTimeState.Uninitialized -> showLoading()
            is RefTimeState.Syncing -> updateProgress(state.progress)
            is RefTimeState.Available -> showAccurateTime()
            is RefTimeState.Failed -> showError(state.error)
        }
    }
}

// 获取时间更新
lifecycleScope.launch {
    refTime.timeUpdates.collectLatest { instant ->
        updateTimeDisplay(instant)
    }
}
```

## 📖 核心 API

### 时间访问方法

```kotlin
// 所有方法都返回 kotlinx.datetime.Instant
val accurateTime = refTime.now()         // 准确网络时间
val safeTime = refTime.nowSafe()         // 失败时回退到系统时间
val optionalTime = refTime.nowOrNull()   // 未同步时返回 null

// 时间计算
val offset = refTime.getClockOffset()    // 时钟偏移量
val duration = refTime.durationSince(someInstant) // 时间间隔

// 实用工具
val timestamp = refTime.nowMillis()      // Unix 时间戳（毫秒）
val isSynced = refTime.hasSynced()       // 检查是否已同步
```

### Jetpack Compose 集成

```kotlin
@Composable
fun TimeDisplay(refTime: RefTime) {
    val state by refTime.state.collectAsStateWithLifecycle()
    val currentTime by refTime.timeUpdates
        .collectAsStateWithLifecycle(initialValue = null)
    
    when (val current = state) {
        RefTimeState.Uninitialized -> 
            Text("正在初始化...")
        is RefTimeState.Syncing -> 
            CircularProgressIndicator(current.progress)
        is RefTimeState.Available -> 
            Text("准确时间: ${currentTime?.formatForDisplay()}")
        is RefTimeState.Failed -> 
            Text("错误: ${current.error.message}")
    }
}
```

## ⚙️ 配置选项

| 参数 | 描述 | 默认值 |
|------|------|--------|
| `ntpHosts` | NTP 服务器列表 | `["time.google.com"]` |
| `connectionTimeout` | 连接超时时间 | `30.seconds` |
| `maxRetries` | 最大重试次数 | `3` |
| `baseRetryDelay` | 基础重试延迟 | `1.seconds` |
| `maxRetryDelay` | 最大重试延迟 | `30.seconds` |
| `debug` | 调试模式 | `false` |
| `cacheValidDuration` | 缓存有效期 | `1.hours` |

## 🎨 高级用法

### 自定义配置

```kotlin
val customRefTime = RefTime {
    ntpHosts("cn.pool.ntp.org", "time.windows.com")
    connectionTimeout = Duration.parse("PT15S")
    maxRetries = 5
    baseRetryDelay = Duration.parse("PT2S")
    maxRetryDelay = Duration.parse("PT60S")
    debug = true
    cacheValidDuration = Duration.parse("PT30M")
}
```

### 错误处理

```kotlin
lifecycleScope.launch {
    refTime.sync().fold(
        onSuccess = { 
            // 同步成功
            val time = refTime.now()
        },
        onFailure = { error ->
            when (error) {
                is RefTimeError.NetworkUnavailable -> 
                    showNetworkError()
                is RefTimeError.ServerTimeout -> 
                    showTimeoutError(error.server)
                is RefTimeError.AllServersFailed -> 
                    showAllServersFailed(error.errors)
                else -> showGenericError()
            }
        }
    )
}
```

### 测试支持

```kotlin
class TimeServiceTest {
    private val testTime = Instant.parse("2024-01-15T12:00:00Z")
    private val mockRefTime = TestRefTime(testTime)
    
    @Test
    fun testTimeAccess() = runTest {
        assertEquals(testTime, mockRefTime.now())
        assertTrue(mockRefTime.hasSynced())
    }
}
```

## 📊 性能特性

- **低延迟**: 优化的网络请求，最小化时间同步延迟
- **智能重试**: 指数退避重试机制，避免网络拥塞
- **内存高效**: 使用协程和 Flow，内存占用低
- **电池友好**: 智能调度，减少电池消耗

## 🔄 迁移指南

这是 TrueTime 的完全重写版本，所有 API 都已现代化。如果您从旧版本迁移，请参考 [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)。

**主要变化:**
- `java.util.Date` → `kotlinx.datetime.Instant`
- 阻塞调用 → `suspend` 函数
- 轮询 → `Flow` 响应式
- 构建器 → Kotlin DSL

## 🤝 贡献

我们欢迎贡献！请参阅：[贡献指南](CONTRIBUTING.md)

### 开发设置

```bash
# 克隆项目
git clone https://github.com/instacart/truetime-android.git
cd truetime-android

# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 发布到本地 Maven
./gradlew :library:publishToMavenLocal
```

## 📝 许可证

Apache 2.0 - 详见 [LICENSE](LICENSE) 文件

## 🌟 相关项目

- [TrueTime for Swift](https://github.com/instacart/TrueTime.swift) - Swift 版本的 TrueTime
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) - Kotlin 日期时间库

## 📞 支持

- 📖 [文档](https://github.com/instacart/truetime-android/wiki)
- 🐛 [问题报告](https://github.com/instacart/truetime-android/issues)
- 💬 [讨论](https://github.com/instacart/truetime-android/discussions)

---

**TrueTime Android** - 让您的应用始终显示准确的时间！⏰