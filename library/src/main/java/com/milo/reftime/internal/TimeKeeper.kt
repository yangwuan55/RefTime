package com.milo.reftime.internal

import com.milo.reftime.model.*
import kotlin.time.Duration
import kotlinx.datetime.Clock

/** 现代化时间管理器 - 完全基于kotlinx-datetime */
internal class TimeKeeper(private val cacheValidDuration: RefTimeDuration = Duration.parse("PT1H")) {

  // 缓存的网络时间
  private var cachedNetworkTime: RefTimeInstant? = null

  // 时钟偏移量
  private var clockOffset: RefTimeDuration = Duration.ZERO

  // 准确度
  private var accuracy: RefTimeDuration = Duration.ZERO

  // 最后同步时的系统时间
  private var lastSyncSystemTime: RefTimeInstant? = null

  // 缓存有效期 (从配置传入或使用默认值)

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
  fun getCurrentTime(): RefTimeInstant? {
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
  fun getClockOffset(): RefTimeDuration = clockOffset

  /** 获取准确度 */
  fun getAccuracy(): RefTimeDuration = accuracy

  /** 获取最后同步时间 */
  fun getLastSyncTime(): RefTimeInstant? = cachedNetworkTime

  /** 清除缓存 */
  fun clearCache() {
    cachedNetworkTime = null
    clockOffset = Duration.ZERO
    accuracy = Duration.ZERO
    lastSyncSystemTime = null
  }

  /** 检查缓存剩余有效时间 */
  fun getCacheRemainingTime(): RefTimeDuration? {
    val lastSync = lastSyncSystemTime ?: return null
    val currentSystemTime = Clock.System.now()
    val elapsed = currentSystemTime.minus(lastSync)
    val remaining = cacheValidDuration - elapsed

    return if (remaining > Duration.ZERO) remaining else Duration.ZERO
  }

}
