package com.instacart.truetime

import com.instacart.truetime.sntp.ModernSntpClient
import com.instacart.truetime.time.ModernTimeKeeper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/** KotlinDateTime TrueTime实现 - 完全基于kotlinx-datetime的现代化实现 */
class KotlinDateTimeTrueTime(private val config: TrueTimeConfig) : TrueTime {

  // 协程作用域
  private val scope =
      CoroutineScope(
          Dispatchers.IO +
              SupervisorJob() +
              CoroutineName("TrueTime-${System.currentTimeMillis()}"))

  // 状态管理
  private val _state = MutableStateFlow<TrueTimeState>(TrueTimeState.Uninitialized)
  override val state: StateFlow<TrueTimeState> = _state.asStateFlow()

  // 时间更新流
  private val _timeUpdates =
      MutableSharedFlow<TrueTimeInstant>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  override val timeUpdates: Flow<TrueTimeInstant> = _timeUpdates.asSharedFlow()

  // 核心组件
  private val timeKeeper = ModernTimeKeeper()
  private val sntpClient = ModernSntpClient()

  // 同步任务
  private var syncJob: Job? = null

  override suspend fun now(): TrueTimeInstant {
    return nowOrNull() ?: throw TrueTimeError.NotSynced
  }

  override suspend fun nowOrNull(): TrueTimeInstant? {
    return timeKeeper.getCurrentTime()
  }

  override suspend fun nowSafe(): TrueTimeInstant {
    return nowOrNull() ?: Clock.System.now()
  }

  override suspend fun sync(): Result<Unit> =
      withContext(scope.coroutineContext) {
        // 取消之前的同步任务
        syncJob?.cancel()

        // 启动新的同步任务
        syncJob = launch { performSync() }

        // 等待同步完成
        try {
          syncJob!!.join()

          // 检查最终状态
          when (val currentState = _state.value) {
            is TrueTimeState.Available -> Result.success(Unit)
            is TrueTimeState.Failed -> Result.failure(currentState.error)
            else -> Result.failure(TrueTimeError.NotSynced)
          }
        } catch (e: CancellationException) {
          Result.failure(TrueTimeError.NetworkUnavailable)
        }
      }

  private suspend fun performSync() {
    try {
      _state.emit(TrueTimeState.Syncing(0f))

      val servers = config.ntpHosts
      if (servers.isEmpty()) {
        throw TrueTimeError.InvalidResponse("none", "No NTP servers configured")
      }

      val errors = mutableMapOf<String, Exception>()

      // 尝试每个服务器
      servers.forEachIndexed { index, server ->
        try {
          val progress = index.toFloat() / servers.size
          _state.emit(TrueTimeState.Syncing(progress))

          if (config.debug) {
            println("TrueTime: Attempting sync with server: $server")
          }

          // 执行SNTP请求
          val result = sntpClient.requestTime(server = server, timeout = config.connectionTimeout)

          // 保存同步结果
          timeKeeper.saveSync(result)

          // 更新状态
          val successState =
              TrueTimeState.Available(
                  clockOffset = result.clockOffset,
                  lastSyncTime = result.networkTime,
                  accuracy = result.accuracy)

          _state.emit(successState)
          _timeUpdates.emit(result.networkTime)

          if (config.debug) {
            println("TrueTime: Successfully synced with $server")
            println("TrueTime: Clock offset: ${result.clockOffset.toHumanReadable()}")
            println("TrueTime: Network time: ${result.networkTime}")
          }

          return // 成功，退出函数
        } catch (e: Exception) {
          errors[server] = e
          if (config.debug) {
            println("TrueTime: Failed to sync with $server: ${e.message}")
          }

          // 如果不是最后一个服务器，继续尝试下一个
          if (index < servers.size - 1) {
            return@forEachIndexed
          }
        }
      }

      // 所有服务器都失败了
      val error = TrueTimeError.AllServersFailed(errors)
      _state.emit(TrueTimeState.Failed(error))
    } catch (e: CancellationException) {
      // 同步被取消
      throw e
    } catch (e: Exception) {
      // 其他意外错误
      val error = TrueTimeError.InvalidResponse("unknown", e.message ?: "Sync failed")
      _state.emit(TrueTimeState.Failed(error))
    }
  }

  override fun cancel() {
    syncJob?.cancel()
    _state.value = TrueTimeState.Uninitialized
  }

  override suspend fun durationSince(since: TrueTimeInstant): TrueTimeDuration? {
    return nowOrNull()?.minus(since)
  }

  override suspend fun nowMillis(): Long? {
    return nowOrNull()?.toEpochMilliseconds()
  }

  override suspend fun hasSynced(): Boolean {
    return timeKeeper.hasValidCache()
  }

  override suspend fun getClockOffset(): TrueTimeDuration? {
    return if (hasSynced()) timeKeeper.getClockOffset() else null
  }

  override suspend fun getAccuracy(): TrueTimeDuration? {
    return if (hasSynced()) timeKeeper.getAccuracy() else null
  }

  /** 清理资源 */
  internal fun close() {
    scope.cancel()
    timeKeeper.clearCache()
  }

  /** 获取调试信息 */
  fun getDebugInfo(): String = buildString {
    appendLine("KotlinDateTimeTrueTime Debug Info:")
    appendLine("- State: ${_state.value}")
    appendLine("- Config: ${config}")
    appendLine("- Has synced: ${timeKeeper.hasValidCache()}")
    appendLine()
    append(timeKeeper.getDebugInfo())
  }

  override fun toString(): String {
    return when (val current = _state.value) {
      TrueTimeState.Uninitialized -> "TrueTime[Uninitialized]"
      is TrueTimeState.Syncing -> "TrueTime[Syncing(${(current.progress * 100).toInt()}%)]"
      is TrueTimeState.Available -> {
        val offset = current.clockOffset.toHumanReadable()
        "TrueTime[Available(offset=$offset)]"
      }
      is TrueTimeState.Failed -> "TrueTime[Failed(${current.error.message})]"
    }
  }
}
