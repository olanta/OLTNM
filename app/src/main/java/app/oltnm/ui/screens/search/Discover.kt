package app.oltnm.ui.screens.search

import androidx.annotation.StringRes
import app.oltnm.R
import app.oltnm.data.model.Account
import app.oltnm.util.HtmlText
import java.time.Instant
import kotlin.math.log10

data class DiscoverCategory(
    val id: String,
    @StringRes val label: Int,
    val query: String,
    val tags: List<String>,
    val keywords: List<String>
)

val DiscoverCategories = listOf(
    DiscoverCategory(
        id = "art",
        label = R.string.discover_art,
        query = "artist art illustration design",
        tags = listOf("art", "illustration", "design"),
        keywords = listOf("art", "artist", "illustration", "design", "painting", "drawing")
    ),
    DiscoverCategory(
        id = "books",
        label = R.string.discover_books,
        query = "books reading writer poetry",
        tags = listOf("books", "reading", "writing"),
        keywords = listOf("book", "books", "reading", "writer", "writing", "poetry", "author")
    ),
    DiscoverCategory(
        id = "cinema",
        label = R.string.discover_cinema,
        query = "film cinema movies television",
        tags = listOf("film", "cinema", "movies"),
        keywords = listOf("film", "cinema", "movie", "movies", "tv", "television")
    ),
    DiscoverCategory(
        id = "music",
        label = R.string.discover_music,
        query = "music musician band producer",
        tags = listOf("music", "musician", "band"),
        keywords = listOf("music", "musician", "band", "song", "album", "producer")
    ),
    DiscoverCategory(
        id = "tech",
        label = R.string.discover_tech,
        query = "technology programming opensource software",
        tags = listOf("tech", "opensource", "programming"),
        keywords = listOf(
            "tech",
            "technology",
            "programming",
            "developer",
            "software",
            "opensource",
            "linux"
        )
    ),
    DiscoverCategory(
        id = "science",
        label = R.string.discover_science,
        query = "science research physics astronomy",
        tags = listOf("science", "research", "astronomy"),
        keywords = listOf(
            "science",
            "scientist",
            "research",
            "physics",
            "biology",
            "astronomy",
            "space"
        )
    ),
    DiscoverCategory(
        id = "news",
        label = R.string.discover_news,
        query = "news journalism politics",
        tags = listOf("news", "politics", "journalism"),
        keywords = listOf("news", "journalism", "journalist", "politics", "policy", "world")
    ),
    DiscoverCategory(
        id = "nature",
        label = R.string.discover_nature,
        query = "nature environment climate wildlife",
        tags = listOf("nature", "climate", "environment"),
        keywords = listOf(
            "nature",
            "environment",
            "climate",
            "sustainability",
            "wildlife",
            "ecology"
        )
    ),
    DiscoverCategory(
        id = "photography",
        label = R.string.discover_photography,
        query = "photography photographer camera",
        tags = listOf("photography", "photographer", "photo"),
        keywords = listOf("photography", "photographer", "photo", "camera", "landscape")
    ),
    DiscoverCategory(
        id = "gaming",
        label = R.string.discover_gaming,
        query = "gaming videogames indie games",
        tags = listOf("gaming", "videogames", "indiegames"),
        keywords = listOf("game", "games", "gaming", "videogames", "indie", "developer")
    ),
    DiscoverCategory(
        id = "health",
        label = R.string.discover_health,
        query = "health wellbeing mentalhealth fitness",
        tags = listOf("health", "mentalhealth", "wellbeing"),
        keywords = listOf("health", "wellbeing", "mentalhealth", "fitness", "medicine", "care")
    ),
    DiscoverCategory(
        id = "travel",
        label = R.string.discover_travel,
        query = "travel hiking outdoors adventure",
        tags = listOf("travel", "hiking", "outdoors"),
        keywords = listOf("travel", "traveller", "traveler", "hiking", "outdoors", "adventure")
    ),
    DiscoverCategory(
        id = "communities",
        label = R.string.discover_communities,
        query = "lgbtq queer accessibility community",
        tags = listOf("lgbtq", "queer", "accessibility"),
        keywords = listOf(
            "lgbtq",
            "queer",
            "trans",
            "accessibility",
            "neurodiversity",
            "community"
        )
    )
)

data class DiscoverCandidate(
    val account: Account,
    var tagHits: Int = 0,
    var searchHits: Int = 0,
    var lastSeen: Long = 0
)

fun scoreDiscoverCandidate(
    candidate: DiscoverCandidate,
    category: DiscoverCategory,
    now: Long = System.currentTimeMillis()
): Double {
    val account = candidate.account
    val text = "${account.displayName} ${account.username} ${HtmlText.plain(account.note)}"
        .lowercase()
    val tokens = text.split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .toSet()
    val keywordHits = category.keywords.count { it in tokens }
    val recency = if (candidate.lastSeen > 0) {
        (1.0 - (now - candidate.lastSeen).toDouble() / 30.0 / 24.0 / 60.0 / 60.0 / 1000.0)
            .coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    return candidate.searchHits * 25.0 +
        minOf(candidate.tagHits, 6) * 12.0 +
        minOf(keywordHits, 6) * 10.0 +
        log10(account.followersCount + 1.0) * 4.0 +
        log10(account.statusesCount + 1.0) * 2.0 +
        recency * 8.0
}

fun rankDiscoverCandidates(
    candidates: Collection<DiscoverCandidate>,
    category: DiscoverCategory,
    now: Long = System.currentTimeMillis(),
    limit: Int = 15
): List<Account> = candidates
    .sortedWith(
        compareByDescending<DiscoverCandidate> { scoreDiscoverCandidate(it, category, now) }
            .thenByDescending { it.account.followersCount }
            .thenBy { it.account.acct }
    )
    .take(limit)
    .map { it.account }

fun statusRecency(createdAt: String): Long = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0)
