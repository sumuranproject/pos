package com.sakukasir.pos.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.NumberFormat
import java.util.Locale

/* ---------- formatting ---------- */
private val idFmt: NumberFormat get() = NumberFormat.getNumberInstance(Locale("id", "ID"))
fun rupiah(value: Long): String = (if (value < 0) "-Rp " else "Rp ") + idFmt.format(Math.abs(value))
fun digits(value: String) = value.filter(Char::isDigit)
fun formatInput(value: String): String {
    val d = digits(value); if (d.isEmpty()) return ""
    return idFmt.format(d.toLong())
}

/* ---------- toast ---------- */
val LocalToast = compositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

/* ---------- radius tokens (css: 8 / 12 / 16) ---------- */
val RSm = RoundedCornerShape(8.dp)
val RMd = RoundedCornerShape(12.dp)
val RLg = RoundedCornerShape(16.dp)
val RPill = RoundedCornerShape(50)
val RXs = RoundedCornerShape(4.dp)

/* ---------- text ---------- */
@Composable
fun Txt(
    text: String, size: Int = 14, weight: FontWeight = FontWeight.Normal, color: Color = Sk.c.text,
    modifier: Modifier = Modifier, align: TextAlign? = null, mono: Boolean = false,
    maxLines: Int = Int.MAX_VALUE, spacing: Float = 0f, strike: Boolean = false, lineHeight: Float = 1.45f
) {
    Text(
        text = text, modifier = modifier, color = color, fontSize = size.sp, fontWeight = weight,
        fontFamily = if (mono) JetBrainsMono else Inter, letterSpacing = spacing.sp,
        textAlign = align, lineHeight = (size * lineHeight).sp, maxLines = maxLines,
        overflow = TextOverflow.Ellipsis, textDecoration = if (strike) TextDecoration.LineThrough else null
    )
}

/* ---------- surfaces ---------- */
@Composable
fun Modifier.skCard(shape: RoundedCornerShape = RMd, bg: Color? = null): Modifier =
    this.background(bg ?: Sk.c.card, shape).border(1.dp, Sk.c.border, shape)

@Composable
fun SkCard(modifier: Modifier = Modifier, padding: Int = 0, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val base = modifier.skCard().clip(RMd)
    Column(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding.dp), content = content
    )
}

/* ---------- buttons ---------- */
@Composable
fun Btn(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Int = 0, // 0 primary, 1 ghost, 2 danger
    enabled: Boolean = true, icon: ImageVector? = null
) {
    val c = Sk.c
    val bg = when (kind) { 0 -> c.primary; 2 -> c.alert; else -> Color.Transparent }
    val fg = if (kind == 1) c.text else Color.White
    Row(
        modifier.heightIn(min = 44.dp).alpha(if (enabled) 1f else .45f).clip(RSm).background(bg)
            .then(if (kind == 1) Modifier.border(1.dp, c.borderStrong, RSm) else Modifier)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { SkIcon(icon, 16.dp, fg); Spacer(Modifier.width(6.dp)) }
        Txt(text, 14, FontWeight.SemiBold, fg, maxLines = 1)
    }
}

@Composable
fun BtnLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Sk.c.primary) {
    Box(modifier.clip(RXs).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Txt(text, 13, FontWeight.SemiBold, color)
    }
}

@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, size: Int = 18, tint: Color = Sk.c.text, badge: Boolean = false) {
    Box(modifier.size(40.dp).clip(RSm).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        SkIcon(icon, size.dp, tint)
        if (badge) Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(8.dp).background(Sk.c.alert, CircleShape).border(2.dp, Sk.c.card, CircleShape))
    }
}

@Composable
fun SkButton(text: String, onClick: () -> Unit, danger: Boolean = false, enabled: Boolean = true, modifier: Modifier = Modifier, block: Boolean = false) =
    Btn(text, onClick, modifier.then(if (block) Modifier.fillMaxWidth() else Modifier), if (danger) 2 else 0, enabled)

/* ---------- inputs ---------- */
@Composable
fun Field(
    label: String?, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    placeholder: String = "", enabled: Boolean = true, password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, hint: String? = null, trailing: (@Composable () -> Unit)? = null
) {
    val c = Sk.c
    var revealLast by remember { mutableStateOf(false) }
    LaunchedEffect(value, password) {
        if (password && value.isNotEmpty()) {
            revealLast = true
            kotlinx.coroutines.delay(700)
            revealLast = false
        } else revealLast = false
    }
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Txt(label, 13, FontWeight.Medium, c.textMuted)
        BasicTextField(
            value = value, onValueChange = onChange, enabled = enabled, singleLine = singleLine,
            interactionSource = src, cursorBrush = SolidColor(c.primary),
            textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = if (enabled) c.text else c.textMuted),
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
            visualTransformation = when {
                !password -> VisualTransformation.None
                revealLast -> VisualTransformation { text ->
                    androidx.compose.ui.text.input.TransformedText(
                        androidx.compose.ui.text.AnnotatedString(buildString {
                            for (index in text.indices) append(if (index == text.lastIndex) text[index] else '\u2022')
                        }),
                        androidx.compose.ui.text.input.OffsetMapping.Identity
                    )
                }
                else -> PasswordVisualTransformation()
            },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().clip(RSm).background(if (enabled) c.card else c.surfaceAlt)
                        .border(if (focused) 1.5.dp else 1.dp, if (focused) c.primary else c.borderStrong, RSm)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) Txt(placeholder, 15, color = c.textFaint)
                        inner()
                    }
                    if (trailing != null) trailing()
                }
            }
        )
        if (hint != null) Txt(hint, 12, color = c.textFaint)
    }
}

@Composable
fun RupiahField(label: String?, value: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier, placeholder: String = "0", hint: String? = null) {
    Field(label, if (value == 0L) "" else formatInput(value.toString()), { onChange(digits(it).toLongOrNull() ?: 0L) }, modifier, placeholder, keyboard = KeyboardType.Number, hint = hint)
}

@Composable
fun SelectField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Sk.c
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Txt(label, 13, FontWeight.Medium, c.textMuted)
        Box {
            Row(
                Modifier.fillMaxWidth().clip(RSm).background(c.card).border(1.dp, c.borderStrong, RSm)
                    .clickable { open = true }.padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt(value, 15, color = c.text, modifier = Modifier.weight(1f))
                SkIcon(SkIcons.ChevronDown, 16.dp, c.textMuted)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o -> DropdownMenuItem(text = { Txt(o, 14) }, onClick = { onSelect(o); open = false }) }
            }
        }
    }
}

@Composable
fun SkSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (on) 21.dp else 3.dp, label = "sw")
    Box(Modifier.size(44.dp, 26.dp).clip(RPill).background(if (on) Sk.c.primary else Sk.c.borderStrong).clickable { onChange(!on) }) {
        Box(Modifier.offset(x = x, y = 3.dp).size(20.dp).background(Color.White, CircleShape))
    }
}

@Composable
fun SwitchRow(title: String, desc: String?, on: Boolean, onChange: (Boolean) -> Unit, divider: Boolean = false, pad: Int = 10) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = pad.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Txt(title, 14, FontWeight.Medium)
                if (desc != null) Txt(desc, 12, color = Sk.c.textMuted)
            }
            SkSwitch(on, onChange)
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(Sk.c.border))
    }
}

/* ---------- chips / tabs / badges ---------- */
@Composable
fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    val c = Sk.c
    Box(
        Modifier.clip(RPill).background(if (active) c.primary else c.card)
            .border(1.dp, if (active) c.primary else c.borderStrong, RPill).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) { Txt(text, 13, FontWeight.Medium, if (active) Color.White else c.text, maxLines = 1) }
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun SegTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, icons: List<ImageVector>? = null) {
    val c = Sk.c
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val iconOnly = icons != null && cfg.screenWidthDp <= 480 && cfg.screenHeightDp <= 900
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(14.dp)).background(c.surfaceAlt)
            .border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, l ->
            val on = i == selected
            val fg = if (on) Color.White else c.textMuted
            Row(
                Modifier.weight(1f).heightIn(min = if (iconOnly) 44.dp else 40.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.primary else Color.Transparent).clickable { onSelect(i) }.padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                if (icons != null) SkIcon(icons[i], if (iconOnly) 20.dp else 16.dp, fg)
                if (!iconOnly) {
                    if (icons != null) Spacer(Modifier.width(6.dp))
                    Txt(l, 13, if (on) FontWeight.SemiBold else FontWeight.Medium, fg, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun BrandMark(size: Int, radius: Int, modifier: Modifier = Modifier) {
    val c = Sk.c
    val sz = size.dp
    androidx.compose.foundation.Canvas(modifier.size(sz).clip(RoundedCornerShape(radius.dp)).background(c.primary)) {
        val k = this.size.width * 0.72f / 24f
        val off = this.size.width * 0.14f
        withTransform({
            translate(left = off, top = off)
            scale(scaleX = k, scaleY = k, pivot = androidx.compose.ui.geometry.Offset.Zero)
        }) {
            val path = androidx.compose.ui.graphics.vector.PathParser().parsePathString("M8 3.5l1 1 1-1 1 1 1-1 1 1 1-1 1 1 1-1v6H8v-6z").toPath()
            drawPath(path, Color.White)
            drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(3.5f, 10f), androidx.compose.ui.geometry.Size(17f, 10.5f), androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f))
            drawCircle(c.primary, 1.6f, androidx.compose.ui.geometry.Offset(17f, 15f))
        }
    }
}


@Composable
fun ToastView(toast: Pair<String, String>?, top: androidx.compose.ui.unit.Dp) {
    val c = Sk.c
    if (toast == null) return
    Box(Modifier.fillMaxWidth().padding(top = top), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.padding(horizontal = 16.dp).widthIn(max = 400.dp).fillMaxWidth().shadow(8.dp, RSm).clip(RSm)
                .background(when (toast.second) { "success" -> c.cash; "error" -> c.alert; else -> c.text })
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) { Txt(toast.first, 13, FontWeight.Medium, if (toast.second == "") c.surface else Color.White) }
    }
}

@Composable
fun Tag(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, bold: Boolean = false, pill: Boolean = false) {
    Box(modifier.clip(if (pill) RPill else RXs).background(bg).padding(horizontal = if (pill) 8.dp else 6.dp, vertical = 2.dp)) {
        Txt(text.uppercase(), 10, if (bold) FontWeight.Bold else FontWeight.SemiBold, fg, spacing = .3f, maxLines = 1, lineHeight = 1.3f)
    }
}

@Composable fun TagActive(on: Boolean) = if (on) Tag("Aktif", Sk.c.cashSoft, Sk.c.cash, pill = true) else Tag("Nonaktif", Sk.c.surfaceAlt, Sk.c.textMuted, pill = true)

/* ---------- headings ---------- */
@Composable
fun PageHead(title: String, sub: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Txt(title, 20, FontWeight.SemiBold, lineHeight = 1.3f)
            if (sub != null) Txt(sub, 13, color = Sk.c.textMuted, modifier = Modifier.padding(top = 2.dp))
        }
        if (action != null) { Spacer(Modifier.width(12.dp)); action() }
    }
}

@Composable fun BackHome(onClick: () -> Unit) = Btn("← Kelola", onClick, kind = 1)

@Composable
fun SectionTitle(text: String, first: Boolean = false) {
    Txt(text.uppercase(), 12, FontWeight.SemiBold, Sk.c.textMuted, spacing = .72f, modifier = Modifier.padding(top = if (first) 0.dp else 20.dp, bottom = 10.dp))
}

@Composable
fun EmptyState(title: String, sub: String? = null, icon: ImageVector? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (icon != null) SkIcon(icon, 40.dp, Sk.c.textFaint)
        Txt(title, 15, FontWeight.SemiBold, align = TextAlign.Center)
        if (sub != null) Txt(sub, 13, color = Sk.c.textMuted, align = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp))
        if (action != null && onAction != null) Btn(action, onAction, Modifier.padding(top = 8.dp))
    }
}

/* ---------- KPI ---------- */
@Composable
fun KpiHero(label: String, amount: String, delta: String? = null) {
    SkCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), padding = 20) {
        Txt(label, 13, color = Sk.c.textMuted)
        Txt(amount, 32, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp), lineHeight = 1.1f, spacing = -.64f)
        if (delta != null) Txt(delta, 13, FontWeight.Medium, Sk.c.cash)
    }
}

@Composable
fun KpiCard(label: String, amount: String, modifier: Modifier = Modifier, color: Color = Sk.c.text) {
    SkCard(modifier, padding = 14) {
        Txt(label, 12, color = Sk.c.textMuted)
        Txt(amount, 18, FontWeight.Bold, color, Modifier.padding(top = 4.dp), spacing = -.18f)
    }
}

@Composable
fun KpiGrid(items: List<Triple<String, String, Color?>>) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (l, a, col) -> KpiCard(l, a, Modifier.weight(1f), col ?: Sk.c.text) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun KpiCompact(items: List<Triple<String, String, Color?>>) {
    val c = Sk.c
    Column(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RMd).background(c.border).border(1.dp, c.border, RMd).padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                row.forEach { (l, a, col) ->
                    Column(Modifier.weight(1f).background(c.card).padding(14.dp)) {
                        Txt(l, 12, color = c.textMuted)
                        Txt(a, 16, FontWeight.Bold, col ?: c.text, Modifier.padding(top = 4.dp))
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f).background(c.card))
            }
        }
    }
}

/* ---------- rows ---------- */
@Composable
fun KeyRow(label: String, value: String, valueColor: Color = Sk.c.text, bold: Boolean = true, divider: Boolean = true, big: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Txt(label, 13, color = Sk.c.textMuted, modifier = Modifier.weight(1f))
            Txt(value, if (big) 15 else 13, if (bold) FontWeight.SemiBold else FontWeight.Normal, valueColor, align = TextAlign.End)
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(Sk.c.border))
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = Sk.c.text, total: Boolean = false, size: Int = 14) {
    if (total) Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(Sk.c.border))
    Row(Modifier.fillMaxWidth().padding(top = if (total) 12.dp else 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Txt(label, if (total) 18 else size, if (total) FontWeight.Bold else FontWeight.Normal)
        Txt(value, if (total) 18 else size, if (total) FontWeight.Bold else FontWeight.Normal, valueColor)
    }
}

@Composable
fun ListRow(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val base = modifier.fillMaxWidth().skCard().clip(RMd)
    Row(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), content = content
    )
}

@Composable
fun StatusBanner(title: String?, text: String, warn: Boolean, icon: ImageVector, modifier: Modifier = Modifier) {
    val c = Sk.c
    val bg = if (warn) c.warnSoft else c.alertSoft
    val fg = if (warn) c.warn else c.alert
    Row(modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RSm).background(bg).padding(12.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        SkIcon(icon, 18.dp, fg, Modifier.padding(top = 1.dp))
        Column {
            if (title != null) Txt(title, 13, FontWeight.Bold, fg)
            Txt(text, 13, color = fg)
        }
    }
}

/* ---------- sheet / dialog ---------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = Sk.c
    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = c.card, scrimColor = Color.Black.copy(alpha = .4f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), tonalElevation = 0.dp,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 12.dp).size(40.dp, 4.dp).clip(RXs).background(c.borderStrong)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), content = content)
    }
}

@Composable
fun SheetTitle(title: String, sub: String? = null) {
    Txt(title, 17, FontWeight.SemiBold, lineHeight = 1.3f)
    if (sub != null) Txt(sub, 12, color = Sk.c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) else Spacer(Modifier.height(12.dp))
}

@Composable
fun SkDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(20.dp).widthIn(max = 420.dp).fillMaxWidth().clip(RLg).background(Sk.c.card).padding(24.dp), content = content
        )
    }
}

@Composable
fun ConfirmModal(title: String, message: String, confirm: String, danger: Boolean = true, showCancel: Boolean = true, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    SkDialog(onDismiss) {
        Txt(title, 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Txt(message, 14, color = Sk.c.textMuted, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp), lineHeight = 1.55f)
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            if (showCancel) Btn("Batal", onDismiss, kind = 1)
            Btn(confirm, { onConfirm() }, kind = if (danger) 2 else 0)
        }
    }
}

@Composable
fun PromptModal(title: String, label: String, initial: String, confirm: String = "Simpan", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    SkDialog(onDismiss) {
        Txt(title, 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Spacer(Modifier.height(12.dp))
        Field(label, v, { v = it })
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            Btn("Batal", onDismiss, kind = 1)
            Btn(confirm, { onConfirm(v) })
        }
    }
}

@Composable
fun PinModal(title: String, hint: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    SkDialog(onDismiss) {
        Txt(title, 17, FontWeight.SemiBold, lineHeight = 1.3f)
        Txt(hint, 14, color = Sk.c.textMuted, modifier = Modifier.padding(top = 8.dp), lineHeight = 1.55f)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.weight(1f).aspectRatio(1f).clip(RSm).background(Sk.c.card).border(1.dp, Sk.c.borderStrong, RSm),
                    contentAlignment = Alignment.Center
                ) { Txt(if (i < pin.length) "•" else "", 24, FontWeight.Bold) }
            }
        }
        Field(null, pin, { pin = digits(it).take(4) }, placeholder = "PIN 4 digit", keyboard = KeyboardType.NumberPassword)
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            Btn("Batal", onDismiss, kind = 1)
            Btn("Konfirmasi", { onConfirm(pin) }, enabled = pin.length == 4)
        }
    }
}

@Composable
fun FormActions(left: String, onLeft: () -> Unit, right: String, onRight: () -> Unit, rightKind: Int = 0, rightEnabled: Boolean = true, top: Int = 8) {
    Row(Modifier.fillMaxWidth().padding(top = top.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Btn(left, onLeft, Modifier.weight(1f), kind = 1)
        Btn(right, onRight, Modifier.weight(1f), kind = rightKind, enabled = rightEnabled)
    }
}

@Composable
fun SkFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(56.dp).clip(CircleShape).background(Sk.c.primary).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        SkIcon(SkIcons.Plus, 24.dp, Color.White)
    }
}

/* ---------- compat helpers still used by other files ---------- */
@Composable fun SkChip(text: String, active: Boolean, onClick: () -> Unit) = Chip(text, active, onClick)
@Composable fun SkSectionTitle(text: String) = SectionTitle(text)
