package com.example.myfirstapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.data.model.InterviewPost
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class InterviewViewModel(application: Application) : AndroidViewModel(application) {
    private val _posts = MutableStateFlow<List<InterviewPost>>(emptyList())
    val posts: StateFlow<List<InterviewPost>> = _posts.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            val loadedPosts = withContext(Dispatchers.IO) {
                try {
                    val assetManager = getApplication<Application>().assets
                    val inputStream = assetManager.open("interview_data.json")
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.use { it.readText() }

                    val listType = object : TypeToken<List<InterviewPost>>() {}.type
                    Gson().fromJson<List<InterviewPost>>(jsonString, listType)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList<InterviewPost>()
                }
            }
            _posts.value = loadedPosts
        }
    }
}