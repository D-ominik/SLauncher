package de.juvento.plain.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.juvento.plain.BuildConfig
import de.juvento.plain.R
import de.juvento.plain.data.AppSet
import de.juvento.plain.data.Constants
import de.juvento.plain.data.Prefs

/** Actions of the settings that need the hosting fragment/activity. */
interface SettingsHost {
    fun close()
    fun openHiddenApps()
    fun openOwnAppInfo()
    fun chooseDefaultLauncher()
    fun exportBackup()
    fun importBackup()
}

private enum class Page { Main, Appearance, Behavior, HomeScreen, SetEditor, Alignment, Gestures, Backup }

@Composable
fun SettingsScreen(host: SettingsHost) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var page by rememberSaveable { mutableStateOf(Page.Main) }
    var editingSetId by rememberSaveable { mutableStateOf<String?>(null) }

    // The app sets are shared by the list page and the editor page.
    var sets by remember { mutableStateOf(prefs.appSets) }
    var activeSetId by remember { mutableStateOf(prefs.activeSet.id) }

    fun saveSets(newSets: List<AppSet>) {
        sets = newSets
        prefs.appSets = newSets
    }

    fun activate(id: String) {
        activeSetId = id
        prefs.activeSetId = id
    }

    BackHandler(enabled = page != Page.Main) {
        page = if (page == Page.SetEditor) Page.HomeScreen else Page.Main
    }

    val back = { page = Page.Main }

    // The edited set may have vanished (e.g. after restoring state); fall back to the list.
    val editedSet = sets.firstOrNull { it.id == editingSetId }
    val shownPage = if (page == Page.SetEditor && editedSet == null) Page.HomeScreen else page

    when (shownPage) {
        Page.Main -> MainPage(host, onNavigate = { page = it.toPage() })
        Page.Appearance -> AppearancePage(prefs, onClose = back)
        Page.Behavior -> BehaviorPage(prefs, onBack = back)
        Page.Alignment -> AlignmentPage(prefs, onBack = back)
        Page.Gestures -> GesturesPage(prefs, onBack = back)
        Page.Backup -> BackupPage(host, onBack = back)
        Page.HomeScreen -> HomeScreenPage(
            prefs = prefs,
            sets = sets,
            activeSetId = activeSetId,
            onSetsChange = ::saveSets,
            onActivate = ::activate,
            onEdit = { id ->
                editingSetId = id
                page = Page.SetEditor
            },
            onBack = back,
        )
        Page.SetEditor -> SetEditorPage(
            set = editedSet!!,
            allSets = sets,
            isActive = editedSet.id == activeSetId,
            onChange = { updated -> saveSets(sets.map { if (it.id == updated.id) updated else it }) },
            onActivate = { activate(editedSet.id) },
            onBack = { page = Page.HomeScreen },
        )
    }
}

enum class SettingsSection { Appearance, Behavior, HomeScreen, Alignment, Gestures, Backup }

private fun SettingsSection.toPage(): Page = when (this) {
    SettingsSection.Appearance -> Page.Appearance
    SettingsSection.Behavior -> Page.Behavior
    SettingsSection.HomeScreen -> Page.HomeScreen
    SettingsSection.Alignment -> Page.Alignment
    SettingsSection.Gestures -> Page.Gestures
    SettingsSection.Backup -> Page.Backup
}

@Composable
private fun MainPage(host: SettingsHost, onNavigate: (SettingsSection) -> Unit) {
    SettingsPage(title = stringResource(R.string.settings), onBack = host::close) {
        NavigationRow(stringResource(R.string.hidden_apps), onClick = host::openHiddenApps)
        SettingsDivider()
        NavigationRow(stringResource(R.string.app_info), onClick = host::openOwnAppInfo)
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.version_info),
            subtitle = "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
        SettingsDivider()
        NavigationRow(stringResource(R.string.choose_default_launcher), onClick = host::chooseDefaultLauncher)

        SectionHeader(stringResource(R.string.settings_sections))
        NavigationRow(stringResource(R.string.appearance)) { onNavigate(SettingsSection.Appearance) }
        SettingsDivider()
        NavigationRow(stringResource(R.string.behavior)) { onNavigate(SettingsSection.Behavior) }
        SettingsDivider()
        NavigationRow(stringResource(R.string.homescreen)) { onNavigate(SettingsSection.HomeScreen) }
        SettingsDivider()
        NavigationRow(stringResource(R.string.alignment)) { onNavigate(SettingsSection.Alignment) }
        SettingsDivider()
        NavigationRow(stringResource(R.string.gestures)) { onNavigate(SettingsSection.Gestures) }
        SettingsDivider()
        NavigationRow(stringResource(R.string.backup)) { onNavigate(SettingsSection.Backup) }
    }
}

@Composable
private fun BehaviorPage(prefs: Prefs, onBack: () -> Unit) {
    var autoKeyboard by remember { mutableStateOf(prefs.autoShowKeyboard) }
    var autoOpen by remember { mutableStateOf(prefs.autoOpenApp) }
    var orientation by remember { mutableStateOf(prefs.orientation) }

    SettingsPage(title = stringResource(R.string.behavior), onBack = onBack) {
        DropdownRow(
            title = stringResource(R.string.screen_orientation),
            selected = orientation,
            options = Constants.Orientation.values().toList(),
            optionText = { it.string() },
        ) {
            orientation = it
            prefs.orientation = it
        }
        SettingsDivider()
        SwitchRow(stringResource(R.string.auto_show_keyboard), autoKeyboard) {
            autoKeyboard = it
            prefs.autoShowKeyboard = it
        }
        SettingsDivider()
        SwitchRow(stringResource(R.string.auto_open_apps), autoOpen, subtitle = stringResource(R.string.auto_open_apps_hint)) {
            autoOpen = it
            prefs.autoOpenApp = it
        }
    }
}

@Composable
private fun AlignmentPage(prefs: Prefs, onBack: () -> Unit) {
    var home by remember { mutableStateOf(prefs.homeAlignment) }
    var bottom by remember { mutableStateOf(prefs.homeAlignmentBottom) }
    var clock by remember { mutableStateOf(prefs.clockAlignment) }
    var drawer by remember { mutableStateOf(prefs.drawerAlignment) }
    val gravities = Constants.Gravity.values().toList()

    SettingsPage(title = stringResource(R.string.alignment), onBack = onBack) {
        DropdownRow(stringResource(R.string.home_alignment), home, gravities, { it.string() }) {
            home = it
            prefs.homeAlignment = it
        }
        SettingsDivider()
        SwitchRow(stringResource(R.string.home_alignment_bottom), bottom) {
            bottom = it
            prefs.homeAlignmentBottom = it
        }
        SettingsDivider()
        DropdownRow(stringResource(R.string.clock_alignment), clock, gravities, { it.string() }) {
            clock = it
            prefs.clockAlignment = it
        }
        SettingsDivider()
        DropdownRow(stringResource(R.string.drawer_alignment), drawer, gravities, { it.string() }) {
            drawer = it
            prefs.drawerAlignment = it
        }
    }
}

@Composable
private fun BackupPage(host: SettingsHost, onBack: () -> Unit) {
    SettingsPage(title = stringResource(R.string.backup), onBack = onBack) {
        SettingsRow(
            title = stringResource(R.string.store_backup),
            subtitle = stringResource(R.string.store_backup_hint),
            onClick = host::exportBackup,
        )
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.load_backup),
            subtitle = stringResource(R.string.load_backup_hint),
            onClick = host::importBackup,
        )
    }
}
