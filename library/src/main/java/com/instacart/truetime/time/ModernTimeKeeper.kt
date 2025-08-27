package com.instacart.truetime.time

import com.instacart.truetime.*
import kotlin.time.Duration
import kotlinx.datetime.Clock

/** 现代化时间管理器 - 完全基于kotlinx-datetime */
internal class ModernTimeKeeper {

  // 缓存的网络时间
  private var cachedNetworkTime: TrueTimeInstant? = null

  // 时钟偏移量
  private var clockOffset: TrueTimeDuration = Duration.ZERO

  // 准确度
  private var accuracy: TrueTimeDuration = Duration.ZERO

  // 最后同步时的系统时间
  private var lastSyncSystemTime: TrueTimeInstant? = null

  // 缓存有效期 (默认1小时)
  private val cacheValidDuration: TrueTimeDuration = Duration.parse("PT1H")

  /** 保存同步结果 */
  fun saveSync(result: SntpResult) {
    cachedNetworkTime = result.networkTime
    clockOffset = result.clockOffset
    accuracy = result.accuracy
    lastSyncSystemTime = Clock.System.now()
  }

  /**
   * 获取当前准确时间
   *
   * @return 当前准确时间，如果缓存无效则返回null
   */
  fun getCurrentTime(): TrueTimeInstant? {
    if (!hasValidCache()) return null

    val cachedTime = cachedNetworkTime ?: return null
    val lastSync = lastSyncSystemTime ?: return null

    // 计算自上次同步以来经过的时间
    val currentSystemTime = Clock.System.now()
    val systemElapsed = currentSystemTime.minus(lastSync)

    // 返回修正后的网络时间
    return cachedTime.plus(systemElapsed)
  }

  /** 检查是否有有效的缓存 */
  fun hasValidCache(): Boolean {
    val lastSync = lastSyncSystemTime ?: return false
    val cachedTime = cachedNetworkTime ?: return false

    val currentSystemTime = Clock.System.now()
    val elapsed = currentSystemTime.minus(lastSync)

    return elapsed < cacheValidDuration
  }

  /** 获取时钟偏移量 */
  fun getClockOffset(): TrueTimeDuration = clockOffset

  /** 获取准确度 */
  fun getAccuracy(): TrueTimeDuration = accuracy

  /** 获取最后同步时间 */
  fun getLastSyncTime(): TrueTimeInstant? = cachedNetworkTime

  /** 清除缓存 */
  fun clearCache() {
    cachedNetworkTime = null
    clockOffset = Duration.ZERO
    accuracy = Duration.ZERO
    lastSyncSystemTime = null
  }

  /** 检查缓存剩余有效时间 */
  fun getCacheRemainingTime(): TrueTimeDuration? {
    val lastSync = lastSyncSystemTime ?: return null
    val currentSystemTime = Clock.System.now()
    val elapsed = currentSystemTime.minus(lastSync)
    val remaining = cacheValidDuration - elapsed

    return if (remaining > Duration.ZERO) remaining else Duration.ZERO
  }

  /** 获取调试信息 */
  fun getDebugInfo(): String = buildString {
    appendLine("ModernTimeKeeper Debug Info:")
    appendLine("- Has valid cache: ${hasValidCache()}")
    appendLine("- Clock offset: ${clockOffset.toHumanReadable()}")
    appendLine("- Accuracy: ${accuracy.toHumanReadable()}")
    appendLine("- Last sync: ${lastSyncSystemTime}")
    appendLine("- Cached time: ${cachedNetworkTime}")
    appendLine("- Cache remaining: ${getCacheRemainingTime()?.toHumanReadable() ?: "N/A"}")
  }
}
