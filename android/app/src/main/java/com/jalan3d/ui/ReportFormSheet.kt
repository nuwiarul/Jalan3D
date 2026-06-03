package com.jalan3d.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormSheet(
    address: String?,
    selectedSeverity: String,
    photoUri: Uri?,
    isSubmitting: Boolean,
    submitError: String?,
    submitSuccess: Boolean,
    onDismiss: () -> Unit,
    onSeveritySelected: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onSubmit: () -> Unit
) {
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title row with cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lapor Jalan Rusak",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Batal")
                }
            }

            // Address
            if (address != null) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Severity selector
            Text("Tingkat Kerusakan", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ringan", "sedang", "berat", "kritis").forEach { severity ->
                    FilterChip(
                        selected = selectedSeverity == severity,
                        onClick = { onSeveritySelected(severity) },
                        label = { Text(severity.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    onDescriptionChange(it)
                },
                label = { Text("Deskripsi (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Photo preview + button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onTakePhoto) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Ambil Foto")
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoUri != null) "Ganti Foto" else "Ambil Foto")
                }

                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Preview foto",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Error message
            if (submitError != null) {
                Text(
                    text = submitError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Submit button
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Lapor!")
            }
        }
    }
}
