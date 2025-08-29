package com.milo.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.milo.demo.ui.RefTimeApp
import com.milo.reftime.RefTime
import kotlin.time.Duration
import com.milo.reftime.config.*

fun main() = application {
    val refTime = RefTime {
        ntpHosts("time.google.com", "time.apple.com", "pool.ntp.org", "time.cloudflare.com")
        timeout(Duration.parse("PT30S"))
        retries(3)
        retryDelay(base = Duration.parse("PT1S"), max = Duration.parse("PT30S"))
        debug(true)
        cacheValid(Duration.parse("PT1H"))
    }
    
    Window(
        title = "TrueTime Desktop Demo",
        onCloseRequest = ::exitApplication
    ) {
        RefTimeApp(refTime = refTime)
    }
}