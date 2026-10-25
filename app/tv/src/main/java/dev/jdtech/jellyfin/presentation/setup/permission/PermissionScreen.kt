package dev.jdtech.jellyfin.presentation.setup.permission

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.setup.R as SetupR
import dev.jdtech.jellyfin.setup.presentation.permission.PermissionAction
import dev.jdtech.jellyfin.setup.presentation.permission.PermissionViewModel
import dev.jdtech.jellyfin.utils.openAppSettings
import dev.jdtech.jellyfin.utils.shouldShowPermissionRationale

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel,
    onDecided: () -> Unit,
    onBackPressed: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check when returning to the screen, e.g. after visiting the system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(PermissionAction.Check)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Only set for the upgrader interstitial: back dismisses it for the session without
    // recording a decision.
    onBackPressed?.let { BackHandler(onBack = it) }

    // Advance when the queue is exhausted, or nothing applies on this OS version.
    LaunchedEffect(state.currentRequest) {
        if (state.currentRequest == null) {
            onDecided()
        }
    }

    val request = state.currentRequest ?: return

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.onAction(PermissionAction.OnPermissionResult(granted = true))
            } else {
                val activity = context as? Activity
                if (
                    activity != null && !shouldShowPermissionRationale(activity, request.permission)
                ) {
                    // The system dialog is suppressed ("don't ask again").
                    viewModel.onAction(PermissionAction.OnPermanentlyDenied)
                }
            }
        }

    PermissionScreenLayout(
        title = request.permission.nameStringResource,
        rationale = request.permission.rationaleStringRes,
        icon = request.permission.iconDrawableId,
        permanentlyDenied = state.permanentlyDenied,
        onGrantClick = { launcher.launch(request.permission.id) },
        onOpenSettingsClick = { openAppSettings(context) },
        onSkipClick = { viewModel.onAction(PermissionAction.OnSkipClick) },
    )
}

@Composable
private fun PermissionScreenLayout(
    @StringRes title: Int,
    @StringRes rationale: Int,
    @DrawableRes icon: Int,
    permanentlyDenied: Boolean,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).widthIn(max = 640.dp),
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
            Text(
                text = stringResource(rationale),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (permanentlyDenied) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
                Text(
                    text =
                        stringResource(
                            SetupR.string.permission_permanently_denied,
                            stringResource(title),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            OutlinedButton(onClick = onSkipClick) {
                Text(text = stringResource(SetupR.string.permission_btn_not_now))
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            Button(
                onClick = if (permanentlyDenied) onOpenSettingsClick else onGrantClick,
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                Text(
                    text =
                        stringResource(
                            if (permanentlyDenied) {
                                SetupR.string.permission_btn_open_settings
                            } else {
                                SetupR.string.permission_btn_grant
                            }
                        )
                )
            }
        }
    }

    LaunchedEffect(true) { focusRequester.requestFocus() }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun PermissionScreenLayoutPreview() {
    FindroidTheme {
        PermissionScreenLayout(
            title = SettingsR.string.permission_local_network,
            rationale = SettingsR.string.permission_local_network_rationale,
            icon = SettingsR.drawable.ic_network,
            permanentlyDenied = false,
            onGrantClick = {},
            onOpenSettingsClick = {},
            onSkipClick = {},
        )
    }
}
