package com.university.smartcampuspantry.ui.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.smartcampuspantry.service.APIService
import com.university.smartcampuspantry.service.FirebaseService
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen() {
    val firebaseService = FirebaseService.shared
    val apiService = APIService.shared
    val context = LocalContext.current

    val files = listOf(
        "tuna_can_05.jpg",
        "cereal_box_02.jpg",
        "milk_carton_01.jpg",
        "apple_fresh_04.jpg",
        "vegetable_soup_11.jpg"
    )

    var selectedFile by remember { mutableStateOf(files[0]) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analyzedItem by remember { mutableStateOf<JSONObject?>(null) }
    var donationQty by remember { mutableStateOf(1) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Smart Donation Box",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101A2C)
                )
                Text(
                    text = "Use Firebase Cloud Storage & In-House Serverless ML to automatically identify and catalogue donated food items.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 1.4.sp
                )

                // Simulated Lens Viewfinder Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val emoji = when {
                            selectedFile.contains("tuna") -> "🐟"
                            selectedFile.contains("cereal") -> "🌾"
                            selectedFile.contains("milk") -> "🥛"
                            selectedFile.contains("apple") -> "🍎"
                            selectedFile.contains("soup") -> "🥣"
                            else -> "📸"
                        }
                        Text(emoji, fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(selectedFile, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    }
                }

                // File Selector
                Text("Select Mock Camera Image File:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(selectedFile)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            files.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file) },
                                    onClick = {
                                        selectedFile = file
                                        analyzedItem = null
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Run Analysis Button
                Button(
                    onClick = {
                        isAnalyzing = true
                        analyzedItem = null
                        // 1. Upload photo to Firebase Cloud Storage (returns public URL)
                        firebaseService.uploadDonationPhoto(selectedFile.replace(".jpg", "")) { uploadResult ->
                            if (uploadResult.isSuccess) {
                                val url = uploadResult.getOrThrow()
                                // 2. Send image URL to in-house Flask API to perform heavy ML processing
                                apiService.analyzeFoodImage(url) { analysisResult ->
                                    isAnalyzing = false
                                    if (analysisResult.isSuccess) {
                                        val data = analysisResult.getOrThrow()
                                        analyzedItem = data.getJSONObject("analyzedItem")
                                    } else {
                                        Toast.makeText(context, "Cloud ML execution failed.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                isAnalyzing = false
                                Toast.makeText(context, "Storage upload failed.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101A2C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAnalyzing
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Offload Image Recognition to Cloud")
                    }
                }
            }
        }

        // Analysis Results Panel
        analyzedItem?.let { item ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cloud ML Results",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF)
                    )

                    val name = item.getString("itemName")
                    val category = item.getString("category")
                    val days = item.getInt("daysToExpiry")
                    val confidence = (item.getDouble("confidenceScore") * 100).toInt()
                    val node = item.getString("offloadNode")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Detected Item:", fontSize = 11.sp, color = Color.Gray)
                            Text(name, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Category:", fontSize = 11.sp, color = Color.Gray)
                            Text(category, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Confidence:", fontSize = 11.sp, color = Color.Gray)
                            Text("$confidence%", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Forecast Shelf Life:", fontSize = 11.sp, color = Color.Gray)
                            Text("${days}d", fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "ML offloaded to $node",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )

                    Divider()

                    // Quantity and Confirm
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quantity:", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (donationQty > 1) donationQty-- },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-")
                            }
                            Text(
                                text = donationQty.toString(),
                                modifier = Modifier.padding(horizontal = 12.dp),
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { donationQty++ },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isSubmitting = true
                            val studentId = firebaseService.currentStudentId.value ?: return@Button
                            val pointsAwarded = 50 * donationQty
                            // 1. Submit points award to Flask REST API
                            apiService.donatePoints(
                                studentId = studentId,
                                points = pointsAwarded,
                                itemName = name,
                                quantity = donationQty,
                                imageUrl = selectedFile,
                                location = "Kolej Perindu Hub"
                            ) { result ->
                                if (result.isSuccess) {
                                    // 2. Add stocked item directly in Firestore inventory
                                    firebaseService.addInventoryItem(name, category, donationQty, selectedFile.replace(".jpg", ""), days)
                                    isSubmitting = false
                                    analyzedItem = null
                                    donationQty = 1
                                    Toast.makeText(context, "Donation Successful! +$pointsAwarded Impact Points awarded.", Toast.LENGTH_LONG).show()
                                } else {
                                    isSubmitting = false
                                    Toast.makeText(context, "Points synchronization failed.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Confirm Donation (+${50 * donationQty} pts)")
                        }
                    }
                }
            }
        }
    }
}
