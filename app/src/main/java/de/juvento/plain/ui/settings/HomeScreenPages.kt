package de.juvento.plain.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.juvento.plain.R
import de.juvento.plain.data.AppModel
import de.juvento.plain.data.AppSet
import de.juvento.plain.data.AppSets
import de.juvento.plain.data.Constants
import de.juvento.plain.data.HomeApp
import de.juvento.plain.data.Prefs
import de.juvento.plain.data.SetNameError
import de.juvento.plain.helper.resolveAppLabel

@Composable
private fun setNameValidator(allSets: List<AppSet>, ownId: String?): (String) -> String? {
    val empty = stringResource(R.string.set_name_empty)
    val duplicate = stringResource(R.string.set_name_duplicate)
    return { text ->
        when (AppSets.validateName(text, allSets, ownId)) {
            SetNameError.Empty -> empty
            SetNameError.Duplicate -> duplicate
            null -> null
        }
    }
}

@Composable
fun HomeScreenPage(
    prefs: Prefs,
    sets: List<AppSet>,
    activeSetId: String,
    onSetsChange: (List<AppSet>) -> Unit,
    onActivate: (String) -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showTime by remember { mutableStateOf(prefs.showTime) }
    var showDate by remember { mutableStateOf(prefs.showDate) }
    var extendArea by remember { mutableStateOf(prefs.extendHomeAppsArea) }
    var allowIntents by remember { mutableStateOf(prefs.allowIntentControl) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<AppSet?>(null) }

    SettingsPage(title = stringResource(R.string.homescreen), onBack = onBack) {
        SwitchRow(stringResource(R.string.show_time), showTime) {
            showTime = it
            prefs.showTime = it
        }
        SettingsDivider()
        SwitchRow(stringResource(R.string.show_date), showDate) {
            showDate = it
            prefs.showDate = it
        }
        SettingsDivider()
        SwitchRow(stringResource(R.string.extend_home_apps_area), extendArea) {
            extendArea = it
            prefs.extendHomeAppsArea = it
        }

        SectionHeader(stringResource(R.string.app_sets))
        Text(
            stringResource(R.string.app_sets_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        for (set in sets) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onActivate(set.id) }
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = set.id == activeSetId, onClick = { onActivate(set.id) })
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(set.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.apps_count, set.apps.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onEdit(set.id) }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_set))
                }
                IconButton(onClick = { deleting = set }, enabled = sets.size > 1) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
        TextButton(onClick = { creating = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.new_set))
        }

        SectionHeader(stringResource(R.string.external_control))
        SwitchRow(stringResource(R.string.intent_control), allowIntents) {
            allowIntents = it
            prefs.allowIntentControl = it
        }
        Text(
            stringResource(R.string.intent_control_hint, Constants.ACTION_SET_ACTIVE_SET, Constants.EXTRA_SET_NAME),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.new_set),
            initial = "",
            label = stringResource(R.string.set_name),
            filter = AppSets::sanitizeInput,
            validate = setNameValidator(sets, null),
            onDismiss = { creating = false },
            onConfirm = { name ->
                val created = AppSet(AppSets.newId(), AppSets.normalizeName(name), emptyList())
                onSetsChange(sets + created)
                creating = false
                onEdit(created.id)
            },
        )
    }

    deleting?.let { set ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_set_title, set.name)) },
            text = { Text(stringResource(R.string.delete_set_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val remaining = sets.filter { it.id != set.id }
                    onSetsChange(remaining)
                    if (set.id == activeSetId) onActivate(remaining.first().id)
                    deleting = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun SetEditorPage(
    set: AppSet,
    allSets: List<AppSet>,
    isActive: Boolean,
    onChange: (AppSet) -> Unit,
    onActivate: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(set.name) }
    var renaming by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    val validate = setNameValidator(allSets, set.id)
    val nameError = validate(name)

    fun updateApps(apps: List<HomeApp>) = onChange(set.copy(apps = apps))

    fun move(index: Int, by: Int) {
        val apps = set.apps.toMutableList()
        val target = index + by
        if (target !in apps.indices) return
        apps.add(target, apps.removeAt(index))
        updateApps(apps)
    }

    SettingsPage(title = set.name, onBack = onBack) {
        OutlinedTextField(
            value = name,
            onValueChange = { input ->
                name = AppSets.sanitizeInput(input)
                // only valid names are stored, so the set never ends up unnamed or duplicated
                if (validate(name) == null) onChange(set.copy(name = AppSets.normalizeName(name)))
            },
            label = { Text(stringResource(R.string.set_name)) },
            supportingText = { Text(nameError ?: stringResource(R.string.set_name_hint, AppSets.NAME_MAX_LENGTH)) },
            isError = nameError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (isActive) {
            Text(
                stringResource(R.string.active_set),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            OutlinedButton(onClick = onActivate, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.make_active))
            }
        }

        SectionHeader(stringResource(R.string.apps_in_set))
        if (set.apps.isEmpty()) {
            Text(
                stringResource(R.string.set_is_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        set.apps.forEachIndexed { index, app ->
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { move(index, -1) }, enabled = index > 0, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                }
                IconButton(onClick = { move(index, 1) }, enabled = index < set.apps.lastIndex, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                }
                IconButton(onClick = { renaming = index }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
                }
                IconButton(
                    onClick = { updateApps(set.apps.filterIndexed { i, _ -> i != index }) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
        Button(onClick = { adding = true }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.add_apps))
        }
    }

    renaming?.let { index ->
        val app = set.apps.getOrNull(index)
        if (app == null) {
            renaming = null
        } else {
            TextInputDialog(
                title = stringResource(R.string.rename_home_entry),
                initial = app.label,
                label = stringResource(R.string.home_screen_name),
                onDismiss = { renaming = null },
                onConfirm = { text ->
                    val label = text.ifEmpty { resolveAppLabel(context, app.appPackage, app.user, app.shortcutId) ?: app.label }
                    updateApps(set.apps.mapIndexed { i, a -> if (i == index) a.copy(label = label) else a })
                    renaming = null
                },
                onReset = {
                    val label = resolveAppLabel(context, app.appPackage, app.user, app.shortcutId) ?: app.label
                    updateApps(set.apps.mapIndexed { i, a -> if (i == index) a.copy(label = label) else a })
                    renaming = null
                },
            )
        }
    }

    if (adding) {
        AppPickerDialog(
            title = stringResource(R.string.add_apps),
            multi = true,
            disabledApps = set.apps.map { it.identity }.toSet(),
            onDismiss = { adding = false },
            onConfirm = { picked ->
                updateApps(set.apps + picked.map { it.toHomeApp() })
                adding = false
            },
        )
    }
}

private fun AppModel.toHomeApp() = HomeApp(
    appPackage = appPackage,
    activityName = appActivityName,
    user = user.toString(),
    label = appLabel,
    shortcutId = shortcutId.orEmpty(),
)
