package app.oltnm.ui.screens.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.oltnm.BuildConfig
import app.oltnm.R
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.util.CrashReporter
import app.oltnm.util.OltnLog

@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.diagnostics_share)
    var logs by remember { mutableStateOf("") }
    var crashes by remember { mutableStateOf("") }

    fun refresh() {
        logs = OltnLog.dump()
        crashes = CrashReporter.readAll(context)
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.diagnostics_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = ::refresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh)
                    )
                }
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OltnSectionTitle(stringResource(R.string.diagnostics_logs))
            }
            item {
                SelectionContainer {
                    Text(
                        text = logs.ifBlank { stringResource(R.string.diagnostics_empty) },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
            item {
                OltnSectionTitle(stringResource(R.string.diagnostics_crashes))
            }
            item {
                SelectionContainer {
                    Text(
                        text = crashes.ifBlank { stringResource(R.string.diagnostics_no_crashes) },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val report = buildString {
                                appendLine("OLTN Mobile diagnostics")
                                appendLine(
                                    "version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                                )
                                appendLine("time=${System.currentTimeMillis()}")
                                appendLine()
                                appendLine("== Logs ==")
                                appendLine(logs)
                                appendLine("== Crashes ==")
                                appendLine(crashes)
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    shareTitle
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.diagnostics_share))
                    }
                    OutlinedButton(
                        onClick = {
                            OltnLog.clear()
                            CrashReporter.clear(context)
                            refresh()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.diagnostics_clear))
                    }
                }
            }
        }
    }
}
