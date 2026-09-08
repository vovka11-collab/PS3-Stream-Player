package com.vovka11.ps3streamplayer.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.model.meta.*
import org.fourthline.cling.model.types.ServiceType
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.model.types.UDADeviceType
import java.io.File

class StreamingService : Service() {
    private val binder = StreamingBinder()
    private var upnpService: UpnpService? = null
    private var httpServer: ApplicationEngine? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isRunning = false

    companion object {
        private const val TAG = "StreamingService"
        const val PORT = 8080
    }

    inner class StreamingBinder : Binder() {
        fun getService(): StreamingService = this@StreamingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_STREAMING" -> startStreaming()
            "STOP_STREAMING" -> stopStreaming()
        }
        return START_STICKY
    }

    fun startStreaming() {
        if (isRunning) return
        
        serviceScope.launch {
            try {
                // Запуск HTTP сервера
                startHttpServer()
                
                // Запуск UPnP/DLNA сервиса
                startUPnPService()
                
                isRunning = true
                Log.d(TAG, "Streaming service started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting streaming service", e)
            }
        }
    }

    fun stopStreaming() {
        if (!isRunning) return
        
        serviceScope.launch {
            try {
                httpServer?.stop()
                upnpService?.shutdown()
                isRunning = false
                Log.d(TAG, "Streaming service stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping streaming service", e)
            }
        }
    }

    private suspend fun startHttpServer() {
        httpServer = embeddedServer(CIO, port = PORT) {
            routing {
                // DLNA Device Descriptor
                get("/device.xml") {
                    val deviceDescriptor = generateDeviceDescriptor()
                    call.respondText(deviceDescriptor, contentType = io.ktor.http.ContentType.Application.Xml)
                }
                
                // Content Directory Service Description
                get("/contentdir.xml") {
                    val serviceDescriptor = generateContentDirectoryService()
                    call.respondText(serviceDescriptor, contentType = io.ktor.http.ContentType.Application.Xml)
                }
                
                // Connection Manager Service Description
                get("/connectionmgr.xml") {
                    val serviceDescriptor = generateConnectionManagerService()
                    call.respondText(serviceDescriptor, contentType = io.ktor.http.ContentType.Application.Xml)
                }
                
                // Video streaming endpoint
                get("/video/{id}") {
                    val id = call.parameters["id"] ?: return@get
                    val file = File(filesDir, "videos/$id.mp4")
                    if (file.exists()) {
                        call.respondFile(file, io.ktor.http.ContentType.Video.MP4)
                    } else {
                        call.respond("Not found")
                    }
                }
                
                // Media browsing endpoint
                post("/upnp/control/ContentDirectory1") {
                    // Handle UPnP control requests
                    call.respondText("<s:Envelope></s:Envelope>", contentType = io.ktor.http.ContentType.Application.Xml)
                }
            }
        }.start()
    }

    private fun startUPnPService() {
        upnpService = UpnpServiceImpl()
        upnpService?.registry?.addDevice(createMediaServerDevice())
    }

    private fun createMediaServerDevice(): Device<*, *, *> {
        val localAddress = getLocalIpAddress()
        val baseUrl = "http://$localAddress:$PORT"
        
        return LocalDevice(
            deviceMetadata = DeviceIdentity(
                deviceType = UDADeviceType("MediaServer", 1),
                baseURL = baseUrl
            ),
            type = UDADeviceType("MediaServer", 1),
            details = DeviceDetails(
                friendlyName = "PS3 Stream Player",
                manufacturer = "PS3StreamPlayer",
                manufacturerURI = "http://localhost",
                modelDescription = "Stream converter for PS3",
                modelName = "PS3StreamPlayer",
                modelNumber = "1.0",
                modelURI = "http://localhost",
                serialNumber = "12345"
            ),
            services = arrayOf(
                createContentDirectoryService(),
                createConnectionManagerService()
            )
        )
    }

    private fun createContentDirectoryService(): LocalService<*> {
        return LocalService(
            serviceType = UDAServiceType("ContentDirectory", 1),
            serviceId = "urn:upnp-org:serviceId:ContentDirectory",
            descriptorURI = "/contentdir.xml",
            controlURI = "/upnp/control/ContentDirectory1",
            eventSubscriptionURI = "/upnp/control/ContentDirectory1"
        )
    }

    private fun createConnectionManagerService(): LocalService<*> {
        return LocalService(
            serviceType = UDAServiceType("ConnectionManager", 1),
            serviceId = "urn:upnp-org:serviceId:ConnectionManager",
            descriptorURI = "/connectionmgr.xml",
            controlURI = "/upnp/control/ConnectionManager1",
            eventSubscriptionURI = "/upnp/control/ConnectionManager1"
        )
    }

    private fun generateDeviceDescriptor(): String {
        val localAddress = getLocalIpAddress()
        return """<?xml version="1.0"?>
<root xmlns="urn:schemas-upnp-org:device-1-0">
    <specVersion>
        <major>1</major>
        <minor>0</minor>
    </specVersion>
    <device>
        <deviceType>urn:schemas-upnp-org:device:MediaServer:1</deviceType>
        <friendlyName>PS3 Stream Player</friendlyName>
        <manufacturer>PS3StreamPlayer</manufacturer>
        <manufacturerURL>http://localhost</manufacturerURL>
        <modelDescription>Stream converter for PS3</modelDescription>
        <modelName>PS3StreamPlayer</modelName>
        <modelNumber>1.0</modelNumber>
        <modelURL>http://localhost</modelURL>
        <serialNumber>12345</serialNumber>
        <UDN>uuid:PS3-Stream-Player-12345</UDN>
        <serviceList>
            <service>
                <serviceType>urn:schemas-upnp-org:service:ContentDirectory:1</serviceType>
                <serviceId>urn:upnp-org:serviceId:ContentDirectory</serviceId>
                <controlURL>/upnp/control/ContentDirectory1</controlURL>
                <eventSubURL>/upnp/control/ContentDirectory1</eventSubURL>
                <SCPDURL>/contentdir.xml</SCPDURL>
            </service>
            <service>
                <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>
                <serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId>
                <controlURL>/upnp/control/ConnectionManager1</controlURL>
                <eventSubURL>/upnp/control/ConnectionManager1</eventSubURL>
                <SCPDURL>/connectionmgr.xml</SCPDURL>
            </service>
        </serviceList>
    </device>
</root>"""
    }

    private fun generateContentDirectoryService(): String {
        return """<?xml version="1.0"?>
<scpd xmlns="urn:schemas-upnp-org:service-1-0">
    <specVersion>
        <major>1</major>
        <minor>0</minor>
    </specVersion>
    <actionList>
        <action>
            <name>Browse</name>
            <argumentList>
                <argument>
                    <name>ObjectID</name>
                    <relatedStateVariable>A_ARG_TYPE_ObjectID</relatedStateVariable>
                    <direction>in</direction>
                </argument>
                <argument>
                    <name>BrowseFlag</name>
                    <relatedStateVariable>A_ARG_TYPE_BrowseFlag</relatedStateVariable>
                    <direction>in</direction>
                </argument>
                <argument>
                    <name>Result</name>
                    <relatedStateVariable>A_ARG_TYPE_Result</relatedStateVariable>
                    <direction>out</direction>
                </argument>
            </argumentList>
        </action>
    </actionList>
    <serviceStateTable>
        <stateVariable sendEvents="no">
            <name>A_ARG_TYPE_ObjectID</name>
            <dataType>string</dataType>
        </stateVariable>
        <stateVariable sendEvents="no">
            <name>A_ARG_TYPE_BrowseFlag</name>
            <dataType>string</dataType>
        </stateVariable>
        <stateVariable sendEvents="no">
            <name>A_ARG_TYPE_Result</name>
            <dataType>string</dataType>
        </stateVariable>
    </serviceStateTable>
</scpd>"""
    }

    private fun generateConnectionManagerService(): String {
        return """<?xml version="1.0"?>
<scpd xmlns="urn:schemas-upnp-org:service-1-0">
    <specVersion>
        <major>1</major>
        <minor>0</minor>
    </specVersion>
    <actionList>
        <action>
            <name>GetProtocolInfo</name>
            <argumentList>
                <argument>
                    <name>Source</name>
                    <relatedStateVariable>SourceProtocolInfo</relatedStateVariable>
                    <direction>out</direction>
                </argument>
                <argument>
                    <name>Sink</name>
                    <relatedStateVariable>SinkProtocolInfo</relatedStateVariable>
                    <direction>out</direction>
                </argument>
            </argumentList>
        </action>
    </actionList>
    <serviceStateTable>
        <stateVariable sendEvents="yes">
            <name>SourceProtocolInfo</name>
            <dataType>string</dataType>
        </stateVariable>
        <stateVariable sendEvents="yes">
            <name>SinkProtocolInfo</name>
            <dataType>string</dataType>
        </stateVariable>
    </serviceStateTable>
</scpd>"""
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (iface in interfaces) {
                if (iface.isUp && !iface.isLoopback) {
                    for (addr in iface.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr.hostAddress.contains(".")) {
                            return addr.hostAddress
                        }
                    }
                }
            }
            "localhost"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP", e)
            "localhost"
        }
    }

    override fun onDestroy() {
        stopStreaming()
        serviceScope.cancel()
        super.onDestroy()
    }
}
