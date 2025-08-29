package com.milo.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.milo.demo.ui.RefTimeApp
import com.milo.demo.ui.RefTimeTheme
import com.milo.reftime.config.*

/**
 * 示例 Activity - 展示 RefTime 统一 Compose UI
 */
class SampleActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 获取全局 RefTime 实例
        val app = application as App
        val refTime = app.refTime
        
        setContent {
            RefTimeTheme {
                RefTimeApp(refTime = refTime)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SampleActivityPreview() {
    RefTimeTheme {
        // 预览模式下使用默认配置
        val refTime = com.milo.reftime.RefTime.default()
        RefTimeApp(refTime = refTime)
    }
}