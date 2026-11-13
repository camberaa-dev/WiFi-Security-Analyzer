package com.wifisecurity.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wifisecurity.analyzer.ui.WifiAnalyzerApp
import com.wifisecurity.analyzer.ui.theme.AnalyzerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnalyzerTheme { WifiAnalyzerApp() } }
    }
}
