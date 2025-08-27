package com.instacart.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instacart.truetime.*
import kotlinx.coroutines.launch

/** 现代化示例Activity - 展示新的TrueTime API */
class SampleActivity : ComponentActivity() {

  private lateinit var trueTime: TrueTime

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 获取TrueTime实例
    trueTime = (application as App).trueTime

    setContent { TrueTimeTheme { TrueTimeApp(trueTime = trueTime) } }
  }
}

@Composable
fun TrueTimeApp(trueTime: TrueTime) {
  val scrollState = rememberScrollState()

  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 标题
        Text(
            text = "TrueTime Kotlin DateTime Demo", style = MaterialTheme.typography.headlineMedium)

        // 状态显示卡片
        TrueTimeStateCard(trueTime = trueTime)

        // 时间显示卡片
        TrueTimeDisplayCard(trueTime = trueTime)

        // 控制按钮
        TrueTimeControlsCard(trueTime = trueTime)

        // 调试信息卡片
        TrueTimeDebugCard(trueTime = trueTime)
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrueTimeStateCard(trueTime: TrueTime) {
  val state by trueTime.state.collectAsStateWithLifecycle()

  Card {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = "同步状态", style = MaterialTheme.typography.titleMedium)

      when (val current = state) {
        TrueTimeState.Uninitialized -> {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("未初始化")
              }
        }
        is TrueTimeState.Syncing -> {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Text("正在同步...")
                }
            LinearProgressIndicator(progress = current.progress, modifier = Modifier.fillMaxWidth())
            Text(
                text = "${(current.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall)
          }
        }
        is TrueTimeState.Available -> {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Column {
                  Text("✅ 同步成功")
                  Text(
                      text = "偏移: ${current.clockOffset.toHumanReadable()}",
                      style = MaterialTheme.typography.bodySmall)
                  Text(
                      text = "准确度: ${current.accuracy.toHumanReadable()}",
                      style = MaterialTheme.typography.bodySmall)
                }
              }
        }
        is TrueTimeState.Failed -> {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
                Column {
                  Text(text = "❌ 同步失败", color = MaterialTheme.colorScheme.error)
                  Text(
                      text = current.error.message ?: "未知错误",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onErrorContainer)
                }
              }
        }
      }
    }
  }
}

@Composable
fun TrueTimeDisplayCard(trueTime: TrueTime) {
  val currentTime by trueTime.timeUpdates.collectAsStateWithLifecycle(initialValue = null)
  val formattedTime by
      trueTime.timeUpdatesAsFormatted().collectAsStateWithLifecycle(initialValue = "")

  Card {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = "当前时间", style = MaterialTheme.typography.titleMedium)

      if (currentTime != null) {
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace)

        Text(
            text = "ISO: ${currentTime}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace)

        Text(
            text = "毫秒: ${currentTime?.toEpochMilliseconds()}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace)
      } else {
        Text(
            text = "时间未同步",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
fun TrueTimeControlsCard(trueTime: TrueTime) {
  val scope = rememberCoroutineScope()
  val state by trueTime.state.collectAsStateWithLifecycle()
  val isSyncing = state is TrueTimeState.Syncing

  Card {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(text = "控制操作", style = MaterialTheme.typography.titleMedium)

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
              scope.launch {
                trueTime
                    .sync()
                    .onSuccess { println("✅ 同步成功") }
                    .onFailure { error -> println("❌ 同步失败: ${error.message}") }
              }
            },
            enabled = !isSyncing,
            modifier = Modifier.weight(1f)) {
              if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
              } else {
                Text("同步时间")
              }
            }

        OutlinedButton(
            onClick = { trueTime.cancel() }, enabled = isSyncing, modifier = Modifier.weight(1f)) {
              Text("取消")
            }
      }

      // 快速操作按钮
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = {
              scope.launch {
                val time = trueTime.nowSafe()
                println("安全时间: $time")
              }
            },
            modifier = Modifier.weight(1f)) {
              Text("安全获取")
            }

        FilledTonalButton(
            onClick = {
              scope.launch {
                val time = trueTime.nowOrNull()
                println("可空时间: $time")
              }
            },
            modifier = Modifier.weight(1f)) {
              Text("可空获取")
            }
      }
    }
  }
}

@Composable
fun TrueTimeDebugCard(trueTime: TrueTime) {
  var showDebug by remember { mutableStateOf(false) }
  val debugInfo by trueTime.debugInfo().collectAsStateWithLifecycle(initialValue = "")

  Card {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
            Text(text = "调试信息", style = MaterialTheme.typography.titleMedium)

            TextButton(onClick = { showDebug = !showDebug }) { Text(if (showDebug) "隐藏" else "显示") }
          }

      if (showDebug) {
        Text(
            text = debugInfo,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(8.dp))
      }
    }
  }
}

@Composable
fun TrueTimeTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = lightColorScheme(), content = content)
}
