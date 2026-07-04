package com.university.smartcampuspantry.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSignOutClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf("inventory") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kolej Mawar Pantry",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF101A2C)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == "inventory",
                    onClick = { selectedTab = "inventory" },
                    label = { Text("Inventory", fontSize = 10.sp) },
                    icon = { Text("🛒", fontSize = 20.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == "donate",
                    onClick = { selectedTab = "donate" },
                    label = { Text("Donate", fontSize = 10.sp) },
                    icon = { Text("🎁", fontSize = 20.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == "map",
                    onClick = { selectedTab = "map" },
                    label = { Text("Map", fontSize = 10.sp) },
                    icon = { Text("📍", fontSize = 20.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    label = { Text("Profile", fontSize = 10.sp) },
                    icon = { Text("👤", fontSize = 20.sp) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                "inventory" -> InventoryScreen()
                "donate" -> DonationScreen()
                "map" -> MapScreen()
                "profile" -> ProfileScreen(onSignOutClick = onSignOutClick)
            }
        }
    }
}
