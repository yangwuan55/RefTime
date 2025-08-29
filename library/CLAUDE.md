[根目录](../CLAUDE.md) > **library**

# library 模块文档

## 模块职责

library 模块是 RefTime Android 的核心库，提供现代化的 NTP 时间同步功能。完全基于 Kotlin 协程、Flow 和 kotlinx-datetime 构建，取代传统的 Java 时间 API。

## 入口与启动

### 主要接口
- **RefTime**: 核心接口，提供时间同步和时间访问功能
- **RefTimeConfig**: 配置类，使用 Kotlin DSL 进行类型安全配置

### 初始化方式
```kotlin
// 使用 DSL 配置
val refTime = RefTime {
    ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org")
    timeout(Duration.parse("PT30S"))
    retries(3)
    debug(BuildConfig.DEBUG)
}

// 使用预定义配置
val refTime = RefTime.default()
val refTime = RefTime.fast()
val refTime = RefTime.reliable()
val refTime = RefTime.debug()
```

## 对外接口

### 核心 API
- `suspend fun sync(): Result<Unit>` - 执行时间同步
- `suspend fun now(): RefTimeInstant` - 获取当前准确时间
- `suspend fun nowOrNull(): RefTimeInstant?` - 安全获取时间
- `suspend fun nowSafe(): RefTimeInstant` - 获取时间，失败时回退到系统时间
- `val state: StateFlow<RefTimeState>` - 同步状态流
- `val timeUpdates: Flow<RefTimeInstant>` - 时间更新事件流

### 状态管理
- `RefTimeState.Uninitialized` - 未初始化
- `RefTimeState.Syncing` - 同步中
- `RefTimeState.Available` - 时间可用
- `RefTimeState.Failed` - 同步失败

## 关键依赖与配置

### 依赖库
```kotlin
dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.network)
}
```

### 配置选项
- `ntpHosts`: NTP 服务器列表
- `connectionTimeout`: 连接超时时间
- `maxRetries`: 最大重试次数
- `baseRetryDelay`: 基础重试延迟
- `maxRetryDelay`: 最大重试延迟
- `debug`: 调试模式
- `cacheValidDuration`: 缓存有效期

## 数据模型

### 时间类型
- `RefTimeInstant` (kotlinx.datetime.Instant) - 时间点
- `RefTimeDuration` (kotlin.time.Duration) - 持续时间

### 状态模型
```kotlin
sealed interface RefTimeState {
    data object Uninitialized : RefTimeState
    data class Syncing(val progress: Float) : RefTimeState
    data class Available(
        val clockOffset: RefTimeDuration,
        val lastSyncTime: RefTimeInstant,
        val accuracy: RefTimeDuration
    ) : RefTimeState
    data class Failed(val error: RefTimeError) : RefTimeState
}
```

### 错误类型
```kotlin
sealed class RefTimeError : Exception() {
    data object NotSynced : RefTimeError()
    data object NetworkUnavailable : RefTimeError()
    data class ServerTimeout(val server: String, val timeout: RefTimeDuration) : RefTimeError()
    data class InvalidResponse(val server: String, val reason: String) : RefTimeError()
    data class AllServersFailed(val errors: Map<String, Exception>) : RefTimeError()
}
```

## 测试与质量

### 测试策略
- 单元测试：核心逻辑测试
- 集成测试：NTP 服务器通信测试
- 性能测试：时间同步性能测试

### 质量工具
- ktlint: 代码格式检查
- detekt: 静态代码分析
- JaCoCo: 代码覆盖率检查

## 常见问题 (FAQ)

### Q: 如何处理网络不可用的情况？
A: 库会自动重试配置的次数，最终会进入 Failed 状态，可以通过 state 流监听状态变化。

### Q: 时间同步的准确度如何？
A: 使用 NTP 协议，通常可以达到毫秒级的准确度，具体取决于网络条件和服务器质量。

### Q: 是否支持缓存？
A: 是的，支持时间缓存，默认缓存有效期为 1 小时，可通过配置调整。

## 相关文件清单

### 核心文件
- `src/main/java/com/milo/reftime/RefTime.kt` - 核心接口
- `src/main/java/com/milo/reftime/RefTimeImpl.kt` - 接口实现
- `src/main/java/com/milo/reftime/config/RefTimeConfig.kt` - 配置类
- `src/main/java/com/milo/reftime/model/TimeTypes.kt` - 数据类型定义
- `src/main/java/com/milo/reftime/ext/RefTimeExtensions.kt` - 扩展函数

### 网络通信
- `src/main/java/com/milo/reftime/sntp/SntpClient.kt` - SNTP 客户端实现

### 内部组件
- `src/main/java/com/milo/reftime/internal/TimeKeeper.kt` - 时间管理组件

## 变更记录 (Changelog)

### 2025-08-28 16:52:33
- 创建模块级 CLAUDE.md 文档
- 记录核心 API 和架构信息
- 添加关键文件清单

---

*本文件由 Claude Code 自动生成，最后更新于 2025-08-28 16:52:33*