package dev.jdtech.jellyfin.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import dev.jdtech.jellyfin.BuildConfig
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AboutScreen(
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val safePaddingStart = with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }

    val paddingStart = safePaddingStart
    val paddingEnd = safePaddingEnd

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val libraries by rememberLibraries(R.raw.aboutlibraries)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(SettingsR.string.about))
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingStart + innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = paddingEnd + innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding(),
            ),
            header = {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = MaterialTheme.spacings.default),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(MaterialTheme.spacings.small))
                            Image(
                                painter = painterResource(CoreR.drawable.ic_banner),
                                contentDescription = null,
                                modifier = Modifier.width(240.dp),
                            )
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            Text(
                                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(MaterialTheme.spacings.small))
                            Text(
                                text = stringResource(CoreR.string.app_description),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            HorizontalDivider()
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        try {
                                            uriHandler.openUri("https://github.com/jarnedemeulemeester/findroid")
                                        } catch (e: IllegalArgumentException) {
                                            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_github),
                                        contentDescription = null,
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = {
                                        try {
                                            uriHandler.openUri("https://ko-fi.com/jarnedemeulemeester")
                                        } catch (e: IllegalArgumentException) {
                                            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_coffee),
                                        contentDescription = null,
                                    )
                                }
                            }
                            Spacer(Modifier.height(MaterialTheme.spacings.small))
                        }
                    }
                }
            },
        )
    }
}

@Composable
@PreviewScreenSizes
private fun AboutScreenPreview() {
    FindroidTheme {
        AboutScreen(
            navigateBack = {},
        )
    }
}
