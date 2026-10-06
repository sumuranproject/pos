package com.sakukasir.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun QrisScreen(vm:PosViewModel){
    val set by vm.settings.collectAsState();var enabled by remember(set){mutableStateOf(set.qrisEnabled)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("QRIS",style=MaterialTheme.typography.headlineSmall);SkCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Icon(Icons.Default.QrCode2,null,Modifier.size(180.dp));Text("QRIS outlet: ${set.qrisOutlet}");ListItem(headlineContent={Text("Aktif")},trailingContent={Switch(enabled,{enabled=it})})}};SkButton("Simpan",{vm.updateSettings(set.copy(qrisEnabled=enabled))},block=true)}
}
