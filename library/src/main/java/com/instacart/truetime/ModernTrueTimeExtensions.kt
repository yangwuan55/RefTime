package com.instacart.truetime

import kotlin.time.Duration
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

/** TrueTime扩展函数 - 完全基于kotlinx-datetime */

// ==================== 时间格式化扩展 ====================

/** 获取格式化的当前时间 */
suspend fun TrueTime.formatNow(): String? {
  return nowOrNull()?.let { instant ->
    try {
      instant.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
    } catch (e: Exception) {
      "Invalid Time"
    }
  }
}

/** 获取ISO格式的当前时间 */
suspend fun TrueTime.nowISO(): String? {
  return nowOrNull()?.let { instant ->
    try {
      instant.toString()
    } catch (e: Exception) {
      "Invalid Time"
    }
  }
}

/** 获取本地日期时间 */
suspend fun TrueTime.nowLocalDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime? {
  return try {
    nowOrNull()?.toLocalDateTime(timeZone)
  } catch (e: Exception) {
    null
  }
}

/** 获取本地日期 */
suspend fun TrueTime.nowLocalDate(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDate? {
  return try {
    nowOrNull()?.toLocalDateTime(timeZone)?.date
  } catch (e: Exception) {
    null
  }
}

/** 获取本地时间 */
suspend fun TrueTime.nowLocalTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalTime? {
  return try {
    nowOrNull()?.toLocalDateTime(timeZone)?.time
  } catch (e: Exception) {
    null
  }
}

// ==================== 时间比较扩展 ====================

/** 检查当前时间是否在指定时间之前 */
suspend fun TrueTime.isBefore(other: TrueTimeInstant): Boolean? {
  return nowOrNull()?.let { it < other }
}

/** 检查当前时间是否在指定时间之后 */
suspend fun TrueTime.isAfter(other: TrueTimeInstant): Boolean? {
  return nowOrNull()?.let { it > other }
}

/** 检查当前时间是否在指定时间范围内 */
suspend fun TrueTime.isWithinRange(start: TrueTimeInstant, end: TrueTimeInstant): Boolean? {
  return nowOrNull()?.let { current -> current >= start && current <= end }
}

/** 检查当前时间是否比指定时间早超过指定持续时间 */
suspend fun TrueTime.isOlderThan(duration: TrueTimeDuration): Boolean? {
  return nowOrNull()?.let { current ->
    val threshold = Clock.System.now().minus(duration)
    current < threshold
  }
}

/** 检查当前时间是否比指定时间晚超过指定持续时间 */
suspend fun TrueTime.isNewerThan(duration: TrueTimeDuration): Boolean? {
  return nowOrNull()?.let { current ->
    val threshold = Clock.System.now().minus(duration)
    current > threshold
  }
}

// ==================== 持续时间计算扩展 ====================

/** 计算到指定时间的持续时间 */
suspend fun TrueTime.durationUntil(target: TrueTimeInstant): TrueTimeDuration? {
  return nowOrNull()?.let { current -> target.minus(current) }
}

/** 计算距离现在多长时间前的时间点 */
suspend fun TrueTime.timeAgo(duration: TrueTimeDuration): TrueTimeInstant? {
  return nowOrNull()?.minus(duration)
}

/** 计算距离现在多长时间后的时间点 */
suspend fun TrueTime.timeFromNow(duration: TrueTimeDuration): TrueTimeInstant? {
  return nowOrNull()?.plus(duration)
}

// ==================== Flow 扩展 ====================

/** 状态流扩展 - 检查是否可用 */
val TrueTime.isAvailable: Flow<Boolean>
  get() = state.map { it is TrueTimeState.Available }

/** 状态流扩展 - 检查是否正在同步 */
val TrueTime.isSyncing: Flow<Boolean>
  get() = state.map { it is TrueTimeState.Syncing }

/** 状态流扩展 - 获取错误流 */
val TrueTime.errors: Flow<TrueTimeError>
  get() = state.filterIsInstance<TrueTimeState.Failed>().map { it.error }

/** 状态流扩展 - 获取同步进度 */
val TrueTime.syncProgress: Flow<Float>
  get() = state.filterIsInstance<TrueTimeState.Syncing>().map { it.progress }

/** 时间更新流扩展 - 转换为本地日期时间 */
fun TrueTime.timeUpdatesAsLocalDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Flow<LocalDateTime> = timeUpdates.map { it.toLocalDateTime(timeZone) }

/** 时间更新流扩展 - 转换为格式化字符串 */
fun TrueTime.timeUpdatesAsFormatted(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Flow<String> = timeUpdates.map { instant -> instant.toLocalDateTime(timeZone).toString() }

// ==================== 便利性扩展 ====================

/** 安全获取时钟偏移的人类可读格式 */
suspend fun TrueTime.getClockOffsetFormatted(): String {
  return getClockOffset()?.toHumanReadable() ?: "Unknown"
}

/** 安全获取准确度的人类可读格式 */
suspend fun TrueTime.getAccuracyFormatted(): String {
  return getAccuracy()?.toHumanReadable() ?: "Unknown"
}

/** 获取状态描述 */
fun TrueTime.getStateDescription(): Flow<String> =
    state.map { state ->
      when (state) {
        TrueTimeState.Uninitialized -> "未初始化"
        is TrueTimeState.Syncing -> "同步中 (${(state.progress * 100).toInt()}%)"
        is TrueTimeState.Available -> "已同步 (偏移: ${state.clockOffset.toHumanReadable()})"
        is TrueTimeState.Failed -> "同步失败: ${state.error.message}"
      }
    }

/** 检查缓存是否即将过期 */
suspend fun TrueTime.isCacheExpiringSoon(
): Boolean {
  return when (val currentState = state.value) {
    is TrueTimeState.Available -> {
      val elapsed = Clock.System.now().minus(currentState.lastSyncTime)
      elapsed > Duration.parse("PT50M") // 假设缓存有效期1小时，剩余10分钟算即将过期
    }
    else -> false
  }
}

// ==================== 调试扩展 ====================

/** 获取详细的调试信息流 */
fun TrueTime.debugInfo(): Flow<String> =
    combine(state, timeUpdates.onStart { emit(Clock.System.now()) }) { state, currentTime ->
      buildString {
        appendLine("=== TrueTime Debug Info ===")
        appendLine("State: $state")
        appendLine("Current Time: $currentTime")
        when (state) {
          is TrueTimeState.Available -> {
            appendLine("Clock Offset: ${state.clockOffset.toHumanReadable()}")
            appendLine("Last Sync: ${state.lastSyncTime}")
            appendLine("Accuracy: ${state.accuracy.toHumanReadable()}")
          }
          is TrueTimeState.Failed -> {
            appendLine("Error: ${state.error}")
          }
          else -> {
            /* No additional info */
          }
        }
        appendLine("=== End Debug Info ===")
      }
    }

// ==================== Instant 扩展函数 ====================

/** Instant 到人类可读时间距离的转换 */
fun TrueTimeInstant.toHumanReadableAgo(): String {
  val now = Clock.System.now()
  val duration = now.minus(this)

  return when {
    duration < Duration.parse("PT1M") -> "刚刚"
    duration < Duration.parse("PT1H") -> "${duration.inWholeMinutes}分钟前"
    duration < Duration.parse("P1D") -> "${duration.inWholeHours}小时前"
    duration < Duration.parse("P7D") -> "${duration.inWholeDays}天前"
    else -> {
      val localDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date
      "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
    }
  }
}
