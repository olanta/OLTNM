package app.oltnm.ui.screens.status

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Status
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.StatusCard
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDetailScreen(
    api: MastodonApi,
    statusId: String,
    interactive: Boolean,
    currentAccountId: String,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit,
    onReply: (Status) -> Unit,
    onQuote: (Status) -> Unit,
    onEdit: (Status) -> Unit
) {
    val viewModel: StatusDetailViewModel = viewModel(
        key = "status-detail-${api.hashCode()}-$statusId",
        factory = factoryOf { StatusDetailViewModel(api, statusId, interactive) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportComment by remember { mutableStateOf("") }
    val editableStatus = state.status?.takeIf {
        interactive && it.original.account.id == currentAccountId && it.poll == null
    }
    val moderationStatus = state.status?.original?.takeIf {
        interactive && it.account.id != currentAccountId
    }

    LaunchedEffect(state.moderationEvent) {
        val event = state.moderationEvent ?: return@LaunchedEffect
        Toast.makeText(
            context,
            when (event) {
                ModerationEvent.Blocked -> R.string.user_blocked
                ModerationEvent.Reported -> R.string.report_sent
            },
            Toast.LENGTH_SHORT
        ).show()
        viewModel.clearModerationEvent()
    }

    val accountCallback: (String) -> Unit = if (interactive) {
        onOpenAccount
    } else {
        { accountId ->
            val statuses = listOfNotNull(state.status) + state.ancestors + state.descendants
            statuses.asSequence()
                .map { it.original.account }
                .firstOrNull { it.id == accountId }
                ?.url
                ?.takeIf { it.isNotBlank() }
                ?.let(openUrl)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.status_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                if (moderationStatus != null) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.more)
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            R.string.block_user,
                                            moderationStatus.account.acct
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Block,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showBlockConfirm = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            R.string.report_user,
                                            moderationStatus.account.acct
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Flag,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                }
            }
        )

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                state.loading && state.status == null -> LoadingView()
                state.status == null && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = viewModel::refresh
                )
                state.status == null -> LoadingView()
                else -> {
                    val status = state.status
                    if (status == null) {
                        LoadingView()
                    } else {
                        val threadById = (
                            listOfNotNull(status) + state.ancestors + state.descendants
                            ).associateBy { it.id }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            state.error?.let { message ->
                                item(key = "error") {
                                    ErrorBanner(
                                        message = message,
                                        onDismiss = viewModel::clearError
                                    )
                                }
                            }
                            state.actionError?.let { message ->
                                item(key = "action-error") {
                                    ErrorBanner(
                                        message = message,
                                        onDismiss = viewModel::clearActionError
                                    )
                                }
                            }
                            item(key = "status") {
                                FocusedStatusCard {
                                    StatusCard(
                                        status = status,
                                        onOpenAccount = accountCallback,
                                        onOpenStatus = {},
                                        onReply = onReply,
                                        onQuote = onQuote,
                                        onOpenUrl = openUrl,
                                        onFavourite = viewModel::toggleFavourite,
                                        onReblog = viewModel::toggleReblog,
                                        onBookmark = viewModel::toggleBookmark,
                                        onEdit = if (editableStatus != null) onEdit else null,
                                        interactive = interactive
                                    )
                                }
                            }
                            if (state.ancestors.isNotEmpty()) {
                                item(key = "ancestors-title") {
                                    OltnSectionTitle(
                                        text = stringResource(R.string.ancestors_title),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                                items(state.ancestors, key = { "ancestor-${it.id}" }) { ancestor ->
                                    StatusCard(
                                        status = ancestor,
                                        onOpenAccount = accountCallback,
                                        onOpenStatus = { onOpenStatus(it.original.id) },
                                        onReply = onReply,
                                        onQuote = onQuote,
                                        onOpenUrl = openUrl,
                                        onFavourite = viewModel::toggleFavourite,
                                        onReblog = viewModel::toggleReblog,
                                        onBookmark = viewModel::toggleBookmark,
                                        onEdit = if (
                                            interactive &&
                                            ancestor.original.account.id == currentAccountId &&
                                            ancestor.poll == null
                                        ) {
                                            onEdit
                                        } else {
                                            null
                                        },
                                        interactive = interactive
                                    )
                                }
                            }
                            if (state.descendants.isNotEmpty()) {
                                item(key = "descendants-title") {
                                    OltnSectionTitle(
                                        text = stringResource(R.string.descendants_title),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                                items(state.descendants, key = { "descendant-${it.id}" }) { descendant ->
                                    ThreadReplyCard(
                                        depth = replyDepth(descendant, threadById)
                                    ) {
                                        StatusCard(
                                            status = descendant,
                                            onOpenAccount = accountCallback,
                                            onOpenStatus = { onOpenStatus(it.original.id) },
                                            onReply = onReply,
                                            onQuote = onQuote,
                                            onOpenUrl = openUrl,
                                            onFavourite = viewModel::toggleFavourite,
                                            onReblog = viewModel::toggleReblog,
                                            onBookmark = viewModel::toggleBookmark,
                                            onEdit = if (
                                                interactive &&
                                                descendant.original.account.id == currentAccountId &&
                                                descendant.poll == null
                                            ) {
                                                onEdit
                                            } else {
                                                null
                                            },
                                            interactive = interactive
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBlockConfirm && moderationStatus != null) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = {
                Text(
                    stringResource(
                        R.string.block_user,
                        moderationStatus.account.acct
                    )
                )
            },
            text = { Text(stringResource(R.string.block_user_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockConfirm = false
                        viewModel.blockAccount(moderationStatus.account.id)
                    }
                ) {
                    Text(stringResource(R.string.block))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showReportDialog && moderationStatus != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    stringResource(
                        R.string.report_user,
                        moderationStatus.account.acct
                    )
                )
            },
            text = {
                OltnTextField(
                    value = reportComment,
                    onValueChange = { reportComment = it },
                    placeholder = stringResource(R.string.report_comment_hint),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        viewModel.reportAccount(
                            accountId = moderationStatus.account.id,
                            statusId = moderationStatus.id,
                            comment = reportComment
                        )
                        reportComment = ""
                    }
                ) {
                    Text(stringResource(R.string.report))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        reportComment = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun replyDepth(status: Status, byId: Map<String, Status>): Int {
    var depth = 0
    var current: Status? = status
    while (depth < 6) {
        val parentId = current?.inReplyToId ?: break
        current = byId[parentId] ?: break
        depth += 1
    }
    return depth
}

@Composable
private fun FocusedStatusCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.primary, shape)
    ) {
        content()
    }
}

@Composable
private fun ThreadReplyCard(
    depth: Int,
    content: @Composable () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (12 + depth.coerceIn(0, 6) * 12).dp)
            .drawBehind {
                drawLine(
                    color = accent,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
    ) {
        content()
    }
}
