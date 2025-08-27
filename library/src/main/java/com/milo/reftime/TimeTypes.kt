package com.milo.reftime

import kotlin.time.Duration

/** TrueTime 现代化类型系统 - 完全基于 kotlinx-datetime */

// 统一时间类型定义
typealias RefTimeInstant = kotlinx.datetime.Instant

typealias RefTimeDuration = kotlin.time.Duration

/** TrueTime 状态 - 响应式状态管理 */
sealed interface RefTimeState {
  /** 未初始化状态 */
  data object Uninitialized : RefTimeState

  /**
   * 同步中状态
   *
   * @param progress 同步进度 (0.0-1.0)
   */
  data class Syncing(val progress: Float = 0f) : RefTimeState

  /**
   * 时间可用状态
   *
   * @param clockOffset 时钟偏移量
   * @param lastSyncTime 最后同步时间
   * @param accuracy 准确度
   */
  data class Available(
    val clockOffset: RefTimeDuration,
    val lastSyncTime: RefTimeInstant,
    val accuracy: RefTimeDuration
  ) : RefTimeState

  /**
   * 同步失败状态
   *
   * @param error 错误信息
   */
  data class Failed(val error: RefTimeError) : RefTimeState
}

/** TrueTime 错误类型 - 类型安全的错误处理 */
sealed class RefTimeError : Exception() {
  /** 时间未同步错误 */
  data object NotSynced : RefTimeError() {
    override val message: String = "TrueTime has not been synced yet"
  }

  /** 网络不可用错误 */
  data object NetworkUnavailable : RefTimeError() {
    override val message: String = "Network is not available for time sync"
  }

  /**
   * 服务器超时错误
   *
   * @param server 服务器地址
   * @param timeout 超时时长
   */
  data class ServerTimeout(val server: String, val timeout: RefTimeDuration) : RefTimeError() {
    override val message: String = "Timeout connecting to server $server after $timeout"
  }

  /**
   * 无效响应错误
   *
   * @param server 服务器地址
   * @param reason 错误原因
   */
  data class InvalidResponse(val server: String, val reason: String) : RefTimeError() {
    override val message: String = "Invalid response from server $server: $reason"
  }

  /**
   * 所有服务器都失败
   *
   * @param errors 各服务器的错误信息
   */
  data class AllServersFailed(val errors: Map<String, Exception>) : RefTimeError() {
    override val message: String = "All NTP servers failed: ${errors.keys.joinToString()}"
  }
}

/** SNTP 请求结果 */
data class SntpResult(
  val networkTime: RefTimeInstant,
  val clockOffset: RefTimeDuration,
  val roundTripDelay: RefTimeDuration,
  val accuracy: RefTimeDuration
)

/** 扩展函数 - Duration 格式化 */
fun RefTimeDuration.toHumanReadable(): String =
    when {
      this < Duration.parse("1s") -> "${inWholeMilliseconds}ms"
      this < Duration.parse("1m") -> "${inWholeSeconds}s"
      this < Duration.parse("1h") -> "${inWholeMinutes}m"
      else -> "${inWholeHours}h"
    }

