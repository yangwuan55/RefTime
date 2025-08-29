# TrueTime kotlinx-datetime Migration Guide

## Overview

This is a **complete rewrite** of TrueTime using modern Kotlin technologies. The API has been **completely redesigned** around `kotlinx-datetime`, `kotlin.time`, `Flow`, and `Coroutines`.

**⚠️ This is a BREAKING CHANGE release. All previous APIs have been removed.**

## 🎆 What's New

### Complete Technology Stack Modernization

| Old Technology | New Technology | Benefits |
|---|---|---|
| `java.util.Date` | `kotlinx.datetime.Instant` | Type-safe, multiplatform, immutable |
| `java.time.*` | `kotlin.time.Duration` | Native Kotlin, concise syntax |
| Blocking calls | `suspend` functions | Non-blocking, efficient |
| Polling loops | `StateFlow`/`Flow` | Reactive, declarative |
| Builders | Kotlin DSL | Type-safe, readable |
| Exception handling | Sealed classes | Exhaustive, safe |

### New Features
- ✨ **100% kotlinx-datetime**: No Java time dependencies
- 🌊 **Reactive State**: Real-time sync status via Flow
- 🛡️ **Type Safety**: Sealed error classes
- 🔧 **DSL Configuration**: Fluent, type-safe setup
- 🧪 **Built-in Testing**: `TestRefTime` for mocking
- 📱 **Compose Integration**: Ready for modern Android UI

## 🔄 Before and After Migration

### 1. Basic Setup

#### Before (Old API - REMOVED)
```kotlin
// This API no longer exists
class App : Application() {
    val refTime = RefTimeImpl()
    
    override fun onCreate() {
        super.onCreate()
        trueTime.sync() // Returns Job
    }
}

// Polling for time availability
lifecycleScope.launch {
    while (!trueTime.hasTheTime()) {
        delay(1000)
    }
    val time = trueTime.now() // java.util.Date
}
```

#### After (kotlinx-datetime API)
```kotlin
class App : Application() {
    val refTime = RefTime {
        ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org")
        timeout(Duration.parse("PT30S"))  // kotlin.time.Duration
        retries(3)
        debug(BuildConfig.DEBUG)
    }
    
    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            trueTime.sync().onSuccess {
                println("✅ Sync completed")
            }.onFailure { error ->
                println("❌ Sync failed: ${error.message}")
            }
        }
    }
}

// Reactive state observation
lifecycleScope.launch {
    trueTime.state.collect { state ->
        when (state) {
            is RefTimeState.Available -> {
                val time = trueTime.now() // kotlinx.datetime.Instant
                // Use time...
            }
            is RefTimeState.Failed -> {
                // Handle error...
            }
            else -> { /* Loading states */ }
        }
    }
}
```

#### Modern:
```kotlin
// Reactive approach
lifecycleScope.launch {
    trueTime.state.collect { state ->
        when (state) {
            RefTimeState.Uninitialized -> showLoading()
            is RefTimeState.Syncing -> updateProgress(state.progress)
            is RefTimeState.Available -> showTime()
            is RefTimeState.Failed -> showError(state.error)
        }
    }
}

// Or use Flow for continuous updates
lifecycleScope.launch {
    trueTime.timeUpdates.collectLatest { instant ->
        updateTime(instant)
    }
}
```

### 3. Time Access

#### Legacy:
```kotlin
val time = trueTime.now() // Uses java.util.Date
val isAvailable = trueTime.hasTheTime()

// Safety handling
try {
    val accurateTime = trueTime.nowTrueOnly()
} catch (e: IllegalStateException) {
    // Handle not synchronized
}

val timeOrNull = if (trueTime.hasTheTime()) trueTime.now() else null
```

#### Modern:
```kotlin
val time = trueTime.now() // Returns java.time.Instant with suspend
val isAvailable = trueTime.hasSynced() // Extension function

// Proper error handling
val accurateTime = trueTime.nowOrNull() // Kotlin coroutines style

// Safe fallback
val time = trueTime.nowSafe() // Always returns time

// Type-safe duration handling
val offset = trueTime.durationSince(Instant.now())
```

## Feature-by-Feature Migration

### 4. Configuration Migration

#### Legacy Parameters
```kotlin
val params = RefTimeParameters.Builder()
    .ntpHostPool(arrayListOf("time.apple.com"))
    .connectionTimeout(30000) // milliseconds
    .buildParams()
val refTime = RefTimeImpl(params)
```

#### Modern Configuration
```kotlin
val refTime = RefTime {
    ntpHosts("time.apple.com") // Variadic args
    connectionTimeout = 30.seconds // Kotlin Duration
    maxRetries = 3
    baseRetryDelay = 1.seconds
    maxRetryDelay = 30.seconds
}
```

### 5. Error Handling Migration

#### Legacy Exception Handling
```kotlin
try {
    val time = trueTime.nowTrueOnly()
} catch (e: IllegalStateException) {
    when (e.message) {
        "RefTime not initialized" -> showError()
        // ... other cases
    }
}
```

#### Modern Result Handling
```kotlin
val result = trueTime.nowOrNull()
result?.let { 
    updateTime(it) 
} ?: run { 
    showError("Could not get accurate time") 
}

// Or with custom error types
when (val state = trueTime.state.value) {
    is RefTimeState.Failed -> handleError(state.error)
    // ... other states
}
```

## Advanced Migration Patterns

### 6. Flow Integration in Jetpack Compose

```kotlin
@Composable
fun ModernTimeDisplay(refTime: RefTime) {
    val state by trueTime.state.collectAsState() // Observe state changes
    
    when (val current = state) {
        RefTimeState.Uninitialized -> LoadingView()
        is RefTimeState.Syncing -> ProgressBar(current.progress)
        is RefTimeState.Available -> TimeDisplay(scapegoat = current)
        is RefTimeState.Failed -> ErrorView(current.error)
    }
}

@Composable
fun ContinuousTimeDisplay(refTime: RefTime) {
    val trueNow by trueTime.timeUpdates
        .collectAsStateWithLifecycle("")
    
    Text(formatInstant(trueNow))
}
```

### 7. Testing Migration

#### Legacy Testing
```kotlin
class RefTimeTest {
    private lateinit var refTime: RefTimeImpl
    
    @Before
    fun setup() {
        refTime = RefTimeImpl()
        // Complex mocking required
    }
}
```

#### Modern Testing
```kotlin
class RefTimeTest {
    private val testTime = Instant.parse("2024-01-15T12:00:00Z")
    private val mockRefTime = TestRefTime(testTime)
    
    @Test
    fun testModernAPI() = runTest {
        assertEquals(testTime, mockRefTime.now())
    }
}
```

### 8. Extension Functions Usage

```kotlin
// Modern extensions provide more utility
val refTime = RefTime { ntpHosts("time.google.com") }

// Type-safe formatting
lifecycleScope.launch {
    val isoTime = trueTime.nowISO8601()
    val formatted = trueTime.formatNow("MMM dd, yyyy")
    val zonedDate = trueTime.nowZonedDateTime()
    
    // Duration calculations are type-safe
    val offset = trueTime.durationSince(Instant.now())
}

// Easy validation
lifecycleScope.launch {
    val isRecent = trueTime.isOlderThan(Duration.ofHours(1))
    val isValidRange = trueTime.isWithinRange(start, end)
}
```

## Configuration Reference

| Legacy Parameter | Modern Equivalent |
|---|---|
| `ntpHostPool(ArrayList<String>)` | `ntpHosts(String...)` |
| `connectionTimeout(Int)` | `connectionTimeout = Duration` |
| `syncMaxRetries(Int)` | `maxRetries = Int` |
| `retryDelay(Long)` | `baseRetryDelay = Duration` |
| `maxRetryDelay(Long)` | `maxRetryDelay = Duration` |
| N/A | `debug = Boolean` |

## Common Migration Scenarios

### Migration Example 1: Simple Usage
```kotlin
// Before
val refTime = RefTimeImpl()
trueTime.sync()
val time = trueTime.now()

// After
val refTime = RefTime { ntpHosts("time.google.com") }
aval job = lifecycleScope.launch {
    trueTime.sync().onSuccess { 
        val time = trueTime.now() 
    }
}
```

### Migration Example 2: With Fallback
```kotlin
// Before
try {
    val accurateTime = trueTime.nowTrueOnly()
} catch (e: IllegalStateException) {
    val systemTime = Date()
}

// After
val safeTime = trueTime.nowSafe() // Always returns time
```

### Migration Example 3: Continuous Updates
```kotlin
// Before - Polling
lifecycleScope.launch {
    while (!trueTime.hasTheTime()) {
        delay(1000)
    }
    // Do something
}

// After - Reactive
lifecycleScope.launch {
    trueTime.state
        .filterIsInstance<RefTimeState.Available>()
        .collect { /* React to state */ }
}
```

## Things That Have Changed

### ✅ Added Features
- Kotlin DSL configuration
- Flow-based reactive state
- Type-safe Java 8 time API
- Suspendable functions
- Proper error handling with sealed classes
- Extension utilities
- Testing support
- Composable-first design

### ⚠️ Breaking Changes
- Constructor-based initialization → DSL configuration  
- Date → Instant return types
- Exception handling → sealed class states
- Polling → reactive flows
- Builders → DSL functions

### 🔁 Java Interoperability
A Java-friendly API wrapper is not provided in this modernization. Legacy `RefTimeImpl` can be used for Java projects.

## Migration Checklist

- [ ] Replace `RefTimeImpl()` with `RefTime { ... }` DSL
- [ ] Update all `now()` calls to handle suspend
- [ ] Replace polling loops with flow collection
- [ ] Update error handling to use sealed classes
- [ ] Migrate Date → Instant type usage
- [ ] Update tests to use `TestRefTime`
- [ ] Update configuration builders to DSL syntax
- [ ] Add Flow collection where appropriate (Compose or ViewModel)

## Backwards Compatibility

The legacy API is **still available** for migration periods. You can mix both APIs during transition:
```kotlin
// Migrate gradually
val legacyTime = legacyRefTime.now()
val modernTime = modernRefTime.now()
```

The modern API is designed to be **drop-in compatible** with most use cases while providing much better Kotlin developer experience.