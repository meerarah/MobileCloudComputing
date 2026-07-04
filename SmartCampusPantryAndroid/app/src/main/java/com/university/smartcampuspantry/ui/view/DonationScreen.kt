package com.university.smartcampuspantry.ui.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.smartcampuspantry.service.APIService
import com.university.smartcampuspantry.service.FirebaseService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen() {
    val firebaseService = FirebaseService.shared
    val apiService = APIService.shared
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    // Manual Entry Fields
    var itemName by remember { mutableStateOf("") }
    var itemCategory by remember { mutableStateOf("") }
    var itemExpiry by remember { mutableStateOf("") }
    var donationQty by remember { mutableStateOf(1) }
    
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FC))
            .verticalScroll(rememberScrollState())
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
                    text = "Donate Food Items",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101A2C)
                )
                Text(
                    text = "Manually enter the details of the item you are donating and attach a photo.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // File Selector (Simulated Camera Upload)
                Text("Attach Photo (Required for Verification):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (selectedImageUri != null) "📸 Photo Selected!" else "📸 Select Photo from Gallery")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Item Name
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("e.g., Canned Tuna") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Category
                OutlinedTextField(
                    value = itemCategory,
                    onValueChange = { itemCategory = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g., Proteins, Grains") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Expiration Date
                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState()

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                                val selectedMillis = datePickerState.selectedDateMillis
                                if (selectedMillis != null) {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    itemExpiry = sdf.format(java.util.Date(selectedMillis))
                                }
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                OutlinedTextField(
                    value = itemExpiry,
                    onValueChange = { itemExpiry = it },
                    label = { Text("Expiration Date") },
                    placeholder = { Text("Select date") },
                    singleLine = true,
                    readOnly = true,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Text("📅", fontSize = 20.sp)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Quantity
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantity:", fontWeight = FontWeight.Bold, color = Color(0xFF101A2C))
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
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF101A2C)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (itemName.isBlank() || itemCategory.isBlank() || itemExpiry.isBlank()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        var daysToExpiry = -1
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val expiryDate = sdf.parse(itemExpiry)
                            if (expiryDate != null) {
                                val diffInMillies = expiryDate.time - java.util.Date().time
                                daysToExpiry = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS).toInt()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (daysToExpiry <= 0) {
                            Toast.makeText(context, "Please enter a valid future date (YYYY-MM-DD)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (selectedImageUri == null) {
                            Toast.makeText(context, "Please attach a photo first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        val studentId = firebaseService.currentStudentId.value ?: return@Button
                        val pointsAwarded = 50 * donationQty

                        // 1. Upload photo to Firebase Storage (satisfies Cloud Storage requirement)
                        firebaseService.uploadDonationPhoto(selectedImageUri!!) { uploadResult ->
                            if (uploadResult.isSuccess) {
                                val uploadedUrl = uploadResult.getOrThrow()
                                // 2. Submit points award to Flask REST API
                                apiService.donatePoints(
                                    studentId = studentId,
                                    points = pointsAwarded,
                                    itemName = itemName,
                                    quantity = donationQty,
                                    imageUrl = uploadedUrl,
                                    location = "Kolej Mawar Hub"
                                ) { result ->
                                    if (result.isSuccess) {
                                        // 3. Add stocked item directly in Firestore inventory
                                        firebaseService.addInventoryItem(itemName, itemCategory, donationQty, uploadedUrl, daysToExpiry)
                                        isSubmitting = false
                                        
                                        // Reset fields
                                        itemName = ""
                                        itemCategory = ""
                                        itemExpiry = ""
                                        donationQty = 1
                                        selectedImageUri = null
                                        
                                        Toast.makeText(context, "Donation Successful! +$pointsAwarded Impact Points awarded.", Toast.LENGTH_LONG).show()
                                    } else {
                                        isSubmitting = false
                                        Toast.makeText(context, "Points synchronization failed.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                isSubmitting = false
                                Toast.makeText(context, "Photo upload failed. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Confirm Donation (+${50 * donationQty} pts)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
