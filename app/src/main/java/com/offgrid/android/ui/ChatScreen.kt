package com.offgrid.android.ui

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Legacy ChatScreen - now redirects to HikeChatScreen for consistency
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    // Show settings sheet state
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // Use HikeChatScreen for consistent UI
    HikeChatScreen(
        viewModel = viewModel,
        onSettingsClick = { showSettingsSheet = true }
    )
    
    // Settings sheet
    val currentNickname by viewModel.nickname.collectAsStateWithLifecycle()
    AboutSheet(
        isPresented = showSettingsSheet,
        onDismiss = { showSettingsSheet = false },
        nickname = currentNickname,
        onNicknameChange = { newNickname ->
            viewModel.setNickname(newNickname)
        }
    )
}
