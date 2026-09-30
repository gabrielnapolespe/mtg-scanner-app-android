package com.zaziapps.mtg_scanner.data.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.zaziapps.mtg_scanner.R

/**
 * Data architecture component tracking an individual language choice configuration profile.
 * Pairs a standard two-letter ISO language code with its corresponding UI display label.
 *
 * @property code The target localization ISO short string key identifier.
 * @property displayName The fully localized user-facing textual name representation for the language option.
 */
data class LanguageItem(
    val code: String,
    val displayName: String
)

/**
 * Generates and caches an updated collection of available system language configurations.
 * Resolves current resource bindings from the context environment layer to maintain localization synchronization.
 *
 * @return A collection of initialized [LanguageItem] configurations ready for consumer menu popups.
 */
@Composable
fun rememberAvailableLanguages(): List<LanguageItem> {
    // Resolve localized label strings dynamically out of the application resource distribution bundles.
    val spanishLabel = stringResource(id = R.string.spanish_label)
    val englishLabel = stringResource(id = R.string.english_label)
    val germanLabel = stringResource(id = R.string.german_label)
    val italianLabel = stringResource(id = R.string.italian_label)
    val frenchLabel = stringResource(id = R.string.french_label)

    // Cache the collection structures explicitly to prevent redundant list allocations during runtime layout recomposition.
    return remember(spanishLabel, englishLabel, germanLabel, italianLabel, frenchLabel) {
        listOf(
            LanguageItem("es", spanishLabel),
            LanguageItem("en", englishLabel),
            LanguageItem("de", germanLabel),
            LanguageItem("it", italianLabel),
            LanguageItem("fr", frenchLabel)
        )
    }
}
