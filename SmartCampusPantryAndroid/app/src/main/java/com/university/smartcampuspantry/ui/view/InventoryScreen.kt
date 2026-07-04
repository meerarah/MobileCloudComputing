package com.university.smartcampuspantry.ui.view

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.smartcampuspantry.model.FoodItem
import com.university.smartcampuspantry.service.APIService
import com.university.smartcampuspantry.service.FirebaseService
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    val firebaseService = FirebaseService.shared
    val apiService = APIService.shared
    val context = LocalContext.current

    // Search and Filters State
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedStockFilter by remember { mutableStateOf("All") }
    
    // Popup Dialogue States
    var reportingItem by remember { mutableStateOf<FoodItem?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    val categories = listOf("All", "Proteins", "Grains", "Dairy", "Produce", "Canned Goods")
    val stockFilters = listOf("All", "Plenty", "Running Low", "Out of Stock")

    // Filter items based on category, search, and stock level criteria
    val filteredItems = firebaseService.inventory.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        val matchesStockLevel = when (selectedStockFilter) {
            "All" -> true
            "Plenty" -> item.quantity > 10
            "Running Low" -> item.quantity in 1..10
            "Out of Stock" -> item.quantity == 0
            else -> true
        }
        matchesSearch && matchesCategory && matchesStockLevel
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar with QR Scanner Trailing Icon
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search pantry inventory...") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showScanner = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Scan Item Barcode",
                        tint = Color(0xFF007AFF)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007AFF),
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Type of Items (Categories horizontal chips list)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("CATEGORIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.take(3).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF101A2C),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.drop(3).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF101A2C),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Live Stock Level Filters
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("LIVE STOCK LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stockFilters.forEach { filter ->
                    val color = when (filter) {
                        "Plenty" -> Color(0xFF34C759)
                        "Running Low" -> Color(0xFFFF9500)
                        "Out of Stock" -> Color(0xFFFF3B30)
                        else -> Color(0xFF007AFF)
                    }
                    FilterChip(
                        selected = selectedStockFilter == filter,
                        onClick = { selectedStockFilter = filter },
                        label = { Text(filter, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color,
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedStockFilter == filter) color else Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Inventory Stock Grid List
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No stock items fit selection.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    FoodCard(
                        item = item,
                        onClaimClick = {
                            val studentId = firebaseService.currentStudentId.value ?: return@FoodCard
                            apiService.claimItem(studentId, item.id, item.name) { result ->
                                if (result.isSuccess) {
                                    val data = result.getOrThrow()
                                    firebaseService.claimInventoryItem(item.id)
                                    Toast.makeText(context, data.getString("message"), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Claim denied.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onReportClick = {
                            reportingItem = item
                        }
                    )
                }
            }
        }
    }

    // Barcode/QR Code Scanner Simulation Overlay
    if (showScanner) {
        AlertDialog(
            onDismissRequest = { showScanner = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📷", fontSize = 24.sp)
                    Text("Simulated Barcode Scanner")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Students scan items using their phone's camera to claim them instantly. Select a mock item to simulate scanning its barcode:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    // Simulated Scanning Camera Feed Box with laser effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Scanner box overlay bounds outline
                        Box(
                            modifier = Modifier
                                .size(90.dp, 70.dp)
                                .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(4.dp))
                        )

                        // Animated scanning line
                        val infiniteTransition = rememberInfiniteTransition(label = "laser")
                        val laserOffset by infiniteTransition.animateFloat(
                            initialValue = -30f,
                            targetValue = 30f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laserOffset"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = laserOffset.dp)
                                .background(Color.Red)
                        )

                        Text("SCANNING ACTIVE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                    }

                    // Clickable list of items to scan
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        firebaseService.inventory.take(4).forEach { item ->
                            Button(
                                onClick = {
                                    searchQuery = item.name
                                    showScanner = false
                                    Toast.makeText(context, "Barcode read successfully: ${item.name}", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Scan Item", fontSize = 10.sp, color = Color(0xFF007AFF))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showScanner = false }) {
                    Text("Close Scanner")
                }
            }
        )
    }

    // Item reporting Dialogue Form
    if (reportingItem != null) {
        var issueText by remember { mutableStateOf("") }
        var isSubmittingReport by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { reportingItem = null },
            title = { Text("Report Issue: ${reportingItem?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Describe the item issue (e.g. expired, damaged seal, out of stock shelf):", fontSize = 12.sp)
                    OutlinedTextField(
                        value = issueText,
                        onValueChange = { issueText = it },
                        placeholder = { Text("Describe here...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = reportingItem ?: return@Button
                        val studentId = firebaseService.currentStudentId.value ?: return@Button
                        if (issueText.trim().isEmpty()) {
                            Toast.makeText(context, "Please describe the issue.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmittingReport = true
                        apiService.submitReport(
                            studentId = studentId,
                            itemName = item.name,
                            location = "Kolej Mawar Hub",
                            issue = issueText.trim()
                        ) { result ->
                            isSubmittingReport = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Report submitted successfully!", Toast.LENGTH_LONG).show()
                                reportingItem = null
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed to submit report.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    enabled = !isSubmittingReport
                ) {
                    if (isSubmittingReport) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Submit")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FoodCard(item: FoodItem, onClaimClick: () -> Unit, onReportClick: () -> Unit) {
    val stockLevelColor = when {
        item.quantity > 10 -> Color(0xFF34C759)      // Green Plenty
        item.quantity in 1..10 -> Color(0xFFFF9500)  // Orange Running Low
        else -> Color(0xFFFF3B30)                    // Red Out of Stock
    }
    
    val stockLevelText = when {
        item.quantity > 10 -> "Plenty"
        item.quantity in 1..10 -> "Running Low"
        else -> "Out of Stock"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category & Stock Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
                // Color-coded live stock level label
                Surface(
                    color = stockLevelColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stockLevelText.uppercase(),
                        color = stockLevelColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Food Image Box with AsyncImage loader
            val nameLower = item.name.lowercase()
            var resolvedUrl = ""
            if (item.imageUrl.isNotEmpty() && (item.imageUrl.startsWith("http") || item.imageUrl.startsWith("content://"))) {
                resolvedUrl = item.imageUrl
            } else {
                val searchKey = if (item.imageUrl.isNotEmpty()) item.imageUrl.lowercase() else nameLower
                resolvedUrl = when {
                    searchKey.contains("tuna") -> "https://images.unsplash.com/photo-1599084993091-1cb5c0721cc6?w=150&auto=format&fit=crop&q=60"
                    searchKey.contains("cereal") -> "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?w=150&auto=format&fit=crop&q=60"
                    searchKey.contains("apple") -> "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=150&auto=format&fit=crop&q=60"
                    searchKey.contains("milk") -> "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=150&auto=format&fit=crop&q=60"
                    searchKey.contains("soup") -> "https://images.unsplash.com/photo-1547592165-e1d17fed6006?w=150&auto=format&fit=crop&q=60"
                    searchKey.contains("pasta") || searchKey.contains("spaghetti") -> "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=150&auto=format&fit=crop&q=60"
                    else -> ""
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F7FC)),
                contentAlignment = Alignment.Center
            ) {
                if (resolvedUrl.isNotEmpty()) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val emoji = when (item.category.lowercase()) {
                        "proteins" -> "🐟"
                        "grains" -> "🌾"
                        "dairy" -> "🥛"
                        "produce" -> "🍎"
                        "canned goods" -> "🥫"
                        else -> "📦"
                    }
                    Text(text = emoji, fontSize = 28.sp)
                }
            }

            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101A2C),
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stock: ${item.quantity}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Exp: ${item.daysToExpiry}d",
                    fontSize = 11.sp,
                    color = if (item.daysToExpiry <= 14) Color.Red else Color.Gray
                )
            }

            // Quick Tap Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onClaimClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101A2C)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = item.quantity > 0,
                    modifier = Modifier.weight(0.6f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text(
                        text = if (item.quantity > 0) "Claim" else "Out",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onReportClick,
                    border = BorderStroke(1.dp, Color(0xFFFF3B30)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.4f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Report", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
