package com.whispervault.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.whispervault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = GhostTextSecondary) },
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GhostGreen,
            unfocusedBorderColor = GhostSurfaceVariant,
            focusedTextColor = GhostText,
            unfocusedTextColor = GhostText,
            cursorColor = GhostGreen,
            focusedContainerColor = GhostSurface,
            unfocusedContainerColor = GhostSurface,
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        enabled = enabled
    )
}
