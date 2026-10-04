package com.pentolrebus.kasir.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pentolrebus.kasir.data.OfflineStore
import com.pentolrebus.kasir.data.PosRepository
class PosViewModelFactory(private val repo:PosRepository, private val offline:OfflineStore):ViewModelProvider.Factory{override fun <T:ViewModel>create(modelClass:Class<T>):T=PosViewModel(repo,offline) as T}
