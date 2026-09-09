package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// Classic Chrome T-Rex 16x16 pixel matrix
// 1 = white pixel, 0 = black / empty
val DINO_SPRITE = arrayOf(
    "0000000001111110",
    "0000000001011111",
    "0000000001111111",
    "0000000001110000",
    "0000000001111100",
    "0000000011110000",
    "0000001111111100",
    "0001111111110000",
    "0011111111110000",
    "0011111111110000",
    "0011111111100000",
    "0001111111000000",
    "0000111110000000",
    "0000110110000000",
    "0000100010000000",
    "0000110011000000"
)

// Cactus obstacle pixel matrix (9x16)
val CACTUS_SPRITE = arrayOf(
    "000011000",
    "000011000",
    "011011000",
    "011011011",
    "011011011",
    "011111111",
    "001111110",
    "000011000",
    "000011000",
    "000011000",
    "000011000",
    "000011000",
    "000011000",
    "000011000",
    "000011000",
    "000011000"
)

data class Cactus(
    var x: Float,
    val width: Float = 28f,
    val height: Float = 48f
)

@Composable
fun DinoGame(
    modifier: Modifier = Modifier,
    onGameOver: () -> Unit
) {
    var dinoY by remember { mutableFloatStateOf(0f) }
    var dinoVelocityY by remember { mutableFloatStateOf(0f) }
    var isJumping by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var isDead by remember { mutableStateOf(false) }

    val cacti = remember { mutableStateListOf<Cactus>() }

    // Game loop
    LaunchedEffect(isDead) {
        if (isDead) return@LaunchedEffect

        var lastFrameTime = 0L
        var spawnTimer = 0f
        var speed = 360f // pixels per second

        while (!isDead) {
            withFrameNanos { timeNanos ->
                if (lastFrameTime == 0L) {
                    lastFrameTime = timeNanos
                    return@withFrameNanos
                }

                val dt = (timeNanos - lastFrameTime) / 1_000_000_000f
                lastFrameTime = timeNanos

                // Gravity & jumping
                if (isJumping || dinoY > 0f) {
                    dinoVelocityY -= 1500f * dt
                    dinoY += dinoVelocityY * dt
                    if (dinoY <= 0f) {
                        dinoY = 0f
                        dinoVelocityY = 0f
                        isJumping = false
                    }
                }

                // Move obstacles
                val iterator = cacti.listIterator()
                while (iterator.hasNext()) {
                    val cactus = iterator.next()
                    cactus.x -= speed * dt

                    // Dino bounding box: x in [80..136], y in [dinoY .. dinoY + 54]
                    val dinoLeft = 80f
                    val dinoRight = 132f
                    val dinoBottom = dinoY
                    val dinoTop = dinoY + 52f

                    val cactusLeft = cactus.x
                    val cactusRight = cactus.x + cactus.width
                    val cactusBottom = 0f
                    val cactusTop = cactus.height

                    // AABB Collision check
                    val horizontalOverlap = dinoRight > cactusLeft + 4f && dinoLeft + 6f < cactusRight
                    val verticalOverlap = dinoBottom < cactusTop - 4f

                    if (horizontalOverlap && verticalOverlap) {
                        isDead = true
                        onGameOver()
                        return@withFrameNanos
                    }

                    if (cactus.x + cactus.width < 0f) {
                        iterator.remove()
                        score += 10
                        speed += 6f
                    }
                }

                // Spawn obstacles
                spawnTimer += dt
                if (spawnTimer >= 1.6f && cacti.size < 2) {
                    cacti.add(Cactus(x = 900f + Random.nextInt(0, 200).toFloat()))
                    spawnTimer = 0f
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isJumping && dinoY <= 2f && !isDead) {
                    dinoVelocityY = 620f
                    isJumping = true
                }
            }
            .testTag("dino_game_canvas")
    ) {
        val groundY = maxHeight.value * 0.72f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "HI  " + score.toString().padStart(5, '0'),
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TAP TO JUMP",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height
            val baseGroundY = canvasHeight * 0.72f

            // Ground line
            drawLine(
                color = Color.White,
                start = Offset(0f, baseGroundY),
                end = Offset(size.width, baseGroundY),
                strokeWidth = 2.dp.toPx()
            )

            // Draw Dino using precise 16x16 pixel matrix
            val dinoPixelSize = 3.4f
            val dinoBaseX = 80f
            val dinoCurrentBaseY = baseGroundY - dinoY

            for (row in DINO_SPRITE.indices) {
                val rowString = DINO_SPRITE[row]
                for (col in rowString.indices) {
                    if (rowString[col] == '1') {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(
                                x = dinoBaseX + col * dinoPixelSize,
                                y = dinoCurrentBaseY - (16 - row) * dinoPixelSize
                            ),
                            size = Size(dinoPixelSize, dinoPixelSize)
                        )
                    }
                }
            }

            // Draw Cacti using precise 9x16 pixel matrix
            val cactusPixelSize = 3.0f
            for (cactus in cacti) {
                for (row in CACTUS_SPRITE.indices) {
                    val rowString = CACTUS_SPRITE[row]
                    for (col in rowString.indices) {
                        if (rowString[col] == '1') {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(
                                    x = cactus.x + col * cactusPixelSize,
                                    y = baseGroundY - (16 - row) * cactusPixelSize
                                ),
                                size = Size(cactusPixelSize, cactusPixelSize)
                            )
                        }
                    }
                }
            }
        }
    }
}
