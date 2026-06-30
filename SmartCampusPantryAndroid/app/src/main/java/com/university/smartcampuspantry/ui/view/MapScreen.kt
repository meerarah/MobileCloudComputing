package com.university.smartcampuspantry.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PantryLocation(
    val name: String,
    val campus: String,
    val coordinates: String,
    val directions: List<String>
)

@Composable
fun MapScreen() {
    val locationsList = listOf(
        PantryLocation(
            name = "Kolej Perindu Hub",
            campus = "UiTM Shah Alam Campus (Block C)",
            coordinates = "Lat: 3.0697° N | Lng: 101.5037° E",
            directions = listOf(
                "1. Head east past Kolej Mawar gates",
                "2. Turn left towards Perindu residential block",
                "3. Station is active in Block C lounge area"
            )
        ),
        PantryLocation(
            name = "Kolej Mawar Hub",
            campus = "UiTM Shah Alam Campus (Main Foyer)",
            coordinates = "Lat: 3.0681° N | Lng: 101.5015° E",
            directions = listOf(
                "1. Go past the Mawar administration office",
                "2. Turn right toward the student activity area",
                "3. Pantry shelf is next to the cafeteria entrance"
            )
        ),
        PantryLocation(
            name = "Kolej Mawar Hub 2",
            campus = "UiTM Shah Alam Campus (Block B Lobby)",
            coordinates = "Lat: 3.0712° N | Lng: 101.5055° E",
            directions = listOf(
                "1. Enter through Block B lobby main doors",
                "2. Head past the reception counter",
                "3. Restocking shelf is next to the study corner"
            )
        )
    )

    var selectedIndex by remember { mutableStateOf(0) }
    val activeLoc = locationsList[selectedIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Introduction header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "UiTM Campus Hub Locations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101A2C)
                )
                Text(
                    text = "Select a pantry distribution station below to check its coordinate routes:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Horizontal Selection Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            locationsList.forEachIndexed { index, loc ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedIndex == index) Color(0xFF101A2C) else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedIndex = index }
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loc.name.replace("Kolej ", "").replace(" Hub", ""),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedIndex == index) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }

        // Simulated Google Maps Container Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📍", fontSize = 48.sp)
                    Text(
                        text = activeLoc.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF101A2C)
                    )
                    Text(
                        text = activeLoc.campus,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = activeLoc.coordinates,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🗺️ Route Instructions:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF101A2C))
                            activeLoc.directions.forEach { step ->
                                Text(step, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
