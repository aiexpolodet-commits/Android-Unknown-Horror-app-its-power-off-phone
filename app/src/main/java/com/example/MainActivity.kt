package com.example

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

enum class HorrorPhase {
    PITCH_BLACK,    // Initial 10 seconds of black screen
    CKC9_STAGE,     // White "ckc9?" text and "Разрешить" button
    DINO_GAME,      // Dino runner game
    PHONE_OFF       // Screen turns completely black mimicking phone powered off
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                HorrorAppScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        HorrorSoundGenerator.stop()
    }
}

@Composable
fun HorrorAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPhase by remember { mutableStateOf(HorrorPhase.PITCH_BLACK) }

    // Request notification permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Graceful handling */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 10 seconds pure black screen
        delay(10_000L)

        // Native Android OS system alert dialog
        showNativeSystemAlertDialog(
            context = context,
            onOk = {
                currentPhase = HorrorPhase.CKC9_STAGE

                // Post system notification: "Hello, user."
                NotificationHelper.postNotification(
                    context = context,
                    notificationId = 1001,
                    title = "Unknown",
                    message = "Hello, user."
                )
                NotificationHelper.triggerHorrorVibration(context, isCrash = false)
            },
            onCancel = {
                // Cancel clicked -> roar and shut down
                currentPhase = HorrorPhase.PHONE_OFF
                triggerRoarAndPowerOff(context)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("horror_main_canvas"),
        contentAlignment = Alignment.Center
    ) {
        when (currentPhase) {
            HorrorPhase.PITCH_BLACK -> {
                // Pure black empty screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .testTag("pitch_black_screen")
                )
            }

            HorrorPhase.CKC9_STAGE -> {
                // Clean, white, minimalist typography and button on pitch black canvas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ckc9?",
                        color = Color.White,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("ckc9_text")
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            // Send "Error" system notification
                            NotificationHelper.postNotification(
                                context = context,
                                notificationId = 1002,
                                title = "Unknown",
                                message = "Error"
                            )
                            NotificationHelper.triggerHorrorVibration(context, isCrash = true)

                            // Start 30-second monster roar immediately
                            HorrorSoundGenerator.playMonsterRoar30s(context)

                            // Advance to Dino game
                            currentPhase = HorrorPhase.DINO_GAME
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .testTag("allow_button")
                            .width(180.dp)
                            .height(48.dp)
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "Разрешить",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            HorrorPhase.DINO_GAME -> {
                // Pixel-perfect Dino Runner mini game
                DinoGame(
                    onGameOver = {
                        // When dino dies, screen goes completely black (shutting off phone) and app closes
                        currentPhase = HorrorPhase.PHONE_OFF
                        triggerRoarAndPowerOff(context)
                    }
                )
            }

            HorrorPhase.PHONE_OFF -> {
                // Pitch black screen representing turned off phone
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .testTag("phone_off_screen")
                )
            }
        }
    }
}

/**
 * Invokes native Android OS system alert dialog (android.app.AlertDialog).
 */
private fun showNativeSystemAlertDialog(
    context: Context,
    onOk: () -> Unit,
    onCancel: () -> Unit
) {
    val activity = context as? Activity ?: return
    if (activity.isFinishing || activity.isDestroyed) return

    AlertDialog.Builder(activity)
        .setTitle("Warning")
        .setMessage("Allow application Unknown to use Dual Memory Parameters?")
        .setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onOk()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            onCancel()
        }
        .setCancelable(false)
        .show()
}

/**
 * Triggers full device power off commands / screen lock and roar.
 */
private fun triggerRoarAndPowerOff(context: Context) {
    HorrorSoundGenerator.playMonsterRoar30s(context)
    SystemPowerOffHelper.performPowerOff(context)
}

/**
 * Preserved for test backward compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}
