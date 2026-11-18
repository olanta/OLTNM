package app.oltnm.ui.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.oltnm.R
import app.oltnm.data.cleanup.CleanupMinAgeSeconds
import app.oltnm.data.model.CleanupPolicy
import app.oltnm.data.store.CleanupStore
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnGlassCard
import app.oltnm.ui.components.OltnTextField

private val ageLabels = listOf(
    CleanupMinAgeSeconds[0] to R.string.cleanup_age_1_week,
    CleanupMinAgeSeconds[1] to R.string.cleanup_age_2_weeks,
    CleanupMinAgeSeconds[2] to R.string.cleanup_age_1_month,
    CleanupMinAgeSeconds[3] to R.string.cleanup_age_2_months,
    CleanupMinAgeSeconds[4] to R.string.cleanup_age_3_months,
    CleanupMinAgeSeconds[5] to R.string.cleanup_age_6_months,
    CleanupMinAgeSeconds[6] to R.string.cleanup_age_1_year,
    CleanupMinAgeSeconds[7] to R.string.cleanup_age_2_years
)

@Composable
fun CleanupSection(
    store: CleanupStore,
    sessionKey: String
) {
    var accountState by remember(sessionKey) { mutableStateOf(store.load(sessionKey)) }
    val policy = accountState.policy

    fun update(next: CleanupPolicy) {
        accountState = accountState.copy(policy = next)
        store.savePolicy(sessionKey, next)
    }

    OltnGlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cleanup_enable),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.cleanup_enable_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = policy.enabled,
                onCheckedChange = { update(policy.copy(enabled = it)) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.cleanup_age),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ageLabels.forEach { (seconds, label) ->
                OltnFilterChip(
                    selected = policy.minStatusAgeSeconds == seconds,
                    onClick = { update(policy.copy(minStatusAgeSeconds = seconds)) },
                    label = stringResource(label),
                    enabled = policy.enabled
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.cleanup_age_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.cleanup_exceptions),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        CleanupToggle(
            label = stringResource(R.string.cleanup_keep_pinned),
            checked = policy.keepPinned,
            enabled = policy.enabled,
            onChange = { update(policy.copy(keepPinned = it)) }
        )
        CleanupToggle(
            label = stringResource(R.string.cleanup_keep_direct),
            checked = policy.keepDirect,
            enabled = policy.enabled,
            onChange = { update(policy.copy(keepDirect = it)) }
        )
        CleanupToggle(
            label = stringResource(R.string.cleanup_keep_favourited),
            checked = policy.keepFavourited,
            enabled = policy.enabled,
            onChange = { update(policy.copy(keepFavourited = it)) }
        )
        CleanupToggle(
            label = stringResource(R.string.cleanup_keep_bookmarked),
            checked = policy.keepBookmarked,
            enabled = policy.enabled,
            onChange = { update(policy.copy(keepBookmarked = it)) }
        )
        CleanupToggle(
            label = stringResource(R.string.cleanup_keep_polls),
            checked = policy.keepPolls,
            enabled = policy.enabled,
            onChange = { update(policy.copy(keepPolls = it)) }
        )
        CleanupToggle(
            label = stringResource(R.string.cleanup_keep_media),
            checked = policy.keepMedia,
            enabled = policy.enabled,
            onChange = { update(policy.copy(keepMedia = it)) }
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.cleanup_interactions),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OltnTextField(
            value = policy.minFavourites.takeIf { it > 0 }?.toString().orEmpty(),
            onValueChange = { value ->
                update(policy.copy(minFavourites = value.filter(Char::isDigit).toIntOrNull() ?: 0))
            },
            placeholder = stringResource(R.string.cleanup_min_favourites),
            enabled = policy.enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OltnTextField(
            value = policy.minBoosts.takeIf { it > 0 }?.toString().orEmpty(),
            onValueChange = { value ->
                update(policy.copy(minBoosts = value.filter(Char::isDigit).toIntOrNull() ?: 0))
            },
            placeholder = stringResource(R.string.cleanup_min_boosts),
            enabled = policy.enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.cleanup_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CleanupToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled
        )
    }
}
