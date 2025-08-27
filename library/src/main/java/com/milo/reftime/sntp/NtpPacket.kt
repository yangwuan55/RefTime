package com.milo.reftime.sntp

import io.ktor.utils.io.core.*
import kotlin.math.roundToLong
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** 现代化NTP数据包表示 */
class NtpPacket() {
    var leapIndicator: Byte = 0
    var version: Byte = NTP_VERSION.toByte()
    var mode: Byte = NTP_MODE.toByte()
    var stratum: Byte = 0
    var pollInterval: Byte = 0
    var precision: Byte = 0
    var rootDelay: Int = 0
    var rootDispersion: Int = 0
    var referenceId: Int = 0
    var referenceTimestamp: Long = 0
    var originateTimestamp: Long = 0
    var receiveTimestamp: Long = 0
    var transmitTimestamp: Long = 0

    companion object {
        private const val OFFSET_1900_TO_EPOCH = 2208988800L
        private const val NTP_MAX_STORAGE = 255
        private const val NTP_VERSION = 3
        private const val NTP_MODE = 3

        /** 从字节流解析NTP包 */
        fun fromByteReadPacket(packet: ByteReadPacket): NtpPacket {
            return try {
                NtpPacket().apply {
                    leapIndicator = (packet.readByte().toInt() shr 6 and 0x03).toByte()
                    version = (packet.readByte().toInt() shr 3 and 0x07).toByte()
                    mode = (packet.readByte().toInt() and 0x07).toByte()
                    stratum = packet.readByte()
                    pollInterval = packet.readByte()
                    precision = packet.readByte()
                    rootDelay = (packet.readInt().toLong() and 0xFFFFFFFFL).toInt() shl 16 shr 16 // sign extend
                    rootDispersion = (packet.readInt().toLong() and 0xFFFFFFFFL).toInt() shl 16 shr 16
                    referenceId = (packet.readInt().toLong() and 0xFFFFFFFFL).toInt()
                    referenceTimestamp = readNtpTimestamp(packet)
                    originateTimestamp = readNtpTimestamp(packet)
                    receiveTimestamp = readNtpTimestamp(packet)
                    transmitTimestamp = readNtpTimestamp(packet)
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse NTP packet: ${e.message}", e)
            }
        }

        /** 读取NTP 64位时间戳 */
        private fun readNtpTimestamp(packet: ByteReadPacket): Long {
            val seconds = (packet.readInt().toLong() and 0xFFFFFFFFL) - OFFSET_1900_TO_EPOCH
            val fraction = (packet.readInt().toLong() and 0xFFFFFFFFL)
            val milliseconds = (seconds * 1000) + ((fraction * 1000L) / 0x100000000L)
            return milliseconds
        }
    }

    /** 转换为字节流 */
    fun toByteReadPacket(): ByteReadPacket = buildPacket {
        writeByte((leapIndicator.toInt() shl 6 or (version.toInt() shl 3) or mode.toInt()).toByte())
        writeByte(stratum)
        writeByte(pollInterval)
        writeByte(precision)
        writeInt(rootDelay)
        writeInt(rootDispersion)
        writeInt(referenceId)
        writeNtpTimestamp(referenceTimestamp)
        writeNtpTimestamp(originateTimestamp)
        writeNtpTimestamp(receiveTimestamp)
        writeNtpTimestamp(transmitTimestamp)
    }

    /** 写入NTP 64位时间戳 */
    private fun BytePacketBuilder.writeNtpTimestamp(milliseconds: Long) {
        val seconds = (milliseconds / 1000L) + OFFSET_1900_TO_EPOCH
        val fraction = ((milliseconds % 1000L) * 0x100000000L) / 1000L
        writeInt(seconds.toInt())
        writeInt(fraction.toInt())
    }

    /** 便于访问的属性 */
    var transmitTime: Long
        get() = transmitTimestamp
        set(milliseconds) { transmitTimestamp = milliseconds }

    var flags: Byte
        get() = (leapIndicator.toInt() shl 6 or (version.toInt() shl 3) or mode.toInt()).toByte()
        set(value) {
            leapIndicator = (value.toInt() shr 6 and 0x03).toByte()
            version = (value.toInt() shr 3 and 0x07).toByte()
            mode = (value.toInt() and 0x07).toByte()
        }

    fun toInstant(timestamp: Long): Instant =
        Instant.fromEpochMilliseconds(timestamp)
}
