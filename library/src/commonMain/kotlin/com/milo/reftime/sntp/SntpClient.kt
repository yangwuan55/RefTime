package com.milo.reftime.sntp

import com.milo.reftime.model.*
import kotlin.time.Duration
import kotlinx.coroutines.*

/** 多平台 SNTP 客户端 使用多平台网络实现，支持 Android、iOS、JVM */
class SntpClient() {

  companion object {
    private const val NTP_PACKET_SIZE = 48
    private const val NTP_PORT = 123
    private const val NTP_MODE = 3 // 客户端模式
    private const val NTP_VERSION = 3 // NTP版本3

    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
    private const val OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L

    // NTP packet offsets
    private const val RECEIVE_TIME_OFFSET = 32
    private const val TRANSMIT_TIME_OFFSET = 40
  }

  private val sntpClientCommon = SntpClientCommon()

  /**
   * 请求网络时间，使用多平台网络实现
   *
   * @param server NTP服务器地址
   * @param timeout 超时时长
   * @return SNTP结果
   */
  suspend fun requestTime(
      server: String,
      timeout: RefTimeDuration = Duration.parse("PT30S")
  ): SntpResult {
    return sntpClientCommon.requestTime(server, timeout)
  }
}
