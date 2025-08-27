# TrueTime Android 时间系统重构设计

## 概述

将 TrueTime Android 项目中现有的混合时间API（`java.util.Date` + `java.time`）统一重构为使用 Kotlin 原生的 `kotlin.time` 和现代化的时间处理方案。重构目标是简化API、提升性能、增强类型安全性，同时保持与现有Legacy API的兼容性。

## 技术栈与环境要求

### 项目环境配置
- **JDK版本**: JDK 17 (已配置)
- **Gradle版本**: 8.5 (当前版本)
- **Kotlin JVM目标**: 17 (已配置)
- **编译SDK**: 34 (需要更新)
- **UI框架**: Jetpack Compose + Material3

### 核心依赖
- **kotlin.time**: Kotlin 标准库的时间API
- **kotlinx-coroutines**: 现有异步编程支持
- **kotlin-stdlib**: Kotlin 标准库
- **androidx.compose**: UI层现代化支持

### 重构策略
- Legacy API 中的 `java.util.Date` → `kotlin.time` + 适配器
- Modern API 中的 `java.time` → 统一的 `kotlin.time` API
- 保持现有 StateFlow 和 Flow 响应式架构
- 简化依赖管理，移除复杂的依赖注入

## 架构设计

### 核心重构思路

``mermaid
graph TB
    A[Legacy TrueTime API] --> B[适配器层]
    C[Modern TrueTime API] --> D[KotlinTime Implementation]
    
    B --> D
    D --> E[TimeKeeper]
    D --> F[SNTP Client]
    
    E --> G[kotlin.time.TimeMark]
    F --> H[kotlin.time.Duration]
    
    I[Compose UI] --> C
    J[Legacy 应用] --> A
```

### 模块设计原则

| 层级 | 职责 | 实现类型 | 技术选型 |
|------|------|----------|----------|
| API 层 | 统一接口 | `ModernTrueTime` | kotlin.time API |
| 适配层 | 兼容性 | `LegacyTrueTimeAdapter` | java.util.Date ↔ kotlin.time |
| 实现层 | 核心逻辑 | `KotlinTimeKeeper` | 无依赖注入 |
| 存储层 | 缓存管理 | `BasicCacheProvider` | 简化实现 |
| 网络层 | NTP 通信 | `SntpImpl` | 保持现有 |

### 设计约束

1. **简化依赖**：不引入额外时间库，优先使用 kotlin.time
2. **兼容性**：保持 Legacy API 不变，通过适配器实现
3. **性能优先**：减少类型转换开销，直接使用 kotlin.time
4. **Compose 集成**：为 UI 层提供现代化的 API

## 详细设计

### 1. 核心接口重新设计

#### 现代化 TrueTime 接口
``kotlin
interface ModernTrueTime {
    val state: StateFlow<TrueTimeState>
    val timeUpdates: Flow<KotlinTimeMark>
    
    suspend fun now(): KotlinTimeMark
    suspend fun nowOrNull(): KotlinTimeMark?
    suspend fun nowSafe(): KotlinTimeMark
    
    suspend fun sync(): Result<Unit>
    fun cancel()
    
    suspend fun durationSince(since: KotlinTimeMark): kotlin.time.Duration?
    suspend fun nowMillis(): Long?
}
```

#### 状态模型优化
``kotlin
sealed interface TrueTimeState {
    object Uninitialized : TrueTimeState
    data class Available(val offset: kotlin.time.Duration) : TrueTimeState
    data class Syncing(val progress: Float = 0f) : TrueTimeState
    data class Failed(val error: Throwable) : TrueTimeState
}
```

#### 时间标记类型
``kotlin
@JvmInline
value class KotlinTimeMark(
    private val epochMillis: Long
) {
    fun toEpochMillis(): Long = epochMillis
    
    operator fun plus(duration: kotlin.time.Duration): KotlinTimeMark {
        return KotlinTimeMark(epochMillis + duration.inWholeMilliseconds)
    }
    
    operator fun minus(other: KotlinTimeMark): kotlin.time.Duration {
        return (epochMillis - other.epochMillis).milliseconds
    }
    
    // Legacy 兼容性
    fun toDate(): Date = Date(epochMillis)
    fun toInstant(): java.time.Instant = java.time.Instant.ofEpochMilli(epochMillis)
    
    companion object {
        fun now(): KotlinTimeMark = KotlinTimeMark(System.currentTimeMillis())
        fun fromDate(date: Date): KotlinTimeMark = KotlinTimeMark(date.time)
    }
}
```

### 2. 实现层设计

#### KotlinTimeKeeper 重构
``kotlin
internal class KotlinTimeKeeper {
    private var cachedTimeMark: KotlinTimeMark? = null
    private var clockOffset: kotlin.time.Duration = kotlin.time.Duration.ZERO
    private var cacheTimestamp: Long = 0L
    private val cacheValidDuration = 1.hours
    
    fun hasAccurateTime(): Boolean {
        val cached = cachedTimeMark ?: return false
        val elapsed = (System.currentTimeMillis() - cacheTimestamp).milliseconds
        return elapsed < cacheValidDuration
    }
    
    fun getCurrentTime(): KotlinTimeMark? {
        if (!hasAccurateTime()) return null
        
        val cached = cachedTimeMark ?: return null
        val systemElapsed = (System.currentTimeMillis() - cacheTimestamp).milliseconds
        
        return cached + systemElapsed + clockOffset
    }
    
    fun saveResult(timeMark: KotlinTimeMark, offset: kotlin.time.Duration) {
        cachedTimeMark = timeMark
        clockOffset = offset
        cacheTimestamp = System.currentTimeMillis()
    }
}
```

### 3. 配置系统现代化

#### 配置 DSL
``kotlin
@DslMarker
annotation class TrueTimeDsl

@TrueTimeDsl
class TrueTimeConfig internal constructor() {
    var ntpHosts: List<String> = listOf("time.google.com", "time.apple.com")
    var connectionTimeout: kotlin.time.Duration = 30.seconds
    var maxRetries: Int = 3
    var syncInterval: kotlin.time.Duration = 1.hours
    var debug: Boolean = false
    
    fun ntpHosts(vararg hosts: String) {
        this.ntpHosts = hosts.toList()
    }
}

fun createModernTrueTime(config: TrueTimeConfig.() -> Unit = {}): ModernTrueTime {
    val configuration = TrueTimeConfig().apply(config)
    return KotlinTimeTrueTime(configuration)
}
```

### 4. 兼容性桥接

#### Legacy API 适配器
``kotlin
class LegacyTrueTimeAdapter(
    private val modernTrueTime: ModernTrueTime
) : com.instacart.truetime.time.TrueTime {
    
    override fun sync(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            modernTrueTime.sync()
        }
    }
    
    override fun now(): Date {
        return runBlocking {
            modernTrueTime.now().toDate()
        }
    }
    
    override fun nowSafely(): Date {
        return runBlocking {
            modernTrueTime.nowSafe().toDate()
        }
    }
    
    override fun hasTheTime(): Boolean {
        return runBlocking {
            modernTrueTime.nowOrNull() != null
        }
    }
}
```

### 5. Compose UI 集成

#### Compose 扩展函数
``kotlin
@Composable
fun rememberTrueTimeState(trueTime: ModernTrueTime): State<TrueTimeState> {
    return trueTime.state.collectAsState()
}

fun KotlinTimeMark.formatForDisplay(
    pattern: String = "yyyy-MM-dd HH:mm:ss"
): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(toDate())
}
```

### 6. 扩展函数库

#### 时间比较和计算
``kotlin
suspend fun ModernTrueTime.isBefore(other: KotlinTimeMark): Boolean {
    return nowOrNull()?.let { it.toEpochMillis() < other.toEpochMillis() } ?: true
}

suspend fun ModernTrueTime.timeSince(since: KotlinTimeMark): kotlin.time.Duration? {
    return nowOrNull()?.let { it - since }
}

suspend fun ModernTrueTime.debugInfo(): String = buildString {
    append("ModernTrueTime: ")
    when (val currentState = state.value) {
        TrueTimeState.Uninitialized -> append("Uninitialized")
        is TrueTimeState.Available -> {
            append("Available (offset: ${currentState.offset})")
            nowOrNull()?.let { 
                append(", current: ${it.formatForDisplay()}")
            }
        }
        is TrueTimeState.Syncing -> append("Syncing (${currentState.progress * 100}%)")
        is TrueTimeState.Failed -> append("Failed: ${currentState.error.message}")
    }
}
```

## 数据流设计

### 时间同步流程
``mermaid
sequenceDiagram
    participant App as 应用
    participant MT as ModernTrueTime
    participant SNTP as SNTP客户端
    participant Server as NTP服务器
    participant TK as KotlinTimeKeeper
    
    App->>MT: sync()
    MT->>SNTP: requestTime()
    SNTP->>Server: NTP请求
    Server-->>SNTP: NTP响应
    SNTP-->>MT: SntpResult
    MT->>TK: saveResult()
    TK-->>MT: 缓存成功
    MT-->>App: Result.success
    
    App->>MT: now()
    MT->>TK: getCurrentTime()
    TK-->>MT: KotlinTimeMark
    MT-->>App: 准确时间
```

### 状态流转
``mermaid
stateDiagram-v2
    [*] --> Uninitialized
    Uninitialized --> Syncing: sync()
    Syncing --> Available: 同步成功
    Syncing --> Failed: 同步失败
    Available --> Syncing: 重新同步
    Failed --> Syncing: 重试
    Available --> [*]: cancel()
    Failed --> [*]: cancel()
```

## 迁移策略

### 阶段性迁移计划

| 阶段 | 内容 | 交付物 | 风险级别 |
|------|------|--------|----------|
| 阶段1 | 创建新接口和基础类型 | ModernTrueTime接口, KotlinTimeMark | 低 |
| 阶段2 | 实现核心逻辑 | KotlinTimeTrueTime, KotlinTimeKeeper | 中 |
| 阶段3 | 实现适配器层 | LegacyTrueTimeAdapter | 低 |
| 阶段4 | Compose集成和扩展 | Compose扩展函数 | 低 |
| 阶段5 | 测试和验证 | 单元测试, 集成测试 | 中 |

### 兼容性保证

1. **Legacy API**：通过适配器完全保持现有接口
2. **Modern API**：提供 Java 8 时间API的适配
3. **渐进迁移**：新功能使用新API，现有代码保持不变
4. **测试覆盖**：确保所有API变更都有对应测试

## 单元测试策略

### 测试覆盖范围

| 组件 | 测试类型 | 关键测试点 |
|------|----------|------------|
| KotlinTimeMark | 单元测试 | 时间运算、类型转换 |
| ModernTrueTime | 单元测试 | 状态管理、时间获取 |
| KotlinTimeKeeper | 单元测试 | 缓存逻辑、时间计算 |
| 适配器层 | 单元测试 | API兼容性、类型转换 |
| 扩展函数 | 单元测试 | 边界条件、异常处理 |

### 测试工具
- **kotlinx-coroutines-test**: 协程测试
- **MockK**: Kotlin模拟框架  
- **Turbine**: Flow测试
- **JUnit 5**: 测试框架
