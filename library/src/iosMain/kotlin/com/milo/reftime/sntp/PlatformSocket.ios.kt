package com.milo.reftime.sntp

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** iOS 平台网络套接字实现 使用 iOS 原生网络 API (占位实现，需要完善) */
actual class PlatformSocket {

  actual override suspend fun send(
      host: String,
      port: Int,
      data: ByteArray,
      timeout: Long
  ): Result<Unit> {
    return withContext(Dispatchers.Default) {
      try {
        // iOS 网络实现需要完善
        // 这里使用简单的模拟实现
        println("iOS: Sending data to $host:$port (${data.size} bytes)")
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  actual override suspend fun receive(bufferSize: Int, timeout: Long): Result<ByteArray> {
    return withContext(Dispatchers.Default) {
      try {
        // iOS 网络实现需要完善
        // 这里返回模拟数据
        val response = ByteArray(48) { 0 }
        // 设置 NTP 响应包的基本格式
        response[0] = 0x1C // LI=0, VN=3, Mode=4 (server)
        Result.success(response)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  actual override fun close() {
    // iOS 资源清理
  }
}

/** iOS 平台协程调度器 */
actual val platformDispatcher: CoroutineContext = Dispatchers.Default

/** 创建 iOS 平台网络套接字 */
actual fun createPlatformSocket(): PlatformSocket = PlatformSocket()
