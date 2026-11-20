package com.example.mathwake.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.mathwake.R
import com.example.mathwake.data.MathDifficulty
import com.example.mathwake.ui.theme.HardBlue
import com.example.mathwake.ui.theme.MintSuccess
import com.example.mathwake.ui.theme.OrangePrimary

@Composable
fun MathWakeLogoBadge(
    size: Dp = 36.dp,
    withOrangeBackground: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (withOrangeBackground) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD15C), Color(0xFFFF9A34))
                        ),
                        shape = RoundedCornerShape(size * 0.28f)
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(if (withOrangeBackground) size * 0.78f else size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w * 0.5f, h * 0.55f)
            val radius = w * 0.36f
            val navy = Color(0xFF222B45)
            val strokeW = w * 0.068f

            // Bells
            drawCircle(
                color = Color(0xFFFFF3B8),
                radius = radius * 0.32f,
                center = Offset(w * 0.24f, h * 0.24f)
            )
            drawCircle(
                color = navy,
                radius = radius * 0.32f,
                center = Offset(w * 0.24f, h * 0.24f),
                style = Stroke(width = strokeW * 0.85f)
            )
            drawCircle(
                color = Color(0xFFFFF3B8),
                radius = radius * 0.32f,
                center = Offset(w * 0.76f, h * 0.24f)
            )
            drawCircle(
                color = navy,
                radius = radius * 0.32f,
                center = Offset(w * 0.76f, h * 0.24f),
                style = Stroke(width = strokeW * 0.85f)
            )

            // Clock Face
            drawCircle(color = Color(0xFFFFF8EE), radius = radius, center = center)
            drawCircle(color = navy, radius = radius, center = center, style = Stroke(width = strokeW))

            // Math operators
            val opStroke = w * 0.055f
            // Plus (top-left)
            val pX = center.x - radius * 0.34f
            val pY = center.y - radius * 0.30f
            val pR = radius * 0.17f
            drawLine(navy, Offset(pX - pR, pY), Offset(pX + pR, pY), opStroke, StrokeCap.Round)
            drawLine(navy, Offset(pX, pY - pR), Offset(pX, pY + pR), opStroke, StrokeCap.Round)

            // Multiply (top-right)
            val mX = center.x + radius * 0.34f
            val mY = center.y - radius * 0.30f
            val mR = radius * 0.14f
            drawLine(OrangePrimary, Offset(mX - mR, mY - mR), Offset(mX + mR, mY + mR), opStroke, StrokeCap.Round)
            drawLine(OrangePrimary, Offset(mX + mR, mY - mR), Offset(mX - mR, mY + mR), opStroke, StrokeCap.Round)

            // Minus (bottom-left)
            val sX = center.x - radius * 0.34f
            val sY = center.y + radius * 0.32f
            drawLine(MintSuccess, Offset(sX - pR, sY), Offset(sX + pR, sY), opStroke, StrokeCap.Round)

            // Divide (bottom-right)
            val dX = center.x + radius * 0.34f
            val dY = center.y + radius * 0.32f
            drawLine(HardBlue, Offset(dX - pR, dY), Offset(dX + pR, dY), opStroke, StrokeCap.Round)
            drawCircle(HardBlue, radius = opStroke * 0.55f, center = Offset(dX, dY - pR * 0.85f))
            drawCircle(HardBlue, radius = opStroke * 0.55f, center = Offset(dX, dY + pR * 0.85f))
        }
    }
}

fun difficultyColor(difficulty: MathDifficulty): Color = when (difficulty) {
    MathDifficulty.EASY -> MintSuccess
    MathDifficulty.MEDIUM -> OrangePrimary
    MathDifficulty.HARD -> HardBlue
}

@Composable
fun difficultyName(difficulty: MathDifficulty): String = when (difficulty) {
    MathDifficulty.EASY -> stringResource(R.string.easy)
    MathDifficulty.MEDIUM -> stringResource(R.string.medium)
    MathDifficulty.HARD -> stringResource(R.string.hard)
}

@Composable
fun DifficultyBadge(
    difficulty: MathDifficulty,
    modifier: Modifier = Modifier
) {
    val accent = difficultyColor(difficulty)
    Row(
        modifier = modifier
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, CircleShape)
        )
        Text(
            text = difficultyName(difficulty),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

@Composable
fun DeleteAlarmDialog(
    alarmLabel: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.delete_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.delete_confirm_desc, alarmLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onConfirmDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.delete_confirm_yes),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}