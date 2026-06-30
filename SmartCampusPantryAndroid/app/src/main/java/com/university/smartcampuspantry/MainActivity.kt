package com.university.smartcampuspantry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.university.smartcampuspantry.service.FirebaseService
import com.university.smartcampuspantry.ui.view.LoginScreen
import com.university.smartcampuspantry.ui.view.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val firebaseService = FirebaseService.shared
            val userEmail by firebaseService.currentUserEmail

            var isLoggedIn by remember { mutableStateOf(userEmail != null) }

            // Sync Compose state with FirebaseService session state
            LaunchedEffect(userEmail) {
                isLoggedIn = userEmail != null
            }

            if (isLoggedIn) {
                MainScreen(onSignOutClick = {
                    isLoggedIn = false
                })
            } else {
                LoginScreen(onLoginSuccess = {
                    isLoggedIn = true
                })
            }
        }
    }
}
