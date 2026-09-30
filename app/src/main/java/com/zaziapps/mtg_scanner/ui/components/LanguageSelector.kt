package com.zaziapps.mtg_scanner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.zaziapps.mtg_scanner.data.model.LanguageItem
import com.zaziapps.mtg_scanner.ui.themes.Gold
import com.zaziapps.mtg_scanner.ui.themes.Beige
import com.zaziapps.mtg_scanner.ui.themes.Brown
import com.zaziapps.mtg_scanner.ui.themes.LightBrown

/**
 * Dropdown selector interface element managing localization configurations.
 * Binds selected language profiles with interactive container states to mutate locale definitions dynamically.
 *
 * @param selectedLanguage The active [LanguageItem] configuration currently applied to the runtime context.
 * @param languages The complete collection of available [LanguageItem] choices exposed to the selection menu.
 * @param onLanguageSelected Dispatcher callback triggered when a alternative choice is committed by the user.
 * @param modifier Structural formatting constraints delegated down to the root component container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    selectedLanguage: LanguageItem,
    languages: List<LanguageItem>,
    onLanguageSelected: (LanguageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track the explicit presentation status of the dropdown anchor menu overlay.
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded },
        ) {
            // Render a read-only input anchor text element serving as the primary menu button interaction target.
            TextField(
                value = selectedLanguage.displayName,
                onValueChange = {},
                readOnly = true,
                maxLines = 1,
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
                // Apply cohesive thematic color constraints matching the global application aesthetic.
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedTextColor = Gold,
                    unfocusedTextColor = Beige,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = LightBrown,
                    unfocusedIndicatorColor = Brown.copy(alpha = 0.5f),
                    focusedTrailingIconColor = Gold
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            // Construct the popup list layer container mapping each configuration choice onto menu rows.
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = language.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (language == selectedLanguage) Gold else Beige
                            )
                        },
                        onClick = {
                            // Forward the committed language preference change down to parent configuration observers.
                            onLanguageSelected(language)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }
}
