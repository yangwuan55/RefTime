package com.milo.reftime.config

import kotlin.time.Duration
import com.milo.reftime.RefTime
import com.milo.reftime.RefTimeImpl
import com.milo.reftime.model.*

/** DSL标记注解 */
@DslMarker annotation class RefTimeDsl

/** RefTime配置类 - 使用Kotlin DSL */
@RefTimeDsl
class RefTimeConfig internal constructor() {

  /** NTP服务器主机列表 */
  var ntpHosts: List<String> = listOf("time.google.com", "time.apple.com", "pool.ntp.org")

  /** 连接超时时间 */
  var connectionTimeout: RefTimeDuration = Duration.parse("PT30S")

  /** 最大重试次数 */
  var maxRetries: Int = 3

  /** 基础重试延迟 */
  var baseRetryDelay: RefTimeDuration = Duration.parse("PT1S")

  /** 最大重试延迟 */
  var maxRetryDelay: RefTimeDuration = Duration.parse("PT30S")

  /** 启用调试日志 */
  var debug: Boolean = false

  /** 缓存有效期 */
  var cacheValidDuration: RefTimeDuration = Duration.parse("PT1H")

  /** 设置NTP服务器主机（变参数） */
  fun ntpHosts(vararg hosts: String) {
    this.ntpHosts = hosts.toList()
  }

  /** 设置单个NTP服务器主机 */
  fun host(host: String) {
    this.ntpHosts = listOf(host)
  }

  /** 从集合设置NTP服务器主机 */
  fun hosts(hosts: Collection<String>) {
    this.ntpHosts = hosts.toList()
  }

  /** 设置超时时间 */
  fun timeout(duration: RefTimeDuration) {
    this.connectionTimeout = duration
  }

  /** 设置重试次数 */
  fun retries(count: Int) {
    this.maxRetries = count
  }

  /** 设置重试延迟配置 */
  fun retryDelay(base: RefTimeDuration, max: RefTimeDuration = Duration.parse("PT30S")) {
    this.baseRetryDelay = base
    this.maxRetryDelay = max
  }

  /** 启用或禁用调试模式 */
  fun debug(enabled: Boolean = true) {
    this.debug = enabled
  }

  /** 设置缓存有效期 */
  fun cacheValid(duration: RefTimeDuration) {
    this.cacheValidDuration = duration
  }

  override fun toString(): String = buildString {
    append("RefTimeConfig(")
    append("hosts=${ntpHosts.take(3).joinToString()}")
    if (ntpHosts.size > 3) append("...")
    append(", timeout=${connectionTimeout.toHumanReadable()}")
    append(", retries=$maxRetries")
    append(", debug=$debug")
    append(")")
  }
}

/** DSL构建函数 - 创建RefTime实例 */
fun RefTime(config: RefTimeConfig.() -> Unit = {}): RefTime {
  val configuration = RefTimeConfig().apply(config)
  return RefTimeImpl(configuration)
}

/** 预定义配置 - 默认配置 */
fun RefTime.Companion.default(): RefTime = RefTime {
  ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org")
  timeout(Duration.parse("PT30S"))
  retries(3)
  debug(false)
}

/** 预定义配置 - 快速配置（短超时） */
fun RefTime.Companion.fast(): RefTime = RefTime {
  host("time.google.com")
  timeout(Duration.parse("PT10S"))
  retries(1)
  debug(false)
}

/** 预定义配置 - 可靠配置（多服务器、长超时） */
fun RefTime.Companion.reliable(): RefTime = RefTime {
  ntpHosts(
      "time.google.com", "time.apple.com", "pool.ntp.org", "time.nist.gov", "time.cloudflare.com")
  timeout(Duration.parse("PT60S"))
  retries(5)
  debug(false)
}

/** 预定义配置 - 调试配置 */
fun RefTime.Companion.debug(): RefTime = RefTime {
  ntpHosts("time.google.com", "time.apple.com")
  timeout(Duration.parse("PT30S"))
  retries(3)
  debug(true)
}
