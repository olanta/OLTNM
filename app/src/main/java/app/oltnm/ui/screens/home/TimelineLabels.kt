package app.oltnm.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.oltnm.R
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes

@Composable
fun timelineLabel(spec: TimelineSpec): String = when (spec.type) {
    TimelineTypes.HOME -> stringResource(R.string.nav_home)
    TimelineTypes.LOCAL -> stringResource(R.string.timeline_local)
    TimelineTypes.FEDERATED -> stringResource(R.string.timeline_federated)
    TimelineTypes.BOOKMARKS -> stringResource(R.string.bookmarks)
    TimelineTypes.FAVOURITES -> stringResource(R.string.favourites)
    TimelineTypes.MENTIONS -> stringResource(R.string.mentions)
    else -> spec.label.ifBlank {
        when {
            spec.type == TimelineTypes.HASHTAG && !spec.value.isNullOrBlank() -> "#${spec.value}"
            spec.type.startsWith("remote-") && !spec.host.isNullOrBlank() -> spec.host
            spec.type == TimelineTypes.LIST && !spec.value.isNullOrBlank() -> spec.value
            else -> spec.type
        }
    }
}
