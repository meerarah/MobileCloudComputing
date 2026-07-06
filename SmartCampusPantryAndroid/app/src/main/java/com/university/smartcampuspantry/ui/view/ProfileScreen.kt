package com.university.smartcampuspantry.ui.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.smartcampuspantry.model.StudentProfile
import com.university.smartcampuspantry.service.APIService
import com.university.smartcampuspantry.service.FirebaseService

@Composable
fun ProfileScreen(onSignOutClick: () -> Unit) {
    val firebaseService = FirebaseService.shared
    val apiService = APIService.shared
    val context = LocalContext.current

    var profile by remember { mutableStateOf<StudentProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCouponDialog by remember { mutableStateOf(false) }
    var redeemedCouponCode by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

    fun refreshProfile() {
        val studentId = firebaseService.currentStudentId.value ?: return
        isLoading = true
        apiService.fetchStudentProfile(studentId) { result ->
            isLoading = false
            if (result.isSuccess) {
                profile = result.getOrThrow()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF101A2C))
            }
        } else {
            profile?.let { p ->
                // Avatar card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Initials circle
                        val initials = p.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF101A2C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101A2C))
                        Text("STUDENT ID: ${p.studentId}", fontSize = 12.sp, color = Color.Gray)

                        // Status badge pill
                        val statusText = if (p.eligible) "Eligible & Active" else "Suspended / Hold"
                        val statusColor = if (p.eligible) Color(0xFF34C759) else Color(0xFFFF3B30)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Points & Weekly claims card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Contribution score
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Contribution Points:", fontWeight = FontWeight.Bold)
                            Text("${p.impactPoints} pts", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        Divider()

                        // Quota usage
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Weekly Quota Usage:", fontWeight = FontWeight.Bold)
                                Text("${p.claimsThisWeek} / ${p.maxWeeklyClaims}", fontWeight = FontWeight.Bold)
                            }

                            // Progress bar
                            val progress = p.claimsThisWeek.toFloat() / p.maxWeeklyClaims.toFloat()
                            val barColor = if (p.claimsThisWeek >= p.maxWeeklyClaims) Color(0xFFFF3B30) else Color(0xFFFF9500)
                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                color = barColor,
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }
                        
                        // Rewards Section
                        if (p.impactPoints >= 200) {
                            Divider()
                            Button(
                                onClick = {
                                    isRedeeming = true
                                    apiService.redeemCoupon(p.studentId) { result ->
                                        isRedeeming = false
                                        if (result.isSuccess) {
                                            val json = result.getOrThrow()
                                            redeemedCouponCode = json.optString("couponCode", "UITM-CARES-XXXX")
                                            showCouponDialog = true
                                            refreshProfile()
                                        } else {
                                            Toast.makeText(context, "Failed to redeem: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !isRedeeming
                            ) {
                                Text(if (isRedeeming) "Redeeming..." else "Redeem Coupon (200 pts)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }



                Spacer(modifier = Modifier.weight(1f))

                // Sign Out
                Button(
                    onClick = {
                        firebaseService.signOut()
                        onSignOutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    
    if (showCouponDialog) {
        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text("🎉 Reward Unlocked!") },
            text = {
                Column {
                    Text("Show this coupon code to the campus cafe for a free meal or drink!")
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF4F7FC), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(redeemedCouponCode, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101A2C), textAlign = TextAlign.Center)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCouponDialog = false }) {
                    Text("Awesome!")
                }
            }
        )
    }
}

// Utility to run reset claims inside a background thread
private fun threadCallReset(callback: (Boolean) -> Unit) {
    kotlin.concurrent.thread {
        try {
            val urlObj = java.net.URL("${APIService.shared.baseURL}/api/reset-claims")
            val conn = urlObj.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            val success = conn.responseCode == 200
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(success)
            }
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(false)
            }
        }
    }
}
