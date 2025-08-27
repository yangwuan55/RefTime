package com.milo.reftime

import kotlin.time.Duration
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

/** TrueTime扩展函数 - 完全基于kotlinx-datetime */

// ==================== 时间格式化扩展 ====================

/** 获取格式化的当前时间 */
suspend fun RefTime.formatNow(): String? {
  return nowOrNull()?.let { instant ->
    try {
      instant.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
    } catch (e: Exception) {
      "Invalid Time"
    }
  }
}

/** 获取ISO格式的当前时间 */
suspend fun RefTime.nowISO(): String? {
  return nowOrNull()?.let { instant ->
    try {
      instant.toString()
    } catch (e: Exception) {
      "Invalid Time"
    }
  }
}

/** 获取本地日期时间 */
suspend fun RefTime.nowLocalDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime? {
  return try {
    nowOrNull()?.toLocalDateTime(timeZone)
  } catch (e: Exception) {
    null
  }
}

/** 获取本地日期 */
suspend fun RefTime.nowLocalDate(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDate? {
  return try {
    nowOrNull()?.toLocalDateTime(timeZone)?.date
  } catch (e: Exception) {
    null
  }
}

/** 获取本地时间 */
suspend fun RefTime.nowLocalTime(
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
suspend fun RefTime.isBefore(other: RefTimeInstant): Boolean? {
  return nowOrNull()?.let { it < other }
}

/** 检查当前时间是否在指定时间之后 */
suspend fun RefTime.isAfter(other: RefTimeInstant): Boolean? {
  return nowOrNull()?.let { it > other }
}

/** 检查当前时间是否在指定时间范围内 */
suspend fun RefTime.isWithinRange(start: RefTimeInstant, end: RefTimeInstant): Boolean? {
  return nowOrNull()?.let { current -> current >= start && current <= end }
}

/** 检查当前时间是否比指定时间早超过指定持续时间 */
suspend fun RefTime.isOlderThan(duration: RefTimeDuration): Boolean? {
  return nowOrNull()?.let { current ->
    val threshold = Clock.System.now().minus(duration)
    current < threshold
  }
}

/** 检查当前时间是否比指定时间晚超过指定持续时间 */
suspend fun RefTime.isNewerThan(duration: RefTimeDuration): Boolean? {
  return nowOrNull()?.let { current ->
    val threshold = Clock.System.now().minus(duration)
    current > threshold
  }
}

// ==================== 持续时间计算扩展 ====================

/** 计算到指定时间的持续时间 */
suspend fun RefTime.durationUntil(target: RefTimeInstant): RefTimeDuration? {
  return nowOrNull()?.let { current -> target.minus(current) }
}

/** 计算距离现在多长时间前的时间点 */
suspend fun RefTime.timeAgo(duration: RefTimeDuration): RefTimeInstant? {
  return nowOrNull()?.minus(duration)
}

/** 计算距离现在多长时间后的时间点 */
suspend fun RefTime.timeFromNow(duration: RefTimeDuration): RefTimeInstant? {
  return nowOrNull()?.plus(duration)
}

// ==================== Flow 扩展 ====================

/** 状态流扩展 - 检查是否可用 */
val RefTime.isAvailable: Flow<Boolean>
  get() = state.map { it is RefTimeState.Available }

/** 状态流扩展 - 检查是否正在同步 */
val RefTime.isSyncing: Flow<Boolean>
  get() = state.map { it is RefTimeState.Syncing }

/** 状态流扩展 - 获取错误流 */
val RefTime.errors: Flow<RefTimeError>
  get() = state.filterIsInstance<RefTimeState.Failed>().map { it.error }

/** 状态流扩展 - 获取同步进度 */
val RefTime.syncProgress: Flow<Float>
  get() = state.filterIsInstance<RefTimeState.Syncing>().map { it.progress }

/** 时间更新流扩展 - 转换为本地日期时间 */
fun RefTime.timeUpdatesAsLocalDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Flow<LocalDateTime> = timeUpdates.map { it.toLocalDateTime(timeZone) }

/** 时间更新流扩展 - 转换为格式化字符串 */
fun RefTime.timeUpdatesAsFormatted(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Flow<String> = timeUpdates.map { instant -> instant.toLocalDateTime(timeZone).toString() }

// ==================== 便利性扩展 ====================

/** 安全获取时钟偏移的人类可读格式 */
suspend fun RefTime.getClockOffsetFormatted(): String {
  return getClockOffset()?.toHumanReadable() ?: "Unknown"
}

/** 安全获取准确度的人类可读格式 */
suspend fun RefTime.getAccuracyFormatted(): String {
  return getAccuracy()?.toHumanReadable() ?: "Unknown"
}

/** 获取状态描述 */
fun RefTime.getStateDescription(): Flow<String> =
    state.map { state ->
      when (state) {
        RefTimeState.Uninitialized -> "未初始化"
        is RefTimeState.Syncing -> "同步中 (${(state.progress * 100).toInt()}%)"
        is RefTimeState.Available -> "已同步 (偏移: ${state.clockOffset.toHumanReadable()})"
        is RefTimeState.Failed -> "同步失败: ${state.error.message}"
      }
    }

/** 检查缓存是否即将过期 */
suspend fun RefTime.isCacheExpiringSoon(
): Boolean {
  return when (val currentState = state.value) {
    is RefTimeState.Available -> {
      val elapsed = Clock.System.now().minus(currentState.lastSyncTime)
      elapsed > Duration.parse("PT50M") // 假设缓存有效期1小时，剩余10分钟算即将过期
    }
    else -> false
  }
}

// ==================== 调试扩展 ====================

/** 获取详细的调试信息流 */
fun RefTime.debugInfo(): Flow<String> =
    combine(state, timeUpdates.onStart { emit(Clock.System.now()) }) { state, currentTime ->
      buildString {
        appendLine("=== TrueTime Debug Info ===")
        appendLine("State: $state")
        appendLine("Current Time: $currentTime")
        when (state) {
          is RefTimeState.Available -> {
            appendLine("Clock Offset: ${state.clockOffset.toHumanReadable()}")
            appendLine("Last Sync: ${state.lastSyncTime}")
            appendLine("Accuracy: ${state.accuracy.toHumanReadable()}")
          }
          is RefTimeState.Failed -> {
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
fun RefTimeInstant.toHumanReadableAgo(): String {
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
