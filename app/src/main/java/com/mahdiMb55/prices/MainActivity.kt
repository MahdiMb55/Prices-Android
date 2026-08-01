package com.mahdiMb55.prices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mahdiMb55.prices.core.designsystem.DesignSystemShowcase
import com.mahdiMb55.prices.core.designsystem.PricesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PricesTheme {
                DesignSystemShowcase()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DesignSystemPreview() {
    PricesTheme {
        DesignSystemShowcase()
    }
}
