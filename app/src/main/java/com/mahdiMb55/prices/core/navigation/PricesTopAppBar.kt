package com.mahdiMb55.prices.core.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PricesTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onHistory: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    searchOpen: Boolean = false,
    searchQuery: String = "",
    searchHint: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: (() -> Unit)? = null,
    backContentDescription: String = "",
    searchContentDescription: String = "",
    historyContentDescription: String = "",
    settingsContentDescription: String = "",
    closeSearchContentDescription: String = ""
) {
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = searchOpen,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
                label = "search title transition"
            ) { isSearchOpen ->
                if (isSearchOpen) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(searchHint) },
                        singleLine = true
                    )
                } else {
                    Text(title)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backContentDescription)
                }
            }
        },
        actions = {
            if (searchOpen && onCloseSearch != null) {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Default.Close, contentDescription = closeSearchContentDescription)
                }
            } else {
                if (onSearch != null) {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = searchContentDescription)
                    }
                }
                if (onHistory != null) {
                    TextButton(onClick = onHistory) {
                        Text(historyContentDescription)
                    }
                }
                if (onSettings != null) {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = settingsContentDescription)
                    }
                }
            }
        }
    )
}
