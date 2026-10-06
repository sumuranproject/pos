package com.sakukasir.pos.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sakukasir.pos.data.PosRepository
class PosViewModelFactory(private val repo:PosRepository):ViewModelProvider.Factory {
    override fun <T:ViewModel> create(modelClass:Class<T>):T {
        if(modelClass.isAssignableFrom(PosViewModel::class.java)) @Suppress("UNCHECKED_CAST") return PosViewModel(repo) as T
        error("Unknown ViewModel")
    }
}
