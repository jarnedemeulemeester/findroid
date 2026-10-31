package dev.jdtech.jellyfin.presentation.settings.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.PermissionDefinitions
import dev.jdtech.jellyfin.settings.presentation.models.PreferencePermission
import dev.jdtech.jellyfin.utils.isPermissionGranted
import dev.jdtech.jellyfin.utils.openAppSettings
import dev.jdtech.jellyfin.utils.shouldShowPermissionRationale

@Composable
fun SettingsPermissionCard(
    preference: PreferencePermission,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(isPermissionGranted(context, preference.permission))
    }

    // Refresh the status when returning to the screen, e.g. after visiting the system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = isPermissionGranted(context, preference.permission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            isGranted = granted
            if (!granted) {
                val activity = context as? Activity
                if (
                    activity != null &&
                        !shouldShowPermissionRationale(activity, preference.permission)
                ) {
                    // The system dialog is suppressed ("don't ask again").
                    openAppSettings(context)
                }
            }
        }

    SettingsBaseCard(
        preference = preference,
        onClick = {
            if (!isGranted) {
                launcher.launch(preference.permission.id)
            }
        },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(preference.iconDrawableId),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(preference.nameStringResource),
                    style = MaterialTheme.typography.titleMedium,
                )
                preference.descriptionStringRes?.let {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                    Text(
                        text = stringResource(id = it),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
            Icon(
                painter =
                    painterResource(
                        if (isGranted) {
                            CoreR.drawable.ic_check
                        } else {
                            CoreR.drawable.ic_x
                        }
                    ),
                contentDescription =
                    stringResource(
                        if (isGranted) {
                            SettingsR.string.permission_status_granted
                        } else {
                            SettingsR.string.permission_status_denied
                        }
                    ),
                tint =
                    if (isGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
    }
}

@Preview
@Composable
private fun SettingsPermissionCardPreview() {
    FindroidTheme {
        SettingsPermissionCard(
            preference = PreferencePermission(permission = PermissionDefinitions.localNetwork)
        )
    }
}
