package com.milo.reftime.sntp

import com.milo.reftime.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock

/** 现代化SNTP客户端 - 完全基于kotlinx-datetime和协程 */
class SntpClient {

  companion object Companion {
    // 公共NTP常量
    const val NTP_PORT = 123
    const val NTP_DEFAULT_TIMEOUT_SEC = 30

    // 内部使用的NTP常量
    private const val NTP_MODE = 3
    private const val NTP_VERSION = 3
    private const val NTP_PACKET_SIZE = 48

    private const val INDEX_VERSION = 0
    private const val INDEX_ROOT_DELAY = 4
    private const val INDEX_ROOT_DISPERSION = 8
    private const val INDEX_ORIGINATE_TIME = 24
    private const val INDEX_RECEIVE_TIME = 32
    private const val INDEX_TRANSMIT_TIME = 40

    // 1900年到1970年的精确偏移量（秒）
    private const val OFFSET_1900_TO_1970 = 2208988800L
  }

  /**
   * 请求网络时间
   *
   * @param server 服务器地址
   * @param timeout 超时时长
   * @return SNTP结果
   */
  suspend fun requestTime(
      server: String,
      timeout: RefTimeDuration = Duration.parse("PT30S")
  ): com.milo.reftime.SntpResult =
      withContext(Dispatchers.IO) {
        val address =
            try {
              InetAddress.getByName(server)
            } catch (e: Exception) {
              throw RefTimeError.InvalidResponse(
                  server, "Failed to resolve hostname: ${e.message}")
            }

        try {
          withTimeout(timeout) { performSntpRequest(address) }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
          throw RefTimeError.ServerTimeout(server, timeout)
        } catch (e: IOException) {
          throw RefTimeError.InvalidResponse(server, "Network error: ${e.message}")
        } catch (e: Exception) {
          throw RefTimeError.InvalidResponse(server, "Request failed: ${e.message}")
        }
      }

  private suspend fun performSntpRequest(address: InetAddress): com.milo.reftime.SntpResult {
    var socket: DatagramSocket? = null

    try {
      socket = DatagramSocket()
      socket.soTimeout = 30000 // 30 seconds timeout

      val buffer = ByteArray(NTP_PACKET_SIZE)
      val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)

      // 设置NTP版本
      writeNtpVersion(buffer)

      // 记录请求时间
      val requestTime = Clock.System.now()
      val requestTimeMillis = requestTime.toEpochMilliseconds()

      // 写入请求时间戳
      writeTimeStamp(buffer, INDEX_TRANSMIT_TIME, requestTimeMillis)

      // 发送请求
      socket.send(request)

      // 接收响应
      val response = DatagramPacket(buffer, buffer.size)
      socket.receive(response)

      val responseTime = Clock.System.now()

      // 解析响应
      return parseNtpResponse(buffer, requestTime, responseTime)
    } finally {
      socket?.close()
    }
  }

  private fun parseNtpResponse(
    buffer: ByteArray,
    requestTime: RefTimeInstant,
    responseTime: RefTimeInstant
  ): com.milo.reftime.SntpResult {

    // 提取时间戳
    val originateTime = readTimeStamp(buffer, INDEX_ORIGINATE_TIME) // T0
    val receiveTime = readTimeStamp(buffer, INDEX_RECEIVE_TIME) // T1
    val transmitTime = readTimeStamp(buffer, INDEX_TRANSMIT_TIME) // T2
    // responseTime 是 T3

    // 验证响应有效性
    validateNtpResponse(buffer)

    // NTP算法计算
    val T0 = RefTimeInstant.fromEpochMilliseconds(originateTime)
    val T1 = RefTimeInstant.fromEpochMilliseconds(receiveTime)
    val T2 = RefTimeInstant.fromEpochMilliseconds(transmitTime)
    val T3 = responseTime

    // 计算时钟偏移: ((T1 - T0) + (T2 - T3)) / 2
    val offset1 = T1.minus(T0)
    val offset2 = T2.minus(T3)
    val clockOffset = (offset1 + offset2) / 2

    // 计算往返延迟: (T3 - T0) - (T2 - T1)
    val roundTripDelay = T3.minus(T0).minus(T2.minus(T1))

    // 计算网络时间
    val networkTime = responseTime.plus(clockOffset)

    // 估算准确度（使用往返延迟的一半作为不确定性）
    val accuracy = roundTripDelay / 2

    return com.milo.reftime.SntpResult(
        networkTime = networkTime,
        clockOffset = clockOffset,
        roundTripDelay = roundTripDelay,
        accuracy = accuracy)
  }

  private fun validateNtpResponse(buffer: ByteArray) {
    // 检查模式
    val mode = (buffer[0].toInt() and 0x7).toByte()
    if (mode != 4.toByte() && mode != 5.toByte()) {
      throw RefTimeError.InvalidResponse("unknown", "Untrusted mode value: $mode")
    }

    // 检查stratum
    val stratum = buffer[1].toInt() and 0xff
    if (stratum < 1 || stratum > 15) {
      throw RefTimeError.InvalidResponse("unknown", "Untrusted stratum value: $stratum")
    }

    // 检查leap indicator
    val leap = ((buffer[0].toInt() shr 6) and 0x3).toByte()
    if (leap == 3.toByte()) {
      throw RefTimeError.InvalidResponse("unknown", "Unsynchronized server")
    }
  }

  private fun writeNtpVersion(buffer: ByteArray) {
    buffer[INDEX_VERSION] = (NTP_VERSION shl 3 or NTP_MODE).toByte()
  }

  private fun writeTimeStamp(buffer: ByteArray, offset: Int, time: Long) {
    // 从毫秒(1970年起)转换为NTP时间戳(1900年起，秒.秒的分数)
    val secondsSince1970 = time / 1000L
    val secondsSince1900 = secondsSince1970 + OFFSET_1900_TO_1970

    // NTP的分数部分使用2^32作为基数
    val millisecondsRemainder = time % 1000L
    val fraction = (millisecondsRemainder * 0x100000000L) / 1000L

    // 写入64位NTP时间戳（大端字节序）
    buffer[offset] = ((secondsSince1900 ushr 24) and 0xFF).toByte()
    buffer[offset + 1] = ((secondsSince1900 ushr 16) and 0xFF).toByte()
    buffer[offset + 2] = ((secondsSince1900 ushr 8) and 0xFF).toByte()
    buffer[offset + 3] = (secondsSince1900 and 0xFF).toByte()

    buffer[offset + 4] = ((fraction ushr 24) and 0xFF).toByte()
    buffer[offset + 5] = ((fraction ushr 16) and 0xFF).toByte()
    buffer[offset + 6] = ((fraction ushr 8) and 0xFF).toByte()
    buffer[offset + 7] = (fraction and 0xFF).toByte()
  }

  private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
    // 读取64位NTP时间戳
    val secondsSince1900 =
        ((buffer[offset].toLong() and 0xff) shl 24) or
            ((buffer[offset + 1].toLong() and 0xff) shl 16) or
            ((buffer[offset + 2].toLong() and 0xff) shl 8) or
            (buffer[offset + 3].toLong() and 0xff)

    val fraction =
        ((buffer[offset + 4].toLong() and 0xff) shl 24) or
            ((buffer[offset + 5].toLong() and 0xff) shl 16) or
            ((buffer[offset + 6].toLong() and 0xff) shl 8) or
            (buffer[offset + 7].toLong() and 0xff)

    // 从NTP时间戳转换为毫秒（1970年起）
    val secondsSince1970 = secondsSince1900 - OFFSET_1900_TO_1970
    val milliseconds = (secondsSince1970 * 1000L) + ((fraction * 1000L) ushr 32)

    // 验证时间合理性，返回原始值不做极端值调整
    return if (secondsSince1970 < 0 || secondsSince1970 > 4102444800L) {
      Clock.System.now().toEpochMilliseconds() // 无效时间使用系统时间
    } else {
      milliseconds
    }
  }
}
