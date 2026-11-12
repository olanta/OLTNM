package app.oltnm.ui.screens.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.di.AppContainer
import app.oltnm.ui.components.OltnGhostButton
import app.oltnm.ui.components.OltnGlassCard
import app.oltnm.ui.components.OltnMark
import app.oltnm.ui.components.OltnPrimaryButton
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf
import app.oltnm.ui.theme.OltnPalette
import app.oltnm.util.OAuthBus
import coil3.compose.AsyncImage

@Composable
fun LoginScreen(
    container: AppContainer,
    viewModelKey: String = "login",
    onCancel: (() -> Unit)? = null,
    onSignedIn: (() -> Unit)? = null
) {
    val viewModel: LoginViewModel = viewModel(
        key = viewModelKey,
        factory = factoryOf { LoginViewModel(container.authRepository) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val code by OAuthBus.code.collectAsStateWithLifecycle()
    val oauthError by OAuthBus.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openUrl = rememberOpenUrl()

    LaunchedEffect(viewModelKey) {
        if (container.sessionStore.pending() == null) {
            OAuthBus.consumeCode()
            OAuthBus.consumeError()
        }
    }

    LaunchedEffect(state.authUrl) {
        val url = state.authUrl ?: return@LaunchedEffect
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            viewModel.showError(LoginError.Generic)
        }
        viewModel.consumeAuthUrl()
    }

    LaunchedEffect(code) {
        val value = code ?: return@LaunchedEffect
        viewModel.completeLogin(value)
        OAuthBus.consumeCode()
    }

    LaunchedEffect(oauthError) {
        val value = oauthError ?: return@LaunchedEffect
        viewModel.showError(LoginError.Auth)
        OAuthBus.consumeError()
    }

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            viewModel.consumeSignedIn()
            onSignedIn?.invoke()
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val errorText = loginErrorText(state.error)
    val discovered = state.discovered

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(OltnPalette.BgArena, OltnPalette.Bg)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (onCancel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.back))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent)
                            )
                        )
                )
                OltnMark(size = 104.dp)
            }

            Text(
                text = "OLTN",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
            )

            OltnGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(28.dp)
            ) {
                if (discovered == null) {
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.instance_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OltnTextField(
                        value = state.instanceInput,
                        onValueChange = viewModel::onInstanceChange,
                        placeholder = stringResource(R.string.instance_hint),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { viewModel.discover() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorText?.let { message ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    OltnPrimaryButton(
                        text = if (state.busy) {
                            stringResource(R.string.login_looking)
                        } else {
                            stringResource(R.string.login_button)
                        },
                        onClick = viewModel::discover,
                        enabled = state.instanceInput.isNotBlank(),
                        loading = state.busy,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.login_mastodon_intro),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { openUrl("https://joinmastodon.org") }) {
                            Text(
                                text = "joinmastodon.org",
                                style = MaterialTheme.typography.labelSmall,
                                color = primary
                            )
                        }
                    }
                } else {
                    if (!discovered.thumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = discovered.thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            OltnMark(size = 40.dp, animated = false)
                        }
                    }

                    Text(
                        text = discovered.title.ifBlank { discovered.uri },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                    Text(
                        text = discovered.uri.removePrefix("https://").removePrefix("http://"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    discovered.version?.let { version ->
                        Text(
                            text = "Mastodon $version",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }

                    errorText?.let { message ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OltnGhostButton(
                            text = stringResource(R.string.login_back),
                            onClick = viewModel::reselectInstance,
                            modifier = Modifier.weight(1f)
                        )
                        OltnPrimaryButton(
                            text = if (state.busy) {
                                stringResource(R.string.login_looking)
                            } else {
                                stringResource(R.string.login_button)
                            },
                            onClick = viewModel::beginLogin,
                            loading = state.busy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.login_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun loginErrorText(error: LoginError?): String? = when (error) {
    LoginError.NotFound -> stringResource(R.string.login_instance_not_found)
    LoginError.InstanceDown -> stringResource(R.string.login_instance_down)
    LoginError.Auth -> stringResource(R.string.login_error_auth)
    LoginError.Generic -> stringResource(R.string.error_generic)
    null -> null
}
