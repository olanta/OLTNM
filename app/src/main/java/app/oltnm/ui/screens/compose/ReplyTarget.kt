package app.oltnm.ui.screens.compose

import app.oltnm.data.model.Status
import app.oltnm.data.model.Visibility

data class ReplyTarget(
    val statusId: String,
    val acct: String,
    val visibility: Visibility,
    val lookupUrl: String? = null
) {
    val initialText: String
        get() {
            val handle = acct.trim().removePrefix("@")
            return if (handle.isBlank()) "" else "@$handle "
        }

    companion object {
        fun from(status: Status, resolve: Boolean = false): ReplyTarget {
            val original = status.original
            return ReplyTarget(
                statusId = original.id,
                acct = original.account.acct,
                visibility = Visibility.fromApi(original.visibility),
                lookupUrl = if (resolve) original.displayUrl else null
            )
        }
    }
}
