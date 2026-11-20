package com.example.mathwake.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mathwake.R
import com.example.mathwake.alarm.AlarmService
import com.example.mathwake.data.MathDifficulty
import com.example.mathwake.logic.MathProblemGenerator
import com.example.mathwake.ui.components.MathWakeLogoBadge
import com.example.mathwake.ui.theme.MintSuccess
import com.example.mathwake.ui.theme.OrangePrimary
import com.example.mathwake.ui.theme.RingingHeaderNavy
import java.util.Locale

@Composable
fun AlarmRingingScreen(
    alarmId: Int,
    hour: Int,
    minute: Int,
    label: String,
    difficulty: MathDifficulty,
    onStopAlarmService: () -> Unit,
    onAlarmDismissed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // เก็บโจทย์และคำตอบด้วย rememberSaveable เพื่อให้หมุนจอหรือสลับแอปแล้วสถานะเดิมยังอยู่ครบ
    var question by rememberSaveable(alarmId, difficulty) {
        val generated = MathProblemGenerator.generateProblem(difficulty)
        mutableStateOf(generated.question)
    }
    var correctAnswer by rememberSaveable(alarmId, difficulty) {
        // สร้างซ้ำให้ตรงกับ question ด้านบนแบบผูกคู่กันผ่าน state เดียว
        mutableIntStateOf(extractOrComputeAnswer(question))
    }
    var currentAnswer by rememberSaveable(alarmId) { mutableStateOf("") }
    var isError by rememberSaveable(alarmId) { mutableStateOf(false) }
    var isSolved by rememberSaveable(alarmId) { mutableStateOf(false) }

    // บล็อกปุ่ม Back ระหว่างที่ยังแก้โจทย์ไม่ผ่าน; ถ้าผ่านแล้วกด Back จะถือว่ากดเสร็จสิ้น
    BackHandler {
        if (isSolved) {
            onAlarmDismissed()
        }
    }

    // หยุดเสียง/การสั่นชั่วคราวเมื่อสลับไปแอปอื่น และกลับมาทำงานต่อจากสถานะเดิมเมื่อเปิดกลับมา
    DisposableEffect(lifecycleOwner, isSolved) {
        val observer = LifecycleEventObserver { _, event ->
            if (isSolved) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (activity?.isChangingConfigurations != true && AlarmService.isAlarmActive) {
                        context.startService(
                            Intent(context, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_PAUSE
                            }
                        )
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (AlarmService.isAlarmActive) {
                        context.startService(
                            Intent(context, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_RESUME
                            }
                        )
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val timeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    val displayLabel = label.ifBlank { stringResource(R.string.default_alarm_label) }

    if (isSolved) {
        MissionCompleteView(
            problemExpression = question.removeSuffix("= ?").trim(),
            answerText = correctAnswer.toString(),
            isLandscape = isLandscape,
            onDone = onAlarmDismissed
        )
        return
    }

    val onDigitPress: (String) -> Unit = { digit ->
        if (currentAnswer.length < 6) {
            currentAnswer += digit
            isError = false
        }
    }
    val onBackspace: () -> Unit = {
        if (currentAnswer.isNotEmpty()) {
            currentAnswer = currentAnswer.dropLast(1)
            isError = false
        }
    }
    val onSubmit: () -> Unit = {
        if (currentAnswer.toIntOrNull() == correctAnswer) {
            isError = false
            isSolved = true
            onStopAlarmService()
        } else {
            isError = true
            currentAnswer = ""
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(RingingHeaderNavy)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Left Pane: Clock + Problem
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RingingHeaderContent(timeText = timeText, label = displayLabel, compact = true)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusOrErrorSection(isError = isError, compact = true)
                        ProblemExpressionBox(question = question)
                        AnswerDisplayBox(currentAnswer = currentAnswer)
                    }
                }
            }

            // Right Pane: Numpad + Submit
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MathNumpad(
                        onDigit = onDigitPress,
                        onBackspace = onBackspace,
                        onClear = { currentAnswer = ""; isError = false },
                        buttonHeight = 46.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SubmitAnswerButton(enabled = currentAnswer.isNotEmpty(), onClick = onSubmit)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RingingHeaderNavy)
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                RingingHeaderContent(timeText = timeText, label = displayLabel, compact = false)
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatusOrErrorSection(isError = isError, compact = false)
                    ProblemExpressionBox(question = question)
                    AnswerDisplayBox(currentAnswer = currentAnswer)
                    MathNumpad(
                        onDigit = onDigitPress,
                        onBackspace = onBackspace,
                        onClear = { currentAnswer = ""; isError = false },
                        buttonHeight = 52.dp
                    )
                    SubmitAnswerButton(enabled = currentAnswer.isNotEmpty(), onClick = onSubmit)
                    Text(
                        text = stringResource(R.string.ringing_footer_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RingingHeaderContent(
    timeText: String,
    label: String,
    compact: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.ringing_badge),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFD180),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
        Text(
            text = timeText,
            fontSize = if (compact) 38.sp else 52.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun StatusOrErrorSection(isError: Boolean, compact: Boolean) {
    if (isError) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.incorrect_answer_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.incorrect_answer_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MathWakeLogoBadge(size = if (compact) 36.dp else 44.dp, withOrangeBackground = false)
            Column {
                Text(
                    text = stringResource(R.string.wake_brain_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.solve_to_dismiss),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProblemExpressionBox(question: String) {
    val leftPart = question.removeSuffix("?").trim()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = leftPart,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun AnswerDisplayBox(currentAnswer: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = currentAnswer.ifEmpty { stringResource(R.string.enter_answer) },
                fontSize = if (currentAnswer.isEmpty()) 16.sp else 26.sp,
                fontWeight = if (currentAnswer.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                color = if (currentAnswer.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun MathNumpad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    buttonHeight: androidx.compose.ui.unit.Dp
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key == "C" || key == "⌫"
                    Button(
                        onClick = {
                            when (key) {
                                "C" -> onClear()
                                "⌫" -> onBackspace()
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAction) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = key,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitAnswerButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = stringResource(R.string.submit_answer),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MissionCompleteView(
    problemExpression: String,
    answerText: String,
    isLandscape: Boolean,
    onDone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SuccessClockBadge(size = 96.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.mission_complete_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintSuccess
                    )
                    Text(
                        text = stringResource(R.string.wake_up_message),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.wake_up_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    MissionSummaryCard(problemExpression, answerText)
                    DoneMintButton(onClick = onDone)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SuccessClockBadge(size = 132.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.mission_complete_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintSuccess,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = stringResource(R.string.wake_up_message),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.wake_up_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MissionSummaryCard(problemExpression, answerText)
                }
                DoneMintButton(onClick = onDone)
            }
        }
    }
}

@Composable
private fun SuccessClockBadge(size: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .background(MintSuccess.copy(alpha = 0.16f), CircleShape)
        )
        MathWakeLogoBadge(size = size * 0.72f, withOrangeBackground = false)
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MintSuccess,
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

@Composable
private fun MissionSummaryCard(problemExpression: String, answerText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.summary_problem),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = problemExpression,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MintSuccess,
                modifier = Modifier.size(28.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.summary_your_answer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = answerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MintSuccess
                )
            }
        }
    }
}

@Composable
private fun DoneMintButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MintSuccess,
            contentColor = Color.White
        )
    ) {
        Text(
            text = stringResource(R.string.done_btn),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun extractOrComputeAnswer(question: String): Int {
    val expr = question.removeSuffix("= ?").trim()
    val tokens = expr.split(" ")
    return try {
        when {
            tokens.size == 3 && tokens[1] == "+" -> tokens[0].toInt() + tokens[2].toInt()
            tokens.size == 3 && tokens[1] == "-" -> tokens[0].toInt() - tokens[2].toInt()
            tokens.size == 3 && tokens[1] == "×" -> tokens[0].toInt() * tokens[2].toInt()
            tokens.size == 3 && tokens[1] == "÷" -> tokens[0].toInt() / tokens[2].toInt()
            tokens.size == 5 && tokens[1] == "+" && tokens[3] == "+" ->
                tokens[0].toInt() + tokens[2].toInt() + tokens[4].toInt()
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}