package com.milo.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.milo.reftime.RefTime
import com.milo.reftime.ext.debugInfo
import com.milo.reftime.ext.timeUpdatesAsFormatted
import com.milo.reftime.model.RefTimeState
import com.milo.reftime.model.toHumanReadable
import kotlinx.coroutines.launch

/**
 * 共享的 RefTime 演示应用 UI
 * 支持 Android、Desktop 多平台
 */
@Composable
fun RefTimeApp(refTime: RefTime) {
    val state by refTime.state.collectAsState()
    val currentTime by refTime.timeUpdatesAsFormatted().collectAsState(initial = null)
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(text = "RefTime Kotlin DateTime Demo", style = MaterialTheme.typography.headlineMedium)
        
        // 状态显示卡片
        RefTimeStateCard(refTime = refTime, state = state)
        
        // 当前时间显示卡片
        RefTimeDisplayCard(currentTime = currentTime)
        
        // 控制按钮卡片
        RefTimeControlsCard(refTime = refTime)
        
        // 调试信息卡片
        RefTimeDebugCard(refTime = refTime, state = state)
    }
}

@Composable
fun RefTimeStateCard(refTime: RefTime, state: RefTimeState) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "同步状态", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            when (state) {
                is RefTimeState.Uninitialized -> {
                    Text(text = "未初始化", color = MaterialTheme.colorScheme.error)
                }
                is RefTimeState.Syncing -> {
                    val progress = (state as RefTimeState.Syncing).progress
                    Text(text = "同步中... ${(progress * 100).toInt()}%")
                    LinearProgressIndicator(progress = progress)
                }
                is RefTimeState.Available -> {
                    val availableState = state as RefTimeState.Available
                    Text(text = "✅ 时间已同步", color = MaterialTheme.colorScheme.primary)
                    Text(text = "时钟偏移: ${availableState.clockOffset.toHumanReadable()}")
                    Text(text = "最后同步: ${availableState.lastSyncTime}")
                    Text(text = "准确度: ${availableState.accuracy.toHumanReadable()}")
                }
                is RefTimeState.Failed -> {
                    val failedState = state as RefTimeState.Failed
                    Text(text = "❌ 同步失败", color = MaterialTheme.colorScheme.error)
                    Text(text = failedState.error.message ?: "未知错误")
                }
            }
        }
    }
}

@Composable
fun RefTimeDisplayCard(currentTime: String?) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "当前网络时间", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (currentTime != null) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displaySmall
                )
            } else {
                Text(text = "未同步", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun RefTimeControlsCard(refTime: RefTime) {
    val coroutineScope = rememberCoroutineScope()
    
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "控制", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            refTime.sync()
                        }
                    }
                ) {
                    Text(text = "同步时间")
                }
                
                Button(
                    onClick = { refTime.cancel() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(text = "取消同步")
                }
            }
        }
    }
}

@Composable
fun RefTimeDebugCard(refTime: RefTime, state: RefTimeState) {
    var showDebug by remember { mutableStateOf(false) }
    
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "调试信息", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                
                androidx.compose.material3.IconButton(onClick = { showDebug = !showDebug }) {
                    androidx.compose.material3.Text(text = if (showDebug) "▲" else "▼")
                }
            }
            
            if (showDebug) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Text(text = refTime.debugInfo().toString())
            }
        }
    }
}

/**
 * 共享主题定义
 */
@Composable
fun RefTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}