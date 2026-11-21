package app.oltnm.ui.screens.compose

import app.oltnm.data.model.Status
import app.oltnm.data.model.Visibility

data class ReplyTarget(
    val statusId: String,
    val acct: String,
    val visibility: Visibility,
    val lookupUrl: String? = null,
    val mentions: List<String> = emptyList(),
    val spoiler: String = "",
    val spoilerEnabled: Boolean = false,
    val includeAuthor: Boolean = true
) {
    val initialText: String
        get() {
            val handles = ((if (includeAuthor) listOf(acct) else emptyList()) + mentions)
                .map { it.trim().removePrefix("@") }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
            return if (handles.isEmpty()) "" else handles.joinToString(" ") { "@$it" } + " "
        }

    companion object {
        fun from(
            status: Status,
            currentAccountId: String? = null,
            resolve: Boolean = false
        ): ReplyTarget {
            val original = status.original
            val handles = buildList {
                if (original.account.id != currentAccountId) {
                    add(original.account.acct)
                }
                original.mentions.forEach { mention ->
                    if (mention.id != currentAccountId && mention.acct.isNotBlank()) {
                        add(mention.acct)
                    }
                }
            }
            return ReplyTarget(
                statusId = original.id,
                acct = original.account.acct,
                visibility = Visibility.fromApi(original.visibility),
                lookupUrl = if (resolve) original.displayUrl else null,
                mentions = handles,
                spoiler = original.spoilerText,
                spoilerEnabled = original.spoilerText.isNotBlank(),
                includeAuthor = original.account.id != currentAccountId
            )
        }
    }
}
