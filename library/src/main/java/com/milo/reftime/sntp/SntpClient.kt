package com.milo.reftime.sntp

import com.milo.reftime.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.time.Duration
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Ktor-based现代化SNTP客户端实现
 * 纯UDP NTP协议实现
 */
class SntpClient() {

    companion object Companion {
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_PORT = 123
        private const val NTP_MODE = 3    // 客户端模式
        private const val NTP_VERSION = 3 // NTP版本3

        // Number of seconds between Jan 1, 1900 and Jan 1, 1970
        private const val OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L

        // NTP packet offsets
        private const val RECEIVE_TIME_OFFSET = 32
        private const val TRANSMIT_TIME_OFFSET = 40
    }

    /**
     * 请求网络时间，使用NTP UDP协议
     *
     * @param server NTP服务器地址
     * @param timeout 超时时长
     * @return SNTP结果
     */
    suspend fun requestTime(
        server: String,
        timeout: RefTimeDuration = Duration.parse("PT30S")
    ): SntpResult {
        return performNtpUdpRequest(server, timeout)
    }

    /**
     * 执行UDP NTP请求
     * 1. 创建UDP socket连接
     * 2. 构建并发送NTP请求包
     * 3. 接收并解析NTP响应包
     * 4. 计算时间偏移和延迟
     */
    private suspend fun performNtpUdpRequest(server: String, timeout: RefTimeDuration): SntpResult {
        return withContext(Dispatchers.IO) {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeout.inWholeMilliseconds.toInt()
                val address = InetAddress.getByName(server)
                val buffer = ByteArray(NTP_PACKET_SIZE)
                val request = DatagramPacket(buffer, buffer.size, address, NTP_PORT)

                // 设置mode = 3 (client) 和 version = 3
                buffer[0] = ((NTP_VERSION shl 3) or NTP_MODE and 0xFF).toByte()

                // 记录请求发送时间
                val requestTime = Clock.System.now()
                val requestTimestamp = requestTime.toEpochMilliseconds()
                writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTimestamp)

                // 发送NTP请求
                socket.send(request)

                // 接收NTP响应
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)
                val responseTime = Clock.System.now()

                // 检查响应包大小
                if (response.length < NTP_PACKET_SIZE) {
                    throw RefTimeError.InvalidResponse(server, "NTP response too short: ${response.length} bytes")
                }

                // 提取时间信息
                val receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET)
                val transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET)

                // 计算时间结果
                calculateNtpTiming(receiveTime, transmitTime, requestTimestamp, responseTime.toEpochMilliseconds())
            }
        }
    }

    /**
     * 计算NTP时间结果
     * 使用NTP算法计算时钟偏移和往返延迟:
     * - T0: 客户端发送时间 (requestTimestamp)
     * - T1: 服务器接收时间 (receiveTime)
     * - T2: 服务器发送时间 (transmitTime)
     * - T3: 客户端接收时间 (responseTimestamp)
     *
     * 时钟偏移 = ((T1 - T0) + (T2 - T3)) / 2
     * 往返延迟 = (T3 - T0) - (T2 - T1)
     */
    private fun calculateNtpTiming(
        receiveTime: Long,
        transmitTime: Long,
        requestTimestamp: Long,
        responseTimestamp: Long
    ): SntpResult {
        val roundTripDelay = (responseTimestamp - requestTimestamp) - (transmitTime - receiveTime)
        val clockOffset = ((receiveTime - requestTimestamp) + (transmitTime - responseTimestamp)) / 2
        val networkTime = responseTimestamp + clockOffset
        val accuracy = roundTripDelay / 2

        return SntpResult(
            networkTime = RefTimeInstant.fromEpochMilliseconds(networkTime),
            clockOffset = clockOffset.toDuration(DurationUnit.MILLISECONDS),
            roundTripDelay = roundTripDelay.toDuration(DurationUnit.MILLISECONDS),
            accuracy = accuracy.toDuration(DurationUnit.MILLISECONDS),
            source = "UDP-NTP"
        )
    }

    private fun read32(buffer: ByteArray, offset: Int): Long {
        val b0 = buffer[offset].toInt() and 0xFF
        val b1 = buffer[offset + 1].toInt() and 0xFF
        val b2 = buffer[offset + 2].toInt() and 0xFF
        val b3 = buffer[offset + 3].toInt() and 0xFF

        return ((b0.toLong() shl 24) or (b1.toLong() shl 16) or (b2.toLong() shl 8) or b3.toLong())
    }

    private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
        val seconds = read32(buffer, offset)
        val fraction = read32(buffer, offset + 4)
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L)
    }

    private fun writeTimeStamp(buffer: ByteArray, offset: Int, time: Long) {
        var currentOffset = offset
        val seconds = time / 1000L
        val milliseconds = time - seconds * 1000L
        val ntpSeconds = seconds + OFFSET_1900_TO_1970

        // Write seconds in big endian format
        buffer[currentOffset++] = (ntpSeconds shr 24).toByte()
        buffer[currentOffset++] = (ntpSeconds shr 16).toByte()
        buffer[currentOffset++] = (ntpSeconds shr 8).toByte()
        buffer[currentOffset++] = ntpSeconds.toByte()

        val fraction = milliseconds * 0x100000000L / 1000L
        // Write fraction in big endian format
        buffer[currentOffset++] = (fraction shr 24).toByte()
        buffer[currentOffset++] = (fraction shr 16).toByte()
        buffer[currentOffset++] = (fraction shr 8).toByte()
        // Low order bits should be random data
        buffer[currentOffset] = (Math.random() * 255.0).toInt().toByte()
    }
}
