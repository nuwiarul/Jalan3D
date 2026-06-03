package com.jalan3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.jalan3d.ui.Jalan3DNavigation
import com.jalan3d.ui.theme.Jalan3DTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Jalan3DTheme {
                Jalan3DNavigation(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
