package app.oltnm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.oltnm.R
import app.oltnm.data.model.Session
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes
import app.oltnm.di.AppContainer
import app.oltnm.ui.screens.compose.ComposeScreen
import app.oltnm.ui.screens.home.StatusTimelineScreen
import app.oltnm.ui.screens.home.TimelineScreen
import app.oltnm.ui.screens.lists.ListsScreen
import app.oltnm.ui.screens.login.LoginScreen
import app.oltnm.ui.screens.newsletter.NewsletterScreen
import app.oltnm.ui.screens.notifications.NotificationsScreen
import app.oltnm.ui.screens.profile.ProfileScreen
import app.oltnm.ui.screens.scheduled.ScheduledScreen
import app.oltnm.ui.screens.search.SearchScreen
import app.oltnm.ui.screens.settings.SettingsScreen
import app.oltnm.ui.screens.status.StatusDetailScreen
import app.oltnm.ui.screens.timelines.ManageTimelinesScreen
import app.oltnm.ui.screens.trending.TrendingScreen
import app.oltnm.ui.theme.OltnTheme

sealed interface Route {
    data object Home : Route
    data object Search : Route
    data object Compose : Route
    data object Notifications : Route
    data object Settings : Route
    data object ManageTimelines : Route
    data object Newsletter : Route
    data object Lists : Route
    data object Trending : Route
    data object Scheduled : Route
    data class Profile(val accountId: String) : Route
    data class StatusTimeline(val spec: TimelineSpec, val title: String) : Route
    data class StatusDetail(val statusId: String, val host: String? = null) : Route
}

private data class Tab(
    val route: Route,
    val icon: ImageVector,
    val label: Int
)

private val tabs = listOf(
    Tab(Route.Home, Icons.Rounded.Home, R.string.nav_home),
    Tab(Route.Search, Icons.Rounded.Search, R.string.nav_search),
    Tab(Route.Compose, Icons.Rounded.Add, R.string.nav_compose),
    Tab(Route.Notifications, Icons.Rounded.Notifications, R.string.nav_notifications)
)

@Composable
fun OltnApp(container: AppContainer) {
    val themeMode by container.preferences.themeMode.collectAsStateWithLifecycle()
    val accent by container.preferences.accent.collectAsStateWithLifecycle()
    val fontFamily by container.preferences.fontFamily.collectAsStateWithLifecycle()
    val textSize by container.preferences.textSize.collectAsStateWithLifecycle()
    val textColor by container.preferences.textColor.collectAsStateWithLifecycle()

    OltnTheme(
        themeMode = themeMode,
        accent = accent,
        fontFamily = fontFamily,
        textSize = textSize,
        textColor = textColor
    ) {
        val session by container.sessionStore.session.collectAsStateWithLifecycle()
        val current = session
        if (current == null) {
            LoginScreen(container = container)
        } else {
            MainScaffold(
                container = container,
                session = current,
                onLogout = { container.sessionStore.clear() }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    session: Session,
    onLogout: () -> Unit
) {
    val api = remember(session.accessToken, session.host) { container.apiFor(session) }
    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    val route = stack.last()
    var moreOpen by remember { mutableStateOf(false) }

    val select: (Route) -> Unit = { next ->
        stack.clear()
        stack.add(next)
    }
    val navigate: (Route) -> Unit = { next ->
        stack.add(next)
    }
    val openAccount: (String) -> Unit = { accountId ->
        stack.add(Route.Profile(accountId))
    }
    val openStatus: (String, String?) -> Unit = { statusId, host ->
        stack.add(Route.StatusDetail(statusId, host))
    }
    val back: () -> Unit = {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1, onBack = back)
    val navDividerColor = MaterialTheme.colorScheme.outlineVariant
    val detailRoute = route as? Route.StatusDetail
    val detailApi = remember(detailRoute?.statusId, detailRoute?.host) {
        val host = detailRoute?.host
        if (host.isNullOrBlank()) api else container.publicApiForHost(host)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 0.dp,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = navDividerColor,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
            ) {
                tabs.forEach { tab ->
                    val isCompose = tab.route == Route.Compose
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = { select(tab.route) },
                        icon = {
                            if (isCompose) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(tab.label),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = if (isCompose) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                NavigationBarItem(
                    selected = moreOpen,
                    onClick = { moreOpen = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.more),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val current = route) {
                Route.Home -> TimelineScreen(
                    container = container,
                    api = api,
                    session = session,
                    onOpenAccount = openAccount,
                    onOpenStatus = openStatus,
                    onManageTimelines = { navigate(Route.ManageTimelines) }
                )
                Route.Search -> SearchScreen(
                    api = api,
                    onOpenAccount = openAccount,
                    onOpenStatus = { id -> openStatus(id, null) }
                )
                Route.Compose -> ComposeScreen(
                    api = api,
                    giphyApi = container.giphyApi,
                    translateApi = container.translateApi,
                    draftStore = container.draftStore,
                    preferences = container.preferences,
                    onPosted = { select(Route.Home) }
                )
                Route.Notifications -> NotificationsScreen(
                    api = api,
                    onOpenAccount = openAccount,
                    onOpenStatus = { id -> openStatus(id, null) }
                )
                Route.Settings -> SettingsScreen(
                    container = container,
                    session = session,
                    onLogout = onLogout,
                    onBack = back
                )
                Route.ManageTimelines -> ManageTimelinesScreen(
                    container = container,
                    api = api,
                    onBack = back
                )
                Route.Newsletter -> NewsletterScreen(
                    container = container,
                    api = api,
                    onBack = back,
                    onOpenStatus = { id -> openStatus(id, null) }
                )
                Route.Lists -> ListsScreen(
                    container = container,
                    api = api,
                    onBack = back,
                    onOpenList = { list ->
                        navigate(
                            Route.StatusTimeline(
                                TimelineSpec(
                                    id = "list-${list.id}",
                                    type = TimelineTypes.LIST,
                                    value = list.id,
                                    label = list.title
                                ),
                                list.title
                            )
                        )
                    }
                )
                Route.Trending -> TrendingScreen(
                    api = api,
                    onBack = back,
                    onOpenStatus = { id -> openStatus(id, null) },
                    onOpenHashtag = { name ->
                        navigate(
                            Route.StatusTimeline(
                                TimelineSpec(
                                    id = "tag-$name",
                                    type = TimelineTypes.HASHTAG,
                                    value = name,
                                    label = "#$name"
                                ),
                                "#$name"
                            )
                        )
                    }
                )
                Route.Scheduled -> ScheduledScreen(
                    api = api,
                    onBack = back
                )
                is Route.Profile -> ProfileScreen(
                    api = api,
                    accountId = current.accountId,
                    onBack = back,
                    onOpenAccount = openAccount,
                    onOpenStatus = { id -> openStatus(id, null) }
                )
                is Route.StatusTimeline -> StatusTimelineScreen(
                    api = api,
                    spec = current.spec,
                    title = current.title,
                    onBack = back,
                    onOpenAccount = openAccount,
                    onOpenStatus = { id -> openStatus(id, null) }
                )
                is Route.StatusDetail -> StatusDetailScreen(
                    api = detailApi,
                    statusId = current.statusId,
                    interactive = current.host.isNullOrBlank(),
                    onBack = back,
                    onOpenAccount = openAccount,
                    onOpenStatus = { id -> openStatus(id, current.host) }
                )
            }
        }
    }

    if (moreOpen) {
        MoreMenuSheet(
            onDismiss = { moreOpen = false },
            onNavigate = { destination ->
                moreOpen = false
                navigate(destination)
            }
        )
    }
}
