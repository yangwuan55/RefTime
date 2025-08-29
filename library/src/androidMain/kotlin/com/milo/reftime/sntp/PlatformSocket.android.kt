package com.milo.reftime.sntp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android 平台网络套接字实现 */
actual class PlatformSocket {
  private val socket = DatagramSocket()

  actual suspend fun send(host: String, port: Int, data: ByteArray, timeout: Long): Result<Unit> {
    return withContext(Dispatchers.IO) {
      try {
        socket.soTimeout = timeout.toInt()
        val address = InetAddress.getByName(host)
        val packet = DatagramPacket(data, data.size, address, port)
        socket.send(packet)
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  actual suspend fun receive(bufferSize: Int, timeout: Long): Result<ByteArray> {
    return withContext(Dispatchers.IO) {
      try {
        socket.soTimeout = timeout.toInt()
        val buffer = ByteArray(bufferSize)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        Result.success(buffer.copyOf(packet.length))
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  actual fun close() {
    socket.close()
  }
}

/** Android 平台协程调度器 */
actual val platformDispatcher: CoroutineContext = Dispatchers.IO

/** 创建 Android 平台网络套接字 */
actual fun createPlatformSocket(): PlatformSocket = PlatformSocket()
