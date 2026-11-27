package de.juvento.plain.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.juvento.plain.R
import de.juvento.plain.data.AppModel
import de.juvento.plain.data.AppSearch
import de.juvento.plain.helper.getAppsList

private val PresetColors = listOf(
    0xFF000000, 0xFFFFFFFF, 0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047,
    0xFF00ACC1, 0xFF1E88E5, 0xFF5E35B1, 0xFFD81B60, 0xFF8D6E63, 0xFF9E9E9E,
).map { it.toInt() }

private const val OPAQUE = 0xFF000000.toInt()

fun Int.toHexRgb(): String = "%06X".format(this and 0xFFFFFF)

/** RGB sliders, hex input and presets. Only opaque colors can be picked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var color by remember { mutableIntStateOf(initial or OPAQUE) }
    var hex by remember { mutableStateOf(color.toHexRgb()) }

    fun update(newColor: Int) {
        color = newColor or OPAQUE
        hex = color.toHexRgb()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_color)) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(color), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(8.dp))
                val channels = listOf("R" to 16, "G" to 8, "B" to 0)
                for ((label, shift) in channels) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.padding(end = 8.dp))
                        Slider(
                            value = ((color shr shift) and 0xFF).toFloat(),
                            onValueChange = { v -> update((color and (0xFF shl shift).inv()) or (v.toInt() shl shift)) },
                            valueRange = 0f..255f,
                        )
                    }
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { input ->
                        val clean = input.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.take(6).uppercase()
                        hex = clean
                        if (clean.length == 6) color = clean.toInt(16) or OPAQUE
                    },
                    prefix = { Text("#") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    for (preset in PresetColors) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(preset), CircleShape)
                                .border(
                                    if (preset == color) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                    else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    CircleShape,
                                )
                                .clickable { update(preset) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(color) }) { Text(stringResource(R.string.okay)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * Single line text input. [filter] limits what can be typed, [validate] returns an error text
 * (which also disables saving) or null; [onReset] adds a button that restores a default value.
 */
@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onReset: (() -> Unit)? = null,
    filter: (String) -> String = { it },
    validate: (String) -> String? = { null },
) {
    var text by remember { mutableStateOf(initial) }
    val error = validate(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = filter(it) },
                label = { Text(label) },
                singleLine = true,
                isError = error != null,
                supportingText = { if (error != null) Text(error) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = error == null) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onReset != null) TextButton(onClick = onReset) { Text(stringResource(R.string.reset)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/**
 * Full-screen list of all installed apps (including hidden ones) with search.
 * With [multi] the user ticks several apps and confirms; otherwise a tap picks one app.
 */
@Composable
fun AppPickerDialog(
    title: String,
    multi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<AppModel>) -> Unit,
    disabledApps: Set<String> = emptySet(),
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppModel>?>(null) }
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) { apps = getAppsList(context, showHiddenApps = true) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_apps)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val list = apps
                if (list == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val shown = list.filter { AppSearch.matches(it, query.trim()) }
                    LazyColumn(Modifier.weight(1f)) {
                        items(shown, key = { it.identity + it.appActivityName }) { app ->
                            val id = app.identity
                            val taken = id in disabledApps
                            val checked = taken || id in selected
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !taken) {
                                        if (multi) {
                                            if (id in selected) selected.remove(id) else selected.add(id)
                                        } else {
                                            onConfirm(listOf(app))
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (multi) Checkbox(checked = checked, onCheckedChange = null, enabled = !taken, modifier = Modifier.padding(end = 12.dp))
                                Text(
                                    app.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (taken) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                if (multi) {
                    Button(
                        onClick = {
                            val chosen = list.orEmpty().filter { it.identity in selected }
                            onConfirm(chosen)
                        },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.add_selected, selected.size)) }
                }
            }
        }
    }
}
