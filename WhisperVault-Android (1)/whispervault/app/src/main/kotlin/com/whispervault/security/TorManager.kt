package com.whispervault.security

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class TorStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class TorCircuitInfo(
    val circuitId: String,
    val hops: List<String>,        // relay country codes
    val entryNode: String,
    val exitNode: String,
    val exitIp: String = "unknown"
)

@Singleton
class TorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status      = MutableStateFlow(TorStatus.DISCONNECTED)
    val status: StateFlow<TorStatus> = _status

    private val _circuitInfo = MutableStateFlow<TorCircuitInfo?>(null)
    val circuitInfo: StateFlow<TorCircuitInfo?> = _circuitInfo

    private val _exitIp      = MutableStateFlow<String?>(null)
    val exitIp: StateFlow<String?> = _exitIp

    private val ORBOT_PACKAGE     = "org.torproject.android"
    private val TOR_SOCKS_HOST    = "127.0.0.1"
    private val TOR_SOCKS_PORT    = 9050
    private val TOR_HTTP_PORT     = 8118
    private val CHECK_URL         = "https://check.torproject.org/api/ip"

    // ── Orbot detection ──
    fun isOrbotInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(ORBOT_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) { false }

    // ── OkHttp client routed through SOCKS5 Tor proxy ──
    fun buildTorOkHttpClient(): OkHttpClient {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(TOR_SOCKS_HOST, TOR_SOCKS_PORT))
        return OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ── Check Tor connection + get exit IP ──
    suspend fun checkTorStatus(): Boolean = withContext(Dispatchers.IO) {
        _status.value = TorStatus.CONNECTING
        return@withContext try {
            val client = buildTorOkHttpClient()
            val req = Request.Builder().url(CHECK_URL).build()
            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val isTor = json.optBoolean("IsTor", false)
            val ip    = json.optString("IP", "unknown")

            _exitIp.value = ip
            _status.value = if (isTor) TorStatus.CONNECTED else TorStatus.ERROR
            buildCircuitInfo(ip)
            isTor
        } catch (e: Exception) {
            _status.value = TorStatus.ERROR
            false
        }
    }

    // ── Simulate circuit hops (real implementation needs Tor control port) ──
    private fun buildCircuitInfo(exitIp: String) {
        _circuitInfo.value = TorCircuitInfo(
            circuitId  = (100..999).random().toString(),
            hops       = listOf("🇩🇪 DE", "🇳🇱 NL", "🇸🇪 SE"),
            entryNode  = "Guard Node [DE]",
            exitNode   = "Exit Node [SE]",
            exitIp     = exitIp
        )
    }

    // ── New identity (request Tor circuit renewal via Orbot) ──
    fun requestNewIdentity() {
        val intent = context.packageManager
            .getLaunchIntentForPackage(ORBOT_PACKAGE)
        intent?.let {
            it.action = "org.torproject.android.intent.action.REQUEST_NEW_IDENTITY"
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
        _circuitInfo.value = null
        _exitIp.value = null
        _status.value = TorStatus.CONNECTING
    }

    // ── Open Orbot ──
    fun openOrbot() {
        val intent = if (isOrbotInstalled()) {
            context.packageManager.getLaunchIntentForPackage(ORBOT_PACKAGE)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://guardianproject.info/apps/org.torproject.android"))
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$ORBOT_PACKAGE"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ── Disconnect ──
    fun disconnect() {
        _status.value = TorStatus.DISCONNECTED
        _circuitInfo.value = null
        _exitIp.value = null
    }
}
