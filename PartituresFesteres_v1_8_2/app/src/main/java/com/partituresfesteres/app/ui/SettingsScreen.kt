package com.partituresfesteres.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.partituresfesteres.app.R
import com.partituresfesteres.app.data.AppSettings
import com.partituresfesteres.app.data.LanguageManager
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.MutedInk
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSave: () -> Unit,
    onSelectRoot: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    backupMessage: String?,
    onLibraryClick: () -> Unit,
    onRepertoiresClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onToolsClick: () -> Unit,
    onAddContentClick: () -> Unit,
) {
    AdaptiveNavigationScaffold(
        activeSection = AppSection.SETTINGS,
        onLibraryClick = onLibraryClick,
        onRepertoiresClick = onRepertoiresClick,
        onRecentsClick = onRecentsClick,
        onFavoritesClick = onFavoritesClick,
        onToolsClick = onToolsClick,
        onAddContentClick = onAddContentClick,
    ) {
        val compact = LocalAdaptiveWindowSize.current == AdaptiveWindowSize.COMPACT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 10.dp else 26.dp,
                    end = if (compact) 10.dp else 34.dp,
                    top = if (compact) 7.dp else 18.dp,
                    bottom = if (compact) 7.dp else 22.dp,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CimoPlaceholder(size = if (compact) 40.dp else 64.dp)
                Spacer(Modifier.width(if (compact) 7.dp else 12.dp))
                Column {
                    Text(
                        stringResource(R.string.partitures_festeres),
                        fontSize = if (compact) 24.sp else 31.sp,
                        color = Burgundy,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.settings_title),
                        fontSize = if (compact) 18.sp else 22.sp,
                        color = Navy,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Settings, contentDescription = null, tint = Ink, modifier = Modifier.size(if (compact) 25.dp else 30.dp))
            }

            Spacer(Modifier.height(if (compact) 7.dp else 12.dp))

            if (compact) {
                FestivePanel(
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 18.dp,
                    backgroundColor = ParchmentCard,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                    ) {
                        Text(stringResource(R.string.viewer_title), color = Burgundy, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.settings_viewer_desc), color = MutedInk, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))

                        LanguageSelector(language = settings.appLanguage, onLanguageChange = onLanguageChange)
                        Spacer(Modifier.height(10.dp))

                        SettingSliderRow(
                            label = stringResource(R.string.settings_menu_hide),
                            valueText = stringResource(R.string.seconds_format, settings.menuHideSeconds),
                            value = settings.menuHideSeconds.toFloat(),
                            range = 1f..10f,
                            steps = 8,
                            onChange = { onSettingsChange(settings.copy(menuHideSeconds = it.roundToInt())) },
                        )
                        SettingToggleRow(stringResource(R.string.settings_show_score_name), settings.showScoreNameOnChange) {
                            onSettingsChange(settings.copy(showScoreNameOnChange = it))
                        }
                        SettingToggleRow(stringResource(R.string.settings_remember_zoom), settings.rememberZoom) {
                            onSettingsChange(settings.copy(rememberZoom = it))
                        }
                        SettingToggleRow(stringResource(R.string.settings_keep_screen_on), settings.keepScreenOn) {
                            onSettingsChange(settings.copy(keepScreenOn = it))
                        }
                        SettingToggleRow(stringResource(R.string.settings_allow_rotation), settings.allowViewerRotation) {
                            onSettingsChange(settings.copy(allowViewerRotation = it))
                        }
                        SettingSliderRow(
                            label = stringResource(R.string.settings_side_zones),
                            valueText = stringResource(R.string.percent_format, (settings.sideTapFraction * 100).roundToInt()),
                            value = settings.sideTapFraction,
                            range = 0.12f..0.32f,
                            steps = 9,
                            onChange = { onSettingsChange(settings.copy(sideTapFraction = it)) },
                        )
                        SettingToggleRow(stringResource(R.string.settings_brightness_override), settings.overrideBrightness) {
                            onSettingsChange(settings.copy(overrideBrightness = it))
                        }
                        if (settings.overrideBrightness) {
                            SettingSliderRow(
                                label = stringResource(R.string.settings_viewer_brightness),
                                valueText = stringResource(R.string.percent_format, settings.brightnessPercent),
                                value = settings.brightnessPercent.toFloat(),
                                range = 20f..100f,
                                steps = 7,
                                onChange = { onSettingsChange(settings.copy(brightnessPercent = it.roundToInt())) },
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onSave,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(stringResource(R.string.settings_save_changes)) }

                        Spacer(Modifier.height(20.dp))
                        Text(stringResource(R.string.settings_library_data), color = Burgundy, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(9.dp))
                        SettingsActionButton(Icons.Default.FolderOpen, stringResource(R.string.settings_select_root), onSelectRoot)
                        Spacer(Modifier.height(8.dp))
                        SettingsActionButton(Icons.Default.Backup, stringResource(R.string.settings_create_backup), onCreateBackup)
                        Spacer(Modifier.height(8.dp))
                        SettingsActionButton(Icons.Default.Restore, stringResource(R.string.settings_restore_backup), onRestoreBackup)

                        if (!backupMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(backupMessage, color = Navy, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AgedGold)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.partitures_festeres), color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.settings_version), color = MutedInk, fontSize = 12.sp)
                                Text(stringResource(R.string.settings_backup_desc), color = MutedInk, fontSize = 11.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(stringResource(R.string.app_copyright), color = MutedInk.copy(alpha = 0.82f), fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FestivePanel(
                        modifier = Modifier.weight(0.64f).fillMaxHeight(),
                        cornerRadius = 20.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(18.dp),
                        ) {
                            Text(stringResource(R.string.viewer_title), color = Burgundy, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.settings_viewer_desc), color = MutedInk, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))

                            LanguageSelector(language = settings.appLanguage, onLanguageChange = onLanguageChange)
                            Spacer(Modifier.height(12.dp))

                            SettingSliderRow(
                                label = stringResource(R.string.settings_menu_hide),
                                valueText = stringResource(R.string.seconds_format, settings.menuHideSeconds),
                                value = settings.menuHideSeconds.toFloat(),
                                range = 1f..10f,
                                steps = 8,
                                onChange = { onSettingsChange(settings.copy(menuHideSeconds = it.roundToInt())) },
                            )
                            SettingToggleRow(stringResource(R.string.settings_show_score_name), settings.showScoreNameOnChange) {
                                onSettingsChange(settings.copy(showScoreNameOnChange = it))
                            }
                            SettingToggleRow(stringResource(R.string.settings_remember_zoom), settings.rememberZoom) {
                                onSettingsChange(settings.copy(rememberZoom = it))
                            }
                            SettingToggleRow(stringResource(R.string.settings_keep_screen_on), settings.keepScreenOn) {
                                onSettingsChange(settings.copy(keepScreenOn = it))
                            }
                            SettingToggleRow(stringResource(R.string.settings_allow_rotation), settings.allowViewerRotation) {
                                onSettingsChange(settings.copy(allowViewerRotation = it))
                            }
                            SettingSliderRow(
                                label = stringResource(R.string.settings_side_zones),
                                valueText = stringResource(R.string.percent_format, (settings.sideTapFraction * 100).roundToInt()),
                                value = settings.sideTapFraction,
                                range = 0.12f..0.32f,
                                steps = 9,
                                onChange = { onSettingsChange(settings.copy(sideTapFraction = it)) },
                            )
                            SettingToggleRow(stringResource(R.string.settings_brightness_override), settings.overrideBrightness) {
                                onSettingsChange(settings.copy(overrideBrightness = it))
                            }
                            if (settings.overrideBrightness) {
                                SettingSliderRow(
                                    label = stringResource(R.string.settings_viewer_brightness),
                                    valueText = stringResource(R.string.percent_format, settings.brightnessPercent),
                                    value = settings.brightnessPercent.toFloat(),
                                    range = 20f..100f,
                                    steps = 7,
                                    onChange = { onSettingsChange(settings.copy(brightnessPercent = it.roundToInt())) },
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onSave,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text(stringResource(R.string.settings_save_changes)) }
                        }
                    }

                    FestivePanel(
                        modifier = Modifier.weight(0.36f).fillMaxHeight(),
                        cornerRadius = 20.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        Column(Modifier.fillMaxSize().padding(18.dp)) {
                            Text(stringResource(R.string.settings_library_data), color = Burgundy, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            SettingsActionButton(Icons.Default.FolderOpen, stringResource(R.string.settings_select_root), onSelectRoot)
                            Spacer(Modifier.height(10.dp))
                            SettingsActionButton(Icons.Default.Backup, stringResource(R.string.settings_create_backup), onCreateBackup)
                            Spacer(Modifier.height(10.dp))
                            SettingsActionButton(Icons.Default.Restore, stringResource(R.string.settings_restore_backup), onRestoreBackup)

                            if (!backupMessage.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(backupMessage, color = Navy, fontSize = 13.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AgedGold)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(stringResource(R.string.partitures_festeres), color = Ink, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.settings_version), color = MutedInk, fontSize = 12.sp)
                                    Text(stringResource(R.string.settings_backup_desc), color = MutedInk, fontSize = 11.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(stringResource(R.string.app_copyright), color = MutedInk.copy(alpha = 0.82f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun LanguageSelector(
    language: String,
    onLanguageChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_language), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(5.dp))
        Box {
            FestiveButton(
                onClick = { expanded = true },
                backgroundColor = ParchmentCard,
                contentColor = Navy,
                cornerRadius = 12.dp,
            ) {
                Text(
                    if (language == LanguageManager.LANGUAGE_SPANISH) {
                        stringResource(R.string.language_spanish)
                    } else {
                        stringResource(R.string.language_valencian)
                    },
                    color = Navy,
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Navy)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(onClick = {
                    expanded = false
                    if (language != LanguageManager.LANGUAGE_VALENCIAN) onLanguageChange(LanguageManager.LANGUAGE_VALENCIAN)
                }) { Text(stringResource(R.string.language_valencian)) }
                DropdownMenuItem(onClick = {
                    expanded = false
                    if (language != LanguageManager.LANGUAGE_SPANISH) onLanguageChange(LanguageManager.LANGUAGE_SPANISH)
                }) { Text(stringResource(R.string.language_spanish)) }
            }
        }
        Text(stringResource(R.string.settings_language_hint), color = MutedInk, fontSize = 11.sp)
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Burgundy),
        )
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = Navy, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = androidx.compose.material.SliderDefaults.colors(
                thumbColor = Burgundy,
                activeTrackColor = Burgundy,
                inactiveTrackColor = AgedGold.copy(alpha = 0.35f),
            ),
        )
    }
}

@Composable
private fun SettingsActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FestiveButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = ParchmentCard,
        contentColor = Navy,
        cornerRadius = 12.dp,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Navy)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = Navy)
    }
}
