package com.milo.reftime

import com.milo.reftime.sntp.KtorSntpClient
import com.milo.reftime.time.TimeKeeper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/** KotlinDateTime TrueTime实现 - 完全基于kotlinx-datetime的现代化实现 */
class RefTimeImpl(private val config: TrueTimeConfig) : RefTime {

  // 协程作用域
  private val scope =
      CoroutineScope(
          Dispatchers.IO +
              SupervisorJob() +
              CoroutineName("TrueTime-${System.currentTimeMillis()}"))

  // 状态管理
  private val _state = MutableStateFlow<RefTimeState>(RefTimeState.Uninitialized)
  override val state: StateFlow<RefTimeState> = _state.asStateFlow()

  // 时间更新流
  private val _timeUpdates =
      MutableSharedFlow<RefTimeInstant>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  override val timeUpdates: Flow<RefTimeInstant> = _timeUpdates.asSharedFlow()

  // 核心组件
  private val timeKeeper = TimeKeeper()
  private val sntpClient = com.milo.reftime.sntp.KtorSntpClient()

  // 同步任务
  private var syncJob: Job? = null

  override suspend fun now(): RefTimeInstant {
    return nowOrNull() ?: throw RefTimeError.NotSynced
  }

  override suspend fun nowOrNull(): RefTimeInstant? {
    return timeKeeper.getCurrentTime()
  }

  override suspend fun nowSafe(): RefTimeInstant {
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
            is RefTimeState.Available -> Result.success(Unit)
            is RefTimeState.Failed -> Result.failure(currentState.error)
            else -> Result.failure(RefTimeError.NotSynced)
          }
        } catch (e: CancellationException) {
          Result.failure(RefTimeError.NetworkUnavailable)
        }
      }

  private suspend fun performSync() {
    try {
      _state.emit(RefTimeState.Syncing(0f))

      val servers = config.ntpHosts
      if (servers.isEmpty()) {
        throw RefTimeError.InvalidResponse("none", "No NTP servers configured")
      }

      val errors = mutableMapOf<String, Exception>()

      // 尝试每个服务器
      servers.forEachIndexed { index, server ->
        try {
          val progress = index.toFloat() / servers.size
          _state.emit(RefTimeState.Syncing(progress))

          if (config.debug) {
            println("TrueTime: Attempting sync with server: $server")
          }

          // 执行SNTP请求 (支持Ktor-based或传统实现)
          val result = sntpClient.requestTime(server = server, timeout = config.connectionTimeout)

          // 保存同步结果
          timeKeeper.saveSync(result)

          // 更新状态
          val successState =
              RefTimeState.Available(
                  clockOffset = result.clockOffset,
                  lastSyncTime = result.networkTime,
                  accuracy = result.accuracy)

          _state.emit(successState)
          _timeUpdates.emit(result.networkTime)

          if (config.debug) {
            println("TrueTime: Successfully synced with $server via ${result.source}")
            println("TrueTime: Clock offset: ${result.clockOffset.toHumanReadable()}")
            println("TrueTime: Network time: ${result.networkTime}")
            println("TrueTime: Accuracy estimate: ${result.accuracy.toHumanReadable()}")
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
      val error = RefTimeError.AllServersFailed(errors)
      _state.emit(RefTimeState.Failed(error))
    } catch (e: CancellationException) {
      // 同步被取消
      throw e
    } catch (e: Exception) {
      // 其他意外错误
      val error = RefTimeError.InvalidResponse("unknown", e.message ?: "Sync failed")
      _state.emit(RefTimeState.Failed(error))
    }
  }

  override fun cancel() {
    syncJob?.cancel()
    _state.value = RefTimeState.Uninitialized
  }

  override suspend fun durationSince(since: RefTimeInstant): RefTimeDuration? {
    return nowOrNull()?.minus(since)
  }

  override suspend fun nowMillis(): Long? {
    return nowOrNull()?.toEpochMilliseconds()
  }

  override suspend fun hasSynced(): Boolean {
    return timeKeeper.hasValidCache()
  }

  override suspend fun getClockOffset(): RefTimeDuration? {
    return if (hasSynced()) timeKeeper.getClockOffset() else null
  }

  override suspend fun getAccuracy(): RefTimeDuration? {
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
      RefTimeState.Uninitialized -> "TrueTime[Uninitialized]"
      is RefTimeState.Syncing -> "TrueTime[Syncing(${(current.progress * 100).toInt()}%)]"
      is RefTimeState.Available -> {
        val offset = current.clockOffset.toHumanReadable()
        "TrueTime[Available(offset=$offset)]"
      }
      is RefTimeState.Failed -> "TrueTime[Failed(${current.error.message})]"
    }
  }
}
