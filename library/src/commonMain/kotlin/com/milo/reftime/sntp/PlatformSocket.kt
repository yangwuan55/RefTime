package com.milo.reftime.sntp

import kotlin.coroutines.CoroutineContext

/** 多平台网络套接字接口 使用 expect/actual 机制提供跨平台网络操作 */
expect class PlatformSocket {
  /**
   * 发送 UDP 数据包
   *
   * @param host 目标主机
   * @param port 目标端口
   * @param data 要发送的数据
   * @param timeout 超时时间（毫秒）
   */
  suspend fun send(host: String, port: Int, data: ByteArray, timeout: Long): Result<Unit>

  /**
   * 接收 UDP 数据包
   *
   * @param bufferSize 缓冲区大小
   * @param timeout 超时时间（毫秒）
   * @return 接收到的数据包
   */
  suspend fun receive(bufferSize: Int, timeout: Long): Result<ByteArray>

  /** 关闭套接字 */
  fun close()
}

/** 平台特定的协程上下文 */
expect val platformDispatcher: CoroutineContext

/** 创建平台特定的网络套接字 */
expect fun createPlatformSocket(): PlatformSocket
