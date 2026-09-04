package com.vovka11.ps3streamplayer.data.models

data class MediaItem(
    val id: String,
    val parentId: String,
    val title: String,
    val url: String,
    val mimeType: String = "video/mp4",
    val duration: String = "00:00:00",
    val size: Long = 0,
    val isContainer: Boolean = false,
    val childCount: Int = 0
)

data class DlnaDevice(
    val uuid: String,
    val friendlyName: String,
    val ipAddress: String,
    val port: Int,
    val lastSeen: Long = System.currentTimeMillis()
)

data class DlnaRequest(
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val remoteAddress: String,
    val statusCode: Int = 0,
    val contentType: String = ""
)
