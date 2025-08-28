package com.milo.reftime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.milo.reftime.model.*

/**
 * 现代化RefTime API - 完全基于 Kotlin 协程和 Flow
 *
 * 这是RefTime的主要接口，提供响应式的时间同步和访问功能。 使用kotlinx-datetime作为时间类型，Flow作为响应式数据流。
 */
interface RefTime {

  /**
   * 状态流 - 实时反映同步状态
   *
   * 可以订阅此流来监听RefTime的状态变化：
   * - Uninitialized: 未初始化
   * - Syncing: 同步中
   * - Available: 时间可用
   * - Failed: 同步失败
   */
  val state: StateFlow<RefTimeState>

  /**
   * 时间更新事件流 - 仅在获取到新时间时发射
   *
   * 当成功从NTP服务器获取到新的时间时，会发射最新的准确时间。 可以用于实时更新UI显示的时间。
   */
  val timeUpdates: Flow<RefTimeInstant>

  /**
   * 获取当前准确时间
   *
   * @return 当前的网络准确时间
   * @throws RefTimeError.NotSynced 如果尚未同步成功
   */
  suspend fun now(): RefTimeInstant

  /**
   * 安全获取时间，失败时返回null
   *
   * @return 当前的网络准确时间，如果未同步则返回null
   */
  suspend fun nowOrNull(): RefTimeInstant?

  /**
   * 获取时间，未同步时回退到系统时间
   *
   * @return 网络准确时间，如果未同步则返回系统时间
   */
  suspend fun nowSafe(): RefTimeInstant

  /**
   * 执行时间同步
   *
   * 这是一个suspend函数，会尝试连接配置的NTP服务器获取准确时间。 同步过程中会通过state流发送进度更新。
   *
   * @return Result.success(Unit) 如果同步成功，Result.failure(error) 如果失败
   */
  suspend fun sync(): Result<Unit>

  /**
   * 取消当前同步操作
   *
   * 取消正在进行的同步操作，并将状态重置为Uninitialized。
   */
  fun cancel()

  /**
   * 计算从指定时间到现在的持续时间
   *
   * @param since 起始时间
   * @return 持续时间，如果未同步则返回null
   */
  suspend fun durationSince(since: RefTimeInstant): RefTimeDuration?

  /**
   * 获取Unix时间戳（毫秒）
   *
   * @return Unix时间戳（毫秒），如果未同步则返回null
   */
  suspend fun nowMillis(): Long?

  /**
   * 检查是否已经同步
   *
   * @return true 如果时间已同步且有效，false 否则
   */
  suspend fun hasSynced(): Boolean

  /**
   * 获取时钟偏移量
   *
   * @return 本地时钟与网络时间的偏移量，如果未同步则返回null
   */
  suspend fun getClockOffset(): RefTimeDuration?

  /**
   * 获取时间准确度
   *
   * @return 时间的估计准确度，如果未同步则返回null
   */
  suspend fun getAccuracy(): RefTimeDuration?

  companion object Companion
}
