# TrueTime for Android - Kotlin DateTime Edition

![TrueTime](truetime.png "TrueTime for Android")

**A completely modernized, kotlinx-datetime-first NTP client for Android.** 

Provides accurate network time through reactive programming patterns and type-safe APIs using `kotlinx-datetime`, `kotlin.time`, `Flow`, and `Coroutines`.

*This is a major version rewrite that completely removes `java.util.Date` and `java.time` dependencies in favor of kotlinx-datetime.*

----------------------------------------

*Make sure to check out our counterpart too: [TrueTime](https://github.com/instacart/TrueTime.swift), an NTP library for Swift.*

# What is TrueTime?

TrueTime is a **kotlinx-datetime-first** NTP client for Android. It helps you calculate the real "now" using **network time servers**, unaffected by:
- Manual device clock changes
- User timezone adjustments  
- Incorrect system time settings
- Wake-up clock drift

## 🎯 Complete Modernization

This version has been **completely rewritten** using modern Kotlin technologies:

- **✨ kotlinx-datetime**: Pure Kotlin time API, replaces all `java.util.Date` and `java.time`
- **⏱️ kotlin.time**: Native Kotlin duration handling with `Duration`
- **🌊 Coroutines & Flow**: Fully async with reactive state management
- **🏗️ Jetpack Compose**: Modern UI examples with Material3
- **🛡️ Type Safety**: Sealed classes for errors and states
- **🔧 DSL Configuration**: Kotlin DSL for fluent, type-safe configuration
- **🧪 Testing**: Built-in test utilities with `TestTrueTime`

**⚠️ Breaking Changes:** This is a major version that removes all legacy APIs. All time types now use `kotlinx.datetime.Instant` instead of `Date` or `java.time.Instant`.

## Why do I need TrueTime?

In certain applications it becomes important to get the real or "true" date and time. On most
devices, if the clock has been changed manually, then a `Date()` instance gives you a time impacted
by local settings.

Users may do this for a variety of reasons, like being in different timezones, trying to be punctual
by setting their clocks 5 – 10 minutes early, etc. Your application or service may want a date that
is unaffected by these changes and reliable as a source of truth. TrueTime gives you that.

You can read more about the use case in
our [intro blog post](https://tech.instacart.com/offline-first-introducing-truetime-for-swift-and-android-15e5d968df96)
.

# How does TrueTime work?

In a [conference talk](https://vimeo.com/190922794), we explained how the full NTP implementation
works. Check the [video](https://vimeo.com/190922794#t=1466s)
and [slides](https://speakerdeck.com/kaushikgopal/learning-rx-by-example-2?slide=31) out for
implementation details.

TrueTime has since been migrated to Kotlin & Coroutines and no longer requires the additional Rx
dependency. The concept hasn't changed but the above video is still a good explainer on the concept.

# Usage

## 🎆 Modern kotlinx-datetime API 

### Installation
We use [JitPack](https://jitpack.io) to host the library.

[![](https://jitpack.io/v/instacart/truetime-android.svg)](https://jitpack.io/#instacart/truetime-android)

Add this to your application's `build.gradle` file:

```groovy
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation 'com.github.instacart:truetime-android:<release-version>'
}
```

### Basic Setup
```kotlin
class App : Application() {
    val trueTime = TrueTime {
        ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org")
        timeout(Duration.parse("PT30S"))  // kotlin.time.Duration
        retries(3)
        debug(BuildConfig.DEBUG)
    }
    
    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            trueTime.sync().onSuccess {
                println("✅ TrueTime synced")
            }.onFailure { error ->
                println("❌ Sync failed: ${error.message}")
            }
        }
    }
}
```

### Reactive Usage with Flow
No more polling! Use reactive patterns:

```kotlin
// Observe state changes
trueTime.state.collect { state ->
    when (state) {
        TrueTimeState.Uninitialized -> showLoading()
        is TrueTimeState.Available -> showAccurateTime()
        is TrueTimeState.Failed -> showError(state.error)
        else -> {}
    }
}

// Get time updates reactively
trueTime.timeUpdates.collectLatest { instant ->
    updateTime(instant)
}
```

### Time Access Methods
```kotlin
// All methods return kotlinx.datetime.Instant
val accurateTime = trueTime.now()         // kotlinx.datetime.Instant
val safeTime = trueTime.nowSafe()         // Falls back to system time
val optionalTime = trueTime.nowOrNull()   // Null if not synced

// kotlin.time.Duration calculations
val offset = trueTime.getClockOffset()    // kotlin.time.Duration
val timeSince = trueTime.durationSince(someInstant)

// Extension utilities with kotlinx-datetime
val isoTime = trueTime.nowISO()           // ISO 8601 string
val formatted = trueTime.formatNow()      // Formatted with kotlinx-datetime
val localTime = trueTime.nowLocalDateTime() // LocalDateTime
```

## 📱 Jetpack Compose Example
```kotlin
@Composable
fun TrueTimeDisplay(trueTime: TrueTime) {
    val state by trueTime.state.collectAsStateWithLifecycle()
    val currentTime by trueTime.timeUpdates
        .collectAsStateWithLifecycle(initialValue = null)
    
    when (val current = state) {
        is TrueTimeState.Available -> 
            Text("Time: ${currentTime?.formatForDisplay()}")
        is TrueTimeState.Failed -> 
            Text("Error: ${current.error.message}")
        is TrueTimeState.Syncing -> 
            CircularProgressIndicator()
        else -> Text("Initializing...")
    }
}
```

## 🛠️ Configuration Options

| Parameter | Description | Default |
|-----------|-------------|---------|
| `ntpHosts` | NTP server hosts | `["time.google.com"]` |
| `connectionTimeout` | Network timeout | `30.seconds` |
| `maxRetries` | Retry attempts | `3` |
| `baseRetryDelay` | Initial retry delay | `1.seconds` |
| `maxRetryDelay` | Maximum retry delay | `30.seconds` |
| `debug` | Debug logging | `false` |

## 🚀 Quick Start

```kotlin
// Modern kotlinx-datetime API
val trueTime = TrueTime {
    ntpHosts("time.google.com", "time.apple.com")
    timeout(Duration.parse("PT15S"))
}

// In coroutines
lifecycleScope.launch {
    trueTime.sync().onSuccess {
        val accurateTime = trueTime.now()  // kotlinx.datetime.Instant
        Log.d("TrueTime", "Network time: $accurateTime")
    }
}
```

## ✅ Feature Summary

### Modern API Features
- **🎯 kotlinx-datetime**: Pure Kotlin time API, no Java dependencies
- **⏱️ kotlin.time**: Native duration handling with `Duration`
- **🔄 Reactive**: StateFlow and Flow for reactive programming
- **🛡️ Type Safety**: Sealed classes for errors and states
- **🧪 Testing**: Built-in TestTrueTime for mocking
- **⚡ Coroutines**: Suspended functions throughout
- **🔧 DSL**: Kotlin DSL for type-safe configuration
- **🎨 Extensions**: Rich kotlinx-datetime utilities

## 📦 Migration from Legacy API

This version completely removes the old `java.util.Date` and `java.time` based APIs. All time handling now uses `kotlinx-datetime`.

**Key Changes:**
- `java.util.Date` → `kotlinx.datetime.Instant`
- `java.time.Duration` → `kotlin.time.Duration`
- Blocking calls → `suspend` functions
- Polling → `Flow` reactivity
- Builders → Kotlin DSL

**📖 See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for detailed migration instructions.**

## 📝 License

Apache 2.0. See [LICENSE](LICENSE) file.

# License

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
