package com.pentolrebus.kasir.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import com.pentolrebus.kasir.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.pentolrebus.kasir.domain.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Nav(val open: (String, String?) -> Unit, val back: () -> Unit)

fun rp(value: Long): String {
    val s = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(kotlin.math.abs(value))
    return (if (value < 0) "-Rp " else "Rp ") + s
}
fun fmtTime(ms: Long): String = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(ms))
fun fmtDate(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(ms))
fun fmtDateTime(ms: Long): String = SimpleDateFormat("d MMM · HH:mm", Locale("id", "ID")).format(Date(ms))
fun fmtMonth(ms: Long): String = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date(ms))
fun startOfDay(ms: Long): Long = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
fun startOfMonth(ms: Long): Long = Calendar.getInstance().apply { timeInMillis = startOfDay(ms); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
fun shortId(id: String) = "#" + id.takeLast(4).uppercase()

/** Global touch feedback helpers. Clip first so bounded ripple follows the element shape. */
fun Modifier.kRoundedClickable(shape: Shape, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clip(shape).clickable(enabled = enabled, onClick = onClick)

fun Modifier.kCircularClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clip(CircleShape).clickable(enabled = enabled, onClick = onClick)

fun Modifier.kTextClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clickable(
        enabled = enabled,
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )


private const val HTML_REM_DP = 17.6f // 1rem from html{font-size:110%} (16px × 1.10)
private val CardShape = RoundedCornerShape((HTML_REM_DP).dp)
private val ButtonShape = RoundedCornerShape((HTML_REM_DP * .875f).dp)
private val FieldShape = RoundedCornerShape((HTML_REM_DP * .75f).dp)
private val PillShape = RoundedCornerShape((HTML_REM_DP * 1.188f).dp)

@Composable
fun KPage(title: String, nav: Nav?, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}, fab: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = (HTML_REM_DP * .625f).dp, end = (HTML_REM_DP * .75f).dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (nav != null) {
                Surface(
                    Modifier.size(44.dp).kCircularClickable { nav.back() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_back), "Kembali", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            } else Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                Text(title, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            actions()
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(start = HTML_REM_DP.dp, top = (HTML_REM_DP * .25f).dp, end = HTML_REM_DP.dp, bottom = if (fab != null) 96.dp else (HTML_REM_DP * .25f).dp),
                verticalArrangement = Arrangement.spacedBy((HTML_REM_DP * .5f).dp),
                content = content
            )
            if (fab != null) {
                Surface(
                    Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp).size(62.dp).kCircularClickable { fab() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun KCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = CardShape, color = color) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), content = content)
    }
}

@Composable
fun KRow(label: String, value: String, bold: Boolean = false, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun KLabel(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp, bottom = 1.dp))
}

@Composable
fun KPrimary(label: String, enabled: Boolean = true, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = (HTML_REM_DP * 3.0f).dp),
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .45f))
    ) { Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
}

@Composable
fun KSecondary(label: String, danger: Boolean = false, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    val c = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = (HTML_REM_DP * 3.0f).dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer, contentColor = c),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
    ) { Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
}

@Composable
fun KField(label: String, value: String, onValue: (String) -> Unit, number: Boolean = false, enabled: Boolean = true, placeholder: String = "", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
        Surface(Modifier.fillMaxWidth().heightIn(min = (HTML_REM_DP * 3.125f).dp), shape = FieldShape, color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant) {
            BasicTextField(
                value = value,
                onValueChange = { onValue(if (number) it.filter(Char::isDigit) else it) },
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    inner()
                }
            )
        }
    }
}

@Composable
fun KSecretField(label: String, value: String, onValue: (String) -> Unit, numeric: Boolean = false) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
        Surface(Modifier.fillMaxWidth().heightIn(min = (HTML_REM_DP * 3.125f).dp), shape = FieldShape, color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = { onValue(if (numeric) it.filter(Char::isDigit).take(6) else it) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password),
                    decorationBox = { inner ->
                        if (value.isEmpty()) Text(if (numeric) "Masukkan PIN" else "Masukkan password", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    }
                )
                IconButton(onClick = { show = !show }, modifier = Modifier.size(44.dp)) {
                    Icon(painterResource(R.drawable.ic_eye), if (show) "Sembunyikan" else "Tampilkan", tint = if (show) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun KSearchField(value: String, onValue: (String) -> Unit, placeholder: String = "Cari…") {
    Surface(Modifier.fillMaxWidth(), shape = PillShape, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_search), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge); inner() }
            )
            if (value.isNotEmpty()) TextButton(onClick = { onValue("") }, contentPadding = PaddingValues(0.dp)) { Text("×", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun KChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            Surface(
                Modifier.heightIn(min = (HTML_REM_DP * 2.375f).dp).kRoundedClickable(PillShape) { onSelect(o) },
                shape = PillShape,
                color = if (selected == o) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            ) {
                Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(o, color = if (selected == o) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun KSegmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = PillShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(4.dp)) {
            options.forEach { o ->
                Surface(
                    Modifier.weight(1f).height((HTML_REM_DP * 2.25f).dp).kRoundedClickable(RoundedCornerShape((HTML_REM_DP * .9375f).dp)) { onSelect(o) },
                    shape = RoundedCornerShape((HTML_REM_DP * .9375f).dp),
                    color = if (selected == o) MaterialTheme.colorScheme.primary else Color.Transparent
                ) { Box(contentAlignment = Alignment.Center) { Text(o, color = if (selected == o) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) } }
            }
        }
    }
}

@Composable
fun KToggle(checked: Boolean, onChecked: (Boolean) -> Unit) {
    val track = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val knob = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(Modifier.size(width = 48.dp, height = 26.dp).kRoundedClickable(PillShape) { onChecked(!checked) }, shape = PillShape, color = track) {
        Box(Modifier.fillMaxSize().padding(3.dp)) {
            Surface(Modifier.size(20.dp).align(if (checked) Alignment.CenterEnd else Alignment.CenterStart), shape = CircleShape, color = knob) {}
        }
    }
}

@Composable
fun KSwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    KCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            KToggle(checked, onChecked)
        }
    }
}

@Composable
fun KListGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 14.dp), content = content)
    }
}

@Composable
fun KListItem(title: String, subtitle: String = "", trailing: String = "", trailingColor: Color = MaterialTheme.colorScheme.onSurface, icon: Int? = null, onClick: (() -> Unit)? = null, divider: Boolean = true) {
    val m = if (onClick != null) Modifier.fillMaxWidth().kRoundedClickable(RoundedCornerShape(12.dp), onClick = onClick) else Modifier.fillMaxWidth()
    Column {
        Row(m.heightIn(min = 52.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
            }
                    Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.6.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 17.6.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (trailing.isNotEmpty()) Text(trailing, color = trailingColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            if (onClick != null) Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun KItem(title: String, subtitle: String = "", trailing: String = "", trailingColor: Color = MaterialTheme.colorScheme.onSurface, icon: Int? = null, onClick: (() -> Unit)? = null) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface) {
        KListItem(title, subtitle, trailing, trailingColor, icon, onClick, divider = false)
    }
}

@Composable
fun KEmpty(title: String, subtitle: String, icon: Int, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(68.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        if (actionLabel != null && onAction != null) { Spacer(Modifier.height(14.dp)); KPrimary(actionLabel, modifier = Modifier.widthIn(min = 200.dp), onClick = onAction) }
    }
}

@Composable
fun KStat(title: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun KHero(label: String, value: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
            Text(value, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun KConfirm(title: String, text: String, confirm: String, danger: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } })
}

fun syncLabel(s: SyncStatus) = when (s) { SyncStatus.SYNCED -> "Tersinkron"; SyncStatus.PENDING_SYNC -> "Menunggu sinkron"; SyncStatus.SYNC_ERROR -> "Gagal sinkron" }
