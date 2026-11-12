package app.oltnm.ui.screens.compose

import app.oltnm.data.model.Status
import app.oltnm.data.model.Visibility

data class ReplyTarget(
    val statusId: String,
    val acct: String,
    val visibility: Visibility
) {
    val initialText: String
        get() {
            val handle = acct.trim().removePrefix("@")
            return if (handle.isBlank()) "" else "@$handle "
        }

    companion object {
        fun from(status: Status): ReplyTarget {
            val original = status.original
            return ReplyTarget(
                statusId = original.id,
                acct = original.account.acct,
                visibility = Visibility.fromApi(original.visibility)
            )
        }
    }
}
