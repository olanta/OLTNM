package app.oltnm.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.oltnm.BuildConfig
import app.oltnm.R
import app.oltnm.data.model.Accent
import app.oltnm.data.model.FontFamilyChoice
import app.oltnm.data.model.LanguageChoice
import app.oltnm.data.model.Session
import app.oltnm.data.model.TextColorChoice
import app.oltnm.data.model.TextSizeChoice
import app.oltnm.data.model.ThemeMode
import app.oltnm.di.AppContainer
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnGlassCard
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.theme.color

@Composable
fun SettingsScreen(
    container: AppContainer,
    session: Session,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val themeMode by container.preferences.themeMode.collectAsStateWithLifecycle()
    val accent by container.preferences.accent.collectAsStateWithLifecycle()
    val language by container.preferences.language.collectAsStateWithLifecycle()
    val fontFamily by container.preferences.fontFamily.collectAsStateWithLifecycle()
    val textSize by container.preferences.textSize.collectAsStateWithLifecycle()
    val textColor by container.preferences.textColor.collectAsStateWithLifecycle()
    val dark = themeMode == ThemeMode.Dark ||
        (themeMode == ThemeMode.System && isSystemInDarkTheme())

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OltnGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(url = session.avatar, size = 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "@${session.acct}@${session.host}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            OltnSectionTitle(stringResource(R.string.settings_appearance))
            Spacer(Modifier.height(8.dp))
            OltnGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.theme_mode),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OltnFilterChip(
                        selected = themeMode == ThemeMode.Dark,
                        onClick = { container.preferences.setThemeMode(ThemeMode.Dark) },
                        label = stringResource(R.string.theme_dark)
                    )
                    OltnFilterChip(
                        selected = themeMode == ThemeMode.Light,
                        onClick = { container.preferences.setThemeMode(ThemeMode.Light) },
                        label = stringResource(R.string.theme_light)
                    )
                    OltnFilterChip(
                        selected = themeMode == ThemeMode.System,
                        onClick = { container.preferences.setThemeMode(ThemeMode.System) },
                        label = stringResource(R.string.theme_auto)
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.accent_color),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Accent.entries.forEach { item ->
                        val selected = accent == item
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(item.color())
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable { container.preferences.setAccent(item) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = accentLabel(accent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.settings_font),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontFamilyChoice.entries.forEach { item ->
                        OltnFilterChip(
                            selected = fontFamily == item,
                            onClick = { container.preferences.setFontFamily(item) },
                            label = fontFamilyLabel(item)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.settings_text_size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextSizeChoice.entries.forEach { item ->
                        OltnFilterChip(
                            selected = textSize == item,
                            onClick = { container.preferences.setTextSize(item) },
                            label = textSizeLabel(item)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.settings_text_color),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextColorChoice.entries.forEach { item ->
                        val selected = textColor == item
                        val preview = item.color(dark) ?: MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(preview)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable { container.preferences.setTextColor(item) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = textColorLabel(textColor),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageChoice.entries.forEach { item ->
                        OltnFilterChip(
                            selected = language == item,
                            onClick = {
                                if (language != item) {
                                    container.preferences.setLanguage(item)
                                    activity?.recreate()
                                }
                            },
                            label = languageLabel(item)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.language_restart_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            OltnSectionTitle(stringResource(R.string.settings_account))
            Spacer(Modifier.height(8.dp))
            OltnGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                SettingsRow(stringResource(R.string.settings_instance), session.host)
                SettingsRow(
                    stringResource(R.string.settings_version),
                    "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                )
                SettingsRow(stringResource(R.string.settings_client), "OLTN Mobile")
            }

            Spacer(Modifier.height(20.dp))
            OltnSectionTitle(stringResource(R.string.settings_about))
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            OutlinedButton(
                onClick = { confirmLogout = true },
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
            ) {
                Text(
                    text = stringResource(R.string.logout),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun accentLabel(accent: Accent): String = when (accent) {
    Accent.Green -> stringResource(R.string.accent_green)
    Accent.Orange -> stringResource(R.string.accent_orange)
    Accent.Purple -> stringResource(R.string.accent_purple)
    Accent.Blue -> stringResource(R.string.accent_blue)
    Accent.Pink -> stringResource(R.string.accent_pink)
}

@Composable
private fun fontFamilyLabel(font: FontFamilyChoice): String = when (font) {
    FontFamilyChoice.SpaceGrotesk -> stringResource(R.string.font_space_grotesk)
    FontFamilyChoice.Atkinson -> stringResource(R.string.font_atkinson)
    FontFamilyChoice.System -> stringResource(R.string.font_system)
    FontFamilyChoice.Serif -> stringResource(R.string.font_serif)
    FontFamilyChoice.Monospace -> stringResource(R.string.font_monospace)
}

@Composable
private fun textSizeLabel(size: TextSizeChoice): String = when (size) {
    TextSizeChoice.Small -> stringResource(R.string.text_size_small)
    TextSizeChoice.Default -> stringResource(R.string.text_size_default)
    TextSizeChoice.Large -> stringResource(R.string.text_size_large)
    TextSizeChoice.ExtraLarge -> stringResource(R.string.text_size_extra_large)
}

@Composable
private fun textColorLabel(color: TextColorChoice): String = when (color) {
    TextColorChoice.Default -> stringResource(R.string.text_color_default)
    TextColorChoice.HighContrast -> stringResource(R.string.text_color_high_contrast)
    TextColorChoice.Cream -> stringResource(R.string.text_color_cream)
    TextColorChoice.Yellow -> stringResource(R.string.text_color_yellow)
    TextColorChoice.Mint -> stringResource(R.string.text_color_mint)
    TextColorChoice.Blue -> stringResource(R.string.text_color_blue)
    TextColorChoice.Pink -> stringResource(R.string.text_color_pink)
}

@Composable
private fun languageLabel(language: LanguageChoice): String = when (language) {
    LanguageChoice.System -> stringResource(R.string.language_system)
    LanguageChoice.English -> stringResource(R.string.language_english)
    LanguageChoice.PortugueseBrazil -> stringResource(R.string.language_portuguese)
}
