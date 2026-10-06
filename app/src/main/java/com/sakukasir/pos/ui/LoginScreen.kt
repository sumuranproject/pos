package com.sakukasir.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun LoginScreen(vm:PosViewModel){
    var user by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var register by remember{mutableStateOf(false)};var forgot by remember{mutableStateOf(false)}
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(Modifier.widthIn(max=400.dp).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(56.dp),contentAlignment=Alignment.Center){Surface(color=MaterialTheme.colorScheme.primary,shape=MaterialTheme.shapes.medium){Text("SK",color=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.padding(16.dp))}};Text(if(register)"Daftar Owner" else if(forgot)"Lupa password" else "SakuKasir",style=MaterialTheme.typography.headlineMedium);Text(if(register)"Tahap pendaftaran demo" else "Point of Sale untuk usaha kamu",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(20.dp))
        when{register->{SkTextField(user,{user=it},"Username");SkTextField("",{},"Email");SkTextField(pass,{pass=it},"Password");SkTextField("",{},"Nama tampilan");Spacer(Modifier.height(8.dp));SkButton("Daftar",{register=false},block=true);TextButton({register=false}){Text("Kembali ke login")}}
        forgot->{SkTextField(user,{user=it},"Email terdaftar");SkButton("Kirim link reset",{forgot=false},block=true);TextButton({forgot=false}){Text("Kembali ke login")}}
        else->{SkTextField(user,{user=it},"Username");SkTextField(pass,{pass=it},"Password");Text("Demo: owner / kasir",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);vm.state.collectAsState().value.notice?.let{Text(it,color=MaterialTheme.colorScheme.error)};SkButton("Masuk",{vm.login(user,pass)},block=true);Row{TextButton({register=true}){Text("Daftar sebagai Owner")};TextButton({forgot=true}){Text("Lupa password?")}}}}
    }}
}
