package com.example.myfirstapp.data.model

import android.net.Uri

data class UserProfile(
    val name: String,
    val realName: String,
    val age: String,
    val signature: String,
    val avatarUri: Uri?
)