package com.pentolrebus.kasir.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pentolrebus.kasir.domain.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Navigation callbacks handed to every management screen. */
class Nav(val open: (String, String?) -> Unit, val back: () -> Unit)

fun rp(value: Long): String {
    val s = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(Math.abs(value))
    return (if (value < 0) "-Rp " else "Rp ") + s
}
fun fmtTime(ms: Long): String = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(ms))
fun fmtDate(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(ms))
fun fmtDateTime(ms: Long): String = SimpleDateFormat("d MMM · HH:mm", Locale("id", "ID")).format(Date(ms))
fun fmtMonth(ms: Long): String = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date(ms))
fun startOfDay(ms: Long): Long = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
fun startOfMonth(ms: Long): Long = Calendar.getInstance().apply { timeInMillis = startOfDay(ms); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
fun shortId(id: String) = "#" + id.takeLast(4).uppercase()

@Composable
fun KPage(title: String, nav: Nav?, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (nav != null) IconButton(onClick = nav.back, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.ArrowBack, "Kembali", modifier = Modifier.size(21.dp)) }
            else Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            actions()
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun KCard(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = color) { Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), content = content) }
}

@Composable
fun KRow(label: String, value: String, bold: Boolean = false, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable fun KLabel(t: String) { Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }

@Composable
fun KPrimary(label: String, enabled: Boolean = true, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .45f))) { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

@Composable
fun KSecondary(label: String, danger: Boolean = false, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    val c = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Button(onClick = onClick, modifier = modifier.heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (danger) MaterialTheme.colorScheme.error.copy(alpha = .10f) else MaterialTheme.colorScheme.primaryContainer, contentColor = c), elevation = null) { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

@Composable
fun KField(label: String, value: String, onValue: (String) -> Unit, number: Boolean = false, enabled: Boolean = true, placeholder: String = "", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
        Surface(Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp), color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant) {
            BasicTextField(value = value, onValueChange = { onValue(if (number) it.filter(Char::isDigit) else it) }, enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold), keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text), decorationBox = { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                inner()
            })
        }
    }
}

@Composable
fun KSecretField(label: String, value: String, onValue: (String) -> Unit, numeric: Boolean = true) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
        OutlinedTextField(value = value, onValueChange = { onValue(if (numeric) it.filter(Char::isDigit).take(6) else it) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp), visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password), trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (show) "Sembunyikan" else "Tampilkan") } }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline))
    }
}

@Composable
fun KChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            Surface(Modifier.heightIn(min = 38.dp).clickable { onSelect(o) }, shape = RoundedCornerShape(20.dp), color = if (selected == o) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface) {
                Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { Text(o, color = if (selected == o) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun KSegmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) {
        Row(Modifier.padding(4.dp)) { options.forEach { o ->
            Surface(Modifier.weight(1f).height(38.dp).clickable { onSelect(o) }, shape = RoundedCornerShape(16.dp), color = if (selected == o) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent) {
                Box(contentAlignment = Alignment.Center) { Text(o, color = if (selected == o) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
            }
        } }
    }
}

@Composable
fun KSwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) { Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(checked = checked, onCheckedChange = onChecked) } }
}

@Composable
fun KItem(title: String, subtitle: String = "", trailing: String = "", trailingColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, icon: Int? = null, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(m.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Surface(Modifier.size(38.dp), shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) } }; Spacer(Modifier.width(11.dp)) }
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium); if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (trailing.isNotEmpty()) Text(trailing, color = trailingColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            if (onClick != null) Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun KEmpty(title: String, subtitle: String, icon: Int, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(icon), null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) { Spacer(Modifier.height(14.dp)); KPrimary(actionLabel, modifier = Modifier.widthIn(min = 200.dp), onClick = onAction) }
    }
}

@Composable
fun KStat(title: String, value: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun KHero(label: String, value: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
            Text(value, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun KConfirm(title: String, text: String, confirm: String, danger: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("BATAL") } })
}

fun syncLabel(s: SyncStatus) = when (s) { SyncStatus.SYNCED -> "Tersinkron"; SyncStatus.PENDING_SYNC -> "Menunggu sinkron"; SyncStatus.SYNC_ERROR -> "Gagal sinkron" }
