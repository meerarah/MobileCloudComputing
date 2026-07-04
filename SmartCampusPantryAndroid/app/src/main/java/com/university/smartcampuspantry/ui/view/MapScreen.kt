package com.university.smartcampuspantry.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class PantryLocation(
    val name: String,
    val campus: String,
    val geoPoint: GeoPoint,
    val directions: List<String>
)

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = java.io.File(context.cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = java.io.File(context.cacheDir, "osmdroid/tiles")
    }

    val locationsList = listOf(
        PantryLocation("Kolej Mawar Hub", "UiTM Shah Alam", GeoPoint(3.0683, 101.5018), listOf("1. Walk to the main lounge at Kolej Mawar", "2. The community pantry shelf is located next to the main office"))
    )

    var selectedIndex by remember { mutableStateOf(0) }
    val activeLoc = locationsList[selectedIndex]

    // Create MapView once
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }
    }

    // Handle lifecycle for the MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Animate camera and update marker when active location changes
    LaunchedEffect(activeLoc) {
        mapView.controller.animateTo(activeLoc.geoPoint)
        
        // Update Marker
        mapView.overlays.clear()
        val marker = Marker(mapView)
        marker.position = activeLoc.geoPoint
        marker.title = activeLoc.name
        marker.snippet = activeLoc.directions.firstOrNull()
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            locationsList.forEachIndexed { index, loc ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedIndex == index) Color(0xFF101A2C) else Color.White
                    ),
                    modifier = Modifier
                        .clickable { selectedIndex = index }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loc.name.replace("Kolej ", ""),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedIndex == index) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }

        // OpenStreetMap Container
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView }
                )
                
                // Overlay for directions
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🗺️ Route Instructions:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF101A2C))
                        activeLoc.directions.forEach { step ->
                            Text(step, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}
