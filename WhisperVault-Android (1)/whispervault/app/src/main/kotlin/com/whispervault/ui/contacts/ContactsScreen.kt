package com.whispervault.ui.contacts

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whispervault.data.AuthRepository
import com.whispervault.data.local.ConnectionDao
import com.whispervault.data.local.ConnectionEntity
import com.whispervault.data.remote.DiscoveredUser
import com.whispervault.ui.auth.avatarEmoji
import com.whispervault.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val connectionDao: ConnectionDao
) : ViewModel() {

    private val _found = MutableStateFlow<List<DiscoveredUser>>(emptyList())
    val found: StateFlow<List<DiscoveredUser>> = _found

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _manualInput = MutableStateFlow("")
    val manualInput: StateFlow<String> = _manualInput

    fun setManualInput(v: String) { _manualInput.value = v }

    fun discoverFromContacts(mobiles: List<String>) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val res = repo.discoverContacts(mobiles)
                if (res.isSuccessful) _found.value = res.body()?.matched ?: emptyList()
                else _error.value = "Discovery failed"
            } catch (e: Exception) {
                _error.value = "Network error"
            }
            _loading.value = false
        }
    }

    fun searchByUserId(userId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val res = repo.sendChatRequest(userId)
                if (res.isSuccessful) {
                    _error.value = null
                } else {
                    _error.value = "User not found or request failed"
                }
            } catch (e: Exception) {
                _error.value = "Network error"
            }
            _loading.value = false
        }
    }

    fun sendChatRequest(toUserId: String) {
        viewModelScope.launch {
            try { repo.sendChatRequest(toUserId) } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onUserFound: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val found by viewModel.found.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val manualInput by viewModel.manualInput.collectAsState()
    val context = LocalContext.current

    val contactPermission = rememberPermissionState(Manifest.permission.READ_CONTACTS)

    LaunchedEffect(contactPermission.status.isGranted) {
        if (contactPermission.status.isGranted) {
            // Read contacts and discover
            val mobiles = readPhoneNumbers(context)
            viewModel.discoverFromContacts(mobiles)
        }
    }

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = GhostText) }
                },
                title = { Text("Find People", color = GhostText, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostSurface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // Manual user ID search
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { viewModel.setManualInput(it.uppercase()) },
                    placeholder = { Text("Enter User ID (e.g. A9F7K2MZ)", color = GhostTextSecondary, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostGreen, unfocusedBorderColor = GhostSurfaceVariant,
                        focusedTextColor = GhostText, unfocusedTextColor = GhostText,
                        cursorColor = GhostGreen, focusedContainerColor = GhostSurface, unfocusedContainerColor = GhostSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendChatRequest(manualInput.trim()) },
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(GhostGreen)
                ) {
                    Icon(Icons.Default.Search, null, tint = GhostBackground)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Contact discovery
            if (!contactPermission.status.isGranted) {
                Button(
                    onClick = { contactPermission.launchPermissionRequest() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GhostSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Find from Phone Contacts", color = GhostGreen)
                }
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GhostGreen)
                }
            }

            error?.let {
                Text(it, color = GhostRed, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            if (found.isNotEmpty()) {
                Text("${found.size} WhisperVault users found", color = GhostTextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(found) { user ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = GhostSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(GhostSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(avatarEmoji(user.avatarId), fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.displayName, color = GhostText, fontWeight = FontWeight.SemiBold)
                                    Text(user.uniqueId, color = GhostTextSecondary, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.sendChatRequest(user.uniqueId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Request", color = GhostBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun readPhoneNumbers(context: android.content.Context): List<String> {
    val mobiles = mutableListOf<String>()
    try {
        val cursor = context.contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )
        cursor?.use {
            val colIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val number = it.getString(colIdx)?.replace(Regex("[\\s\\-()]"), "") ?: continue
                if (number.isNotBlank()) mobiles.add(number)
            }
        }
    } catch (_: Exception) {}
    return mobiles.distinct().take(500)
}
