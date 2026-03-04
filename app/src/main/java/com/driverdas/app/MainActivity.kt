package com.driverdas.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverdas.app.db.ShiftEntity
import java.text.SimpleDateFormat
import java.util.*

// --- Midnight Theme Colors ---
private val MidnightBlack = Color(0xFF0A0E14)
private val DeepCharcoal = Color(0xFF161B22)
private val NeonBlue = Color(0xFF00D2FF)
private val NeonGreen = Color(0xFF39FF14)
private val GlassWhite = Color(0x1AFFFFFF)
private val AlertRed = Color(0xFFFF3131)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkBackgroundLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.log(this, "MainActivity", "App Launched")
        setContent {
            DashboardApp()
        }
    }

    private fun requestInitialPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Enable 'Allow all the time' for background tracking", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
    }

    @Composable
    fun DashboardApp() {
        val viewModel: DashboardViewModel = viewModel()
        
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = NeonBlue,
                background = MidnightBlack,
                surface = DeepCharcoal,
                onBackground = Color.White,
                onSurface = Color.White
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MidnightBlack) {
                DashboardScreen(
                    viewModel = viewModel,
                    onPermissionRequest = { requestInitialPermissions() }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onPermissionRequest: () -> Unit) {
    val isTracking by viewModel.isTracking.collectAsState()
    val mileage by viewModel.currentMileage.collectAsState()
    val pastShifts by viewModel.pastShifts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar Area
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DRIVER DAS",
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonBlue
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = { viewModel.exportData() }) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = NeonGreen)
            }
            IconButton(onClick = { viewModel.clearLogs() }) {
                Icon(Icons.Default.BugReport, contentDescription = "Clear Logs", tint = Color.Gray)
            }
        }

        // --- Main Gauge Area ---
        Box(
            modifier = Modifier.size(280.dp).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.fillMaxSize(),
                color = GlassWhite,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )
            
            val animatedProgress by animateFloatAsState(
                targetValue = (mileage % 10 / 10).toFloat(),
                animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                label = "progress"
            )
            
            CircularProgressIndicator(
                progress = if (isTracking) animatedProgress else 0f,
                modifier = Modifier.fillMaxSize(),
                color = NeonBlue,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )

            // --- Active Pulse Icon ---
            if (isTracking) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AlertRed.copy(alpha = alpha))
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.2f".format(mileage),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 64.sp
                    )
                )
                Text(
                    "MILES TRACKED",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Gray,
                        letterSpacing = 2.sp
                    )
                )
            }
        }

        // --- Stats Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DeepCharcoal)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                label = "DEDUCTION",
                value = "$${"%.2f".format(TaxConfig.calculateDeduction(mileage))}",
                color = NeonGreen
            )
            Box(modifier = Modifier.height(40.dp).width(1.dp).background(GlassWhite))
            StatItem(
                label = "STATUS",
                value = if (isTracking) "TRACKING" else "IDLE",
                color = if (isTracking) NeonBlue else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Main Action Button ---
        Button(
            onClick = {
                onPermissionRequest()
                viewModel.toggleShift()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(2.dp, if (isTracking) AlertRed.copy(alpha = 0.5f) else NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color.Transparent else NeonBlue.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(
                if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isTracking) AlertRed else NeonBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                if (isTracking) "STOP DELIVERY" else "START DELIVERY",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isTracking) AlertRed else NeonBlue
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- History List ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "RECENT TRIPS",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.Gray, letterSpacing = 1.sp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pastShifts) { shift ->
                MidnightShiftItem(shift)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = color))
    }
}

@Composable
fun MidnightShiftItem(shift: ShiftEntity) {
    val dateFormat = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(dateFormat.format(Date(shift.startTime)), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            Text("${"%.2f".format(shift.totalMiles)} Miles", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
        Text(
            "+$${"%.2f".format(shift.earnings)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NeonGreen)
        )
    }
}
