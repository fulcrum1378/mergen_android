package com.mergen.android

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class Model : ViewModel() {
    val res: MutableLiveData<String> by lazy { MutableLiveData<String>() }
}
