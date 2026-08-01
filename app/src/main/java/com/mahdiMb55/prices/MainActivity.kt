package com.mahdiMb55.prices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mahdiMb55.prices.app.PricesApp
import com.mahdiMb55.prices.core.designsystem.PricesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PricesTheme {
                PricesApp()
            }
        }
    }
}
