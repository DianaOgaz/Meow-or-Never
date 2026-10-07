package com.example.cepillagato

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cepillagato.ui.GameScreen
import com.example.cepillagato.ui.theme.CepillaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CepillaTheme {
                GameScreen()
            }
        }
    }
}
