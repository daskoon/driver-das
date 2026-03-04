package com.driverdas.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        composeView = ComposeView(this).apply {
            val viewModelStore = ViewModelStore()
            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = viewModelStore
            })
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                var isExpanded by remember { mutableStateOf(false) }
                val mileage by LocationService.mileageFlow.collectAsState()
                
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                params.x += dragAmount.x.toInt()
                                params.y += dragAmount.y.toInt()
                                try {
                                    windowManager.updateViewLayout(composeView, params)
                                } catch (e: Exception) {}
                            }
                        }
                ) {
                    if (isExpanded) {
                        ExpandedCard(mileage = mileage, onCollapse = { isExpanded = false })
                    } else {
                        FloatingBubble(onExpand = { isExpanded = true })
                    }
                }
            }
        }

        // Use modern overlay type directly as minSdk is 26
        val layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        windowManager.addView(composeView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(composeView)
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingBubble(onExpand: () -> Unit) {
    Button(
        onClick = onExpand,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text("TAX")
    }
}

@Composable
fun ExpandedCard(mileage: Double, onCollapse: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mileage", style = MaterialTheme.typography.labelMedium)
            Text("${"%.2f".format(mileage)} mi", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Deduction", style = MaterialTheme.typography.labelSmall)
            Text("$${"%.2f".format(mileage * 0.67)}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onCollapse, modifier = Modifier.fillMaxWidth()) {
                Text("Hide", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

class MyLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}
