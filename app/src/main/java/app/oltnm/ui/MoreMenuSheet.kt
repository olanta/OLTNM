package app.oltnm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.oltnm.R
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes
import app.oltnm.ui.components.OltnSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuSheet(
    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
    onNavigate: (Route) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bookmarks = stringResource(R.string.bookmarks)
    val favourites = stringResource(R.string.favourites)
    val mentions = stringResource(R.string.mentions)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.more),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            OltnSectionTitle(stringResource(R.string.navigation))
            MenuRow(
                icon = Icons.Rounded.BookmarkBorder,
                label = bookmarks,
                onClick = {
                    onNavigate(
                        Route.StatusTimeline(
                            TimelineSpec(
                                id = TimelineTypes.BOOKMARKS,
                                type = TimelineTypes.BOOKMARKS
                            ),
                            bookmarks
                        )
                    )
                }
            )
            MenuRow(
                icon = Icons.Rounded.FavoriteBorder,
                label = favourites,
                onClick = {
                    onNavigate(
                        Route.StatusTimeline(
                            TimelineSpec(
                                id = TimelineTypes.FAVOURITES,
                                type = TimelineTypes.FAVOURITES
                            ),
                            favourites
                        )
                    )
                }
            )
            MenuRow(
                icon = Icons.Rounded.AlternateEmail,
                label = mentions,
                onClick = {
                    onNavigate(
                        Route.StatusTimeline(
                            TimelineSpec(
                                id = TimelineTypes.MENTIONS,
                                type = TimelineTypes.MENTIONS
                            ),
                            mentions
                        )
                    )
                }
            )
            MenuRow(
                icon = Icons.Rounded.FormatListBulleted,
                label = stringResource(R.string.lists),
                onClick = { onNavigate(Route.Lists) }
            )
            MenuRow(
                icon = Icons.Rounded.TrendingUp,
                label = stringResource(R.string.trending),
                onClick = { onNavigate(Route.Trending) }
            )
            MenuRow(
                icon = Icons.Rounded.Newspaper,
                label = stringResource(R.string.newsletter),
                onClick = { onNavigate(Route.Newsletter) }
            )
            MenuRow(
                icon = Icons.Rounded.Schedule,
                label = stringResource(R.string.scheduled_posts),
                onClick = { onNavigate(Route.Scheduled) }
            )

            Spacer(Modifier.height(8.dp))
            OltnSectionTitle(stringResource(R.string.timelines_section))
            MenuRow(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.manage_timelines),
                onClick = { onNavigate(Route.ManageTimelines) }
            )

            Spacer(Modifier.height(8.dp))
            OltnSectionTitle(stringResource(R.string.settings_title))
            MenuRow(
                icon = Icons.Rounded.PersonAdd,
                label = stringResource(R.string.add_account),
                onClick = onAddAccount
            )
            MenuRow(
                icon = Icons.Rounded.Settings,
                label = stringResource(R.string.settings_title),
                onClick = { onNavigate(Route.Settings) }
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
