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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverdas.app.db.ShiftEntity
import java.text.SimpleDateFormat
import java.util.*

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
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: DashboardViewModel = viewModel()
                    DashboardScreen(
                        viewModel = viewModel,
                        onPermissionRequest = { requestInitialPermissions() }
                    )
                }
            }
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
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onPermissionRequest: () -> Unit) {
    val isTracking by viewModel.isTracking.collectAsState()
    val mileage by viewModel.currentMileage.collectAsState()
    val pastShifts by viewModel.pastShifts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Driver DAS", style = MaterialTheme.typography.displaySmall)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isTracking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Current Shift Mileage", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${"%.2f".format(mileage)} mi", 
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                onPermissionRequest()
                viewModel.toggleShift()
            },
            modifier = Modifier.fillMaxWidth(0.7f).height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color.Red else Color.Green
            )
        ) {
            Icon(if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isTracking) "Stop Shift" else "Start Shift")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Recent History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Start))
        
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(pastShifts) { shift ->
                ShiftItem(shift)
            }
        }
    }
}

@Composable
fun ShiftItem(shift: ShiftEntity) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    ListItem(
        headlineContent = { Text("${"%.2f".format(shift.totalMiles)} miles") },
        supportingContent = { Text(dateFormat.format(Date(shift.startTime))) },
        trailingContent = { Text("$${"%.2f".format(shift.earnings)}") }
    )
}
