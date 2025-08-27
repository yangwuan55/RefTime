package com.instacart.sample

import android.app.Application
import com.instacart.truetime.*
import kotlin.time.Duration

/** 示例应用 - 展示现代化TrueTime API的使用 */
class App : Application() {

  /** 全局TrueTime实例 - 使用现代化API */
  val trueTime: TrueTime = TrueTime {
    // 配置多个可靠的NTP服务器
    ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org", "time.cloudflare.com")

    // 网络超时30秒
    timeout(Duration.parse("PT30S"))

    // 最多重试3次
    retries(3)

    // 重试延迟配置
    retryDelay(base = Duration.parse("PT1S"), max = Duration.parse("PT30S"))

    // 启用调试日志（在Debug构建中）
    debug(BuildConfig.DEBUG)

    // 缓存有效期1小时
    cacheValid(Duration.parse("PT1H"))
  }

  override fun onCreate() {
    super.onCreate()

    // 应用启动时可以选择自动同步时间
    // lifecycleScope.launch {
    //     trueTime.sync()
    // }

    if (BuildConfig.DEBUG) {
      println("TrueTime App initialized with config: $trueTime")
    }
  }
}
