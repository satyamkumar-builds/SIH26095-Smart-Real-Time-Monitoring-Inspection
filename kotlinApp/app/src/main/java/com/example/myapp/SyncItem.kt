package com.example.myapp

import java.io.File

data class SyncItem(
    val id: String,
    val inspectionId: String,
    val title: String,
    val priority: String,
    val location: String,
    val time: String,
    val imageFile: File,
    var isSynced: Boolean = false
)
