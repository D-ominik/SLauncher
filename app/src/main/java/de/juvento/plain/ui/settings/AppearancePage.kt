package de.juvento.plain.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import de.juvento.plain.R
import de.juvento.plain.data.Constants
import de.juvento.plain.data.Prefs
import de.juvento.plain.style.UiStyle
import androidx.activity.compose.BackHandler

private val LightPreviewBackground = Color(0xFFD9D9D9)
private val DarkPreviewBackground = Color(0xFF202020)
private const val DEFAULT_LIGHT_CUSTOM = android.graphics.Color.BLACK
private const val DEFAULT_DARK_CUSTOM = android.graphics.Color.WHITE

private fun currentLanguage(): Constants.Language {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (locales.isEmpty) Constants.Language.System
    else Constants.Language.fromTag(locales[0]?.toLanguageTag().orEmpty())
}

/**
 * All settings of this page are edited as a draft: nothing is stored until "Apply" is pressed,
 * "Cancel" (and back) discard the draft.
 */
@Composable
fun AppearancePage(prefs: Prefs, onClose: () -> Unit) {
    val initialLanguage = remember { currentLanguage() }

    var theme by rememberSaveable { mutableStateOf(prefs.appTheme) }
    var languageTag by rememberSaveable { mutableStateOf(initialLanguage.tag) }
    val language = Constants.Language.fromTag(languageTag)
    var statusBar by rememberSaveable { mutableStateOf(prefs.showStatusBar) }
    var textSize by rememberSaveable { mutableStateOf(prefs.textSize) }
    var font by rememberSaveable { mutableStateOf(prefs.font) }
    var colorLight by rememberSaveable { mutableStateOf(prefs.textColorLight) }
    var colorDark by rememberSaveable { mutableStateOf(prefs.textColorDark) }

    // null: the preview follows the selected theme mode
    var previewNightOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var pickingLight by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val systemDark = isSystemInDarkTheme()
    val previewNight = previewNightOverride ?: when (theme) {
        Constants.Theme.Dark -> true
        Constants.Theme.Light -> false
        Constants.Theme.System -> systemDark
    }

    val dirty = theme != prefs.appTheme || language != initialLanguage || statusBar != prefs.showStatusBar ||
        textSize != prefs.textSize || font != prefs.font ||
        colorLight != prefs.textColorLight || colorDark != prefs.textColorDark

    fun applyDraft() {
        prefs.showStatusBar = statusBar
        prefs.textSize = textSize
        prefs.font = font
        prefs.textColorLight = colorLight
        prefs.textColorDark = colorDark
        val themeChanged = theme != prefs.appTheme
        prefs.appTheme = theme
        onClose()
        // These recreate the activity, so they come last.
        if (language != initialLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
        if (themeChanged) {
            AppCompatDelegate.setDefaultNightMode(
                when (theme) {
                    Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                    Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                    Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }

    BackHandler { onClose() }

    SettingsPage(
        title = stringResource(R.string.appearance),
        onBack = onClose,
        header = {
            AppearancePreview(
                night = previewNight,
                onNightChange = { previewNightOverride = it },
                font = font,
                textSize = textSize,
                color = UiStyle.effectiveColor(previewNight, if (previewNight) colorDark else colorLight),
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                Button(onClick = ::applyDraft, enabled = dirty, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.apply)) }
            }
        },
    ) {
        DropdownRow(
            title = stringResource(R.string.theme_mode),
            selected = theme,
            options = Constants.Theme.values().toList(),
            optionText = { it.string() },
            onSelect = {
                theme = it
                previewNightOverride = null
            },
        )
        SettingsDivider()
        DropdownRow(
            title = stringResource(R.string.app_language),
            selected = language,
            options = Constants.Language.all,
            optionText = { it.nativeName ?: stringResource(R.string.lang_system) },
            onSelect = { languageTag = it.tag },
        )
        SettingsDivider()
        SwitchRow(stringResource(R.string.status_bar), statusBar) { statusBar = it }
        SettingsDivider()
        SliderRow(stringResource(R.string.app_text_size), textSize, Constants.TEXT_SIZE_MIN..Constants.TEXT_SIZE_MAX) {
            textSize = it
        }
        SettingsDivider()
        DropdownRow(
            title = stringResource(R.string.font),
            selected = font,
            options = Constants.AppFont.values().toList(),
            optionText = { it.label },
            optionFont = { FontFamily(it.typeface()) },
            onSelect = { font = it },
        )

        SectionHeader(stringResource(R.string.text_color_light_theme))
        ColorSetting(
            automaticLabel = stringResource(R.string.text_color_automatic_black),
            color = colorLight,
            onAutomatic = {
                colorLight = if (it) null else DEFAULT_LIGHT_CUSTOM
                previewNightOverride = false
            },
            onPick = { pickingLight = true },
        )
        SectionHeader(stringResource(R.string.text_color_dark_theme))
        ColorSetting(
            automaticLabel = stringResource(R.string.text_color_automatic_white),
            color = colorDark,
            onAutomatic = {
                colorDark = if (it) null else DEFAULT_DARK_CUSTOM
                previewNightOverride = true
            },
            onPick = { pickingLight = false },
        )
    }

    pickingLight?.let { light ->
        ColorPickerDialog(
            initial = (if (light) colorLight else colorDark) ?: if (light) DEFAULT_LIGHT_CUSTOM else DEFAULT_DARK_CUSTOM,
            onDismiss = { pickingLight = null },
            onConfirm = { picked ->
                if (light) colorLight = picked else colorDark = picked
                previewNightOverride = !light
                pickingLight = null
            },
        )
    }
}

@Composable
private fun ColorSetting(automaticLabel: String, color: Int?, onAutomatic: (Boolean) -> Unit, onPick: () -> Unit) {
    SwitchRow(automaticLabel, checked = color == null, onCheckedChange = onAutomatic)
    if (color != null) {
        SettingsRow(
            title = stringResource(R.string.text_color_custom),
            subtitle = "#" + color.toHexRgb(),
            onClick = onPick,
            trailing = {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(Color(color or 0xFF000000.toInt()), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
        )
    }
}

/** Shows three sample entries with the draft font, size and color on a theme-colored background. */
@Composable
private fun AppearancePreview(
    night: Boolean,
    onNightChange: (Boolean) -> Unit,
    font: Constants.AppFont,
    textSize: Int,
    color: Int,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.preview), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            FilterChip(selected = !night, onClick = { onNightChange(false) }, label = { Text(stringResource(R.string.light)) })
            FilterChip(selected = night, onClick = { onNightChange(true) }, label = { Text(stringResource(R.string.dark)) })
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .heightIn(max = 168.dp)
                .background(if (night) DarkPreviewBackground else LightPreviewBackground, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            for (res in listOf(R.string.preview_text_1, R.string.preview_text_2, R.string.preview_text_3)) {
                Text(
                    text = stringResource(res),
                    fontSize = textSize.sp,
                    fontFamily = remember(font) { FontFamily(font.typeface()) },
                    color = Color(color),
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
