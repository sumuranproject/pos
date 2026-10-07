package com.sakukasir.pos.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

fun rupiah(value: Long): String = "Rp " + NumberFormat.getNumberInstance(Locale("id","ID")).format(value)
fun digits(value: String) = value.filter(Char::isDigit)
fun formatInput(value: String): String {
    val d = digits(value); if (d.isEmpty()) return ""
    return NumberFormat.getNumberInstance(Locale("id","ID")).format(d.toLong())
}

@Composable fun SkButton(text: String, onClick: () -> Unit, danger: Boolean = false, enabled: Boolean = true, modifier: Modifier = Modifier, block: Boolean = false) =
    Button(onClick, enabled=enabled, modifier=modifier.then(if(block) Modifier.fillMaxWidth() else Modifier), shape=RoundedCornerShape(8.dp), colors=ButtonDefaults.buttonColors(containerColor=if(danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) { Text(text) }

@Composable fun SkIconButton(icon: @Composable () -> Unit, onClick: () -> Unit, badge: Boolean = false) {
    Box {
        IconButton(onClick) { icon() }
        if (badge) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(50)).align(Alignment.TopEnd))
    }
}
@Composable fun SkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Card(modifier, shape=RoundedCornerShape(12.dp), border=BorderStroke(1.dp, MaterialTheme.colorScheme.outline), elevation=CardDefaults.cardElevation(1.dp), content=content)
@Composable fun SkTextField(value:String,onValueChange:(String)->Unit,label:String,modifier:Modifier=Modifier,singleLine:Boolean=true) =
    OutlinedTextField(value,onValueChange,label={Text(label)},modifier=modifier.fillMaxWidth(),singleLine=singleLine,shape=RoundedCornerShape(8.dp))
@Composable fun SkRupiahField(value:Long,onValueChange:(Long)->Unit,label:String,modifier:Modifier=Modifier) {
    var text by remember(value) { mutableStateOf(if(value==0L) "" else formatInput(value.toString())) }
    SkTextField(text,{ v -> text=formatInput(v); onValueChange(digits(v).toLongOrNull()?:0L)},label,modifier)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SkSheet(show:Boolean,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit) {
    if(show) ModalBottomSheet(onDismissRequest=onDismiss, content=content)
}
@Composable fun SkModal(show:Boolean,onDismiss:()->Unit,title:String,content:@Composable ColumnScope.()->Unit) {
    if(show) AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(content=content)},confirmButton={TextButton(onClick=onDismiss){Text("Tutup")}})
}
@Composable fun SkChip(text:String,active:Boolean,onClick:()->Unit) = Surface(onClick=onClick, color=if(active) Color(0xFF0F5132) else MaterialTheme.colorScheme.surface, contentColor=if(active) Color.White else MaterialTheme.colorScheme.onSurface, shape=RoundedCornerShape(28.dp), border=if(active) null else BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Text(text,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(horizontal=22.dp,vertical=10.dp))}
@Composable fun SkBadge(text:String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) =
    Surface(modifier=modifier,color=color.copy(alpha=.12f),shape=RoundedCornerShape(8.dp)){Text(text,color=color,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(horizontal=8.dp,vertical=6.dp))}
@Composable fun SkEmptyState(title:String,message:String,action:String?=null,onAction:(()->Unit)?=null) {
    Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp)); Text(title,style=MaterialTheme.typography.titleMedium); Text(message,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(action!=null && onAction!=null){Spacer(Modifier.height(12.dp));SkButton(action,onAction)}
    }
}
@Composable fun SkLoading() = Box(Modifier.fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}
@Composable fun SkKpiCard(label:String,value:String,modifier:Modifier=Modifier) = SkCard(modifier){Column(Modifier.padding(14.dp)){Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium);Text(value,style=MaterialTheme.typography.titleLarge)}}
@Composable fun SkKpiHero(label:String,value:String,modifier:Modifier=Modifier) = SkCard(modifier){Column(Modifier.padding(20.dp)){Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,style=MaterialTheme.typography.headlineMedium)}}
@Composable fun SkKpiCompact(items:List<Pair<String,String>>) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(1.dp)){items.forEach{(a,b)->SkCard(Modifier.weight(1f)){Column(Modifier.padding(12.dp)){Text(a,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelSmall);Text(b,style=MaterialTheme.typography.titleMedium)}}}}}
@Composable fun SkSectionTitle(text:String) { Text(text,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(vertical=10.dp)) }
@Composable fun SkSwitch(checked:Boolean,onCheckedChange:(Boolean)->Unit) = Switch(checked,onCheckedChange)
@Composable fun SkSyncDot(online:Boolean) = Box(Modifier.size(10.dp).background(if(online) Color(0xFF16A34A) else Color(0xFFD97706),RoundedCornerShape(50)))
@Composable fun SkStockBadge(stock:Int,low:Int) = SkBadge(if(stock<=0)"Habis" else "Sisa $stock",if(stock<=0)MaterialTheme.colorScheme.error else if(stock<=low)Color(0xFFD97706) else Color(0xFF16A34A))
@Composable fun SkQtyControl(qty:Int,onMinus:()->Unit,onPlus:()->Unit) { Row(verticalAlignment=Alignment.CenterVertically){IconButton(onMinus){Icon(Icons.Default.Remove,null)};Text(qty.toString(),modifier=Modifier.width(28.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center);IconButton(onPlus){Icon(Icons.Default.Add,null)}}}
@Composable fun SkCartBar(items:Int,total:String,onClick:()->Unit){if(items>0)Surface(shadowElevation=8.dp){Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("$items item");Text(total,style=MaterialTheme.typography.titleMedium);Button(onClick){Text("Lihat Cart")}}}}
@Composable fun SkStatusBanner(text:String,danger:Boolean){Surface(color=(if(danger)MaterialTheme.colorScheme.error else Color(0xFFD97706)).copy(alpha=.12f)){Text(text,color=if(danger)MaterialTheme.colorScheme.error else Color(0xFFD97706),modifier=Modifier.fillMaxWidth().padding(10.dp))}}
@Composable fun SkReasonChip(text:String,selected:Boolean,onClick:()->Unit)=FilterChip(selected,onClick,label={Text(text)},modifier=Modifier.fillMaxWidth())
@Composable fun SkPinInput(value:String,onValueChange:(String)->Unit){SkTextField(value,onValueChange,"PIN")}
@Composable fun SkRefundItem(item:String,checked:Boolean,onCheckedChange:(Boolean)->Unit)=Row(Modifier.fillMaxWidth().clickable{onCheckedChange(!checked)}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(checked,onCheckedChange);Text(item)}
@Composable fun SkOutletChip(name:String,clickable:Boolean,onClick:()->Unit)=Surface(onClick=onClick,enabled=clickable,color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(28.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(horizontal=18.dp,vertical=10.dp)){Text(name,style=MaterialTheme.typography.titleMedium);if(clickable){Spacer(Modifier.width(8.dp));Icon(Icons.Default.KeyboardArrowDown,null,Modifier.size(20.dp))}}}

@Composable fun SkProductCard(name:String,price:Long,unit:String,stock:Int,low:Int,onClick:()->Unit) {
    val enabled=stock>0
    Card(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().height(160.dp),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface,disabledContainerColor=MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxSize().padding(18.dp)) {
            Column(Modifier.fillMaxWidth().alpha(if(enabled) 1f else .42f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(name,style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.SemiBold),maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("/ $unit",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyLarge)
                Text(rupiah(price),color=Color(0xFF0F5132),style=MaterialTheme.typography.titleLarge.copy(fontWeight=FontWeight.Bold))
            }
            if(stock<=0) SkBadge("HABIS",Color(0xFFCF6A72),Modifier.align(Alignment.TopEnd))
            else if(stock<=low) SkBadge("SISA $stock",Color(0xFFD17B00),Modifier.align(Alignment.TopEnd))
        }
    }
}
@Composable fun SkTransactionItem(tx:com.sakukasir.pos.domain.Transaction,onClick:()->Unit){ListItem(headlineContent={Text(tx.id, style=trxMonoStyle(FontWeight.Medium))},supportingContent={Text("${tx.time} · ${tx.method.name} · ${rupiah(tx.total)}")},trailingContent={SkBadge(tx.status.name,if(tx.status==com.sakukasir.pos.domain.TransactionStatus.COMPLETED)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)},modifier=Modifier.clickable(onClick=onClick))}
@Composable fun SkAuditRow(entry:com.sakukasir.pos.domain.AuditEntry){ListItem(headlineContent={Text("${entry.type} · ${entry.trxId}", style=trxMonoStyle())},supportingContent={Text("${entry.byName} · ${entry.reason}")},trailingContent={Text(rupiah(entry.amount))})}
