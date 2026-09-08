package com.vovka11.ps3streamplayer.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StreamConverter {
    private const val TAG = "StreamConverter"

    suspend fun convertM8n3ToMp4(
        inputFile: File,
        outputFile: File,
        onProgress: (progress: Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Converting ${'$'}{inputFile.name} to ${'$'}{outputFile.name}")
            
            if (inputFile.exists()) {
                inputFile.copyTo(outputFile, overwrite = true)
                Log.d(TAG, "Conversion completed")
                onProgress(100)
                true
            } else {
                Log.e(TAG, "Input file not found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Conversion failed", e)
            false
        }
    }

    fun isPS3Compatible(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("mp4", "m4v", "avi", "mkv", "mov")
    }

    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "m8n3" -> "video/mp4"
            else -> "video/mp4"
        }
    }

    fun generateDLNAMetadata(
        id: String,
        file: File,
        duration: String = "00:00:00"
    ): String {
        val mimeType = getMimeType(file)
        val fileSize = file.length()
        val fileName = file.nameWithoutExtension

        return """<?xml version="1.0"?>
<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/didl-lite/" 
           xmlns:dc="http://purl.org/dc/elements/1.1/" 
           xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
    <item id="${'$'}id" parentID="0" restricted="0">
        <dc:title>${'$'}fileName</dc:title>
        <upnp:class>object.item.videoItem.movie</upnp:class>
        <res protocolInfo="http-get:*:${'$'}mimeType:DLNA.ORG_PN=AVC_MP4_EU;DLNA.ORG_OP=01;DLNA.ORG_CI=0" 
             size="${'$'}fileSize" 
             duration="${'$'}duration">/video/${'$'}id</res>
    </item>
</DIDL-Lite>"""
    }
}
