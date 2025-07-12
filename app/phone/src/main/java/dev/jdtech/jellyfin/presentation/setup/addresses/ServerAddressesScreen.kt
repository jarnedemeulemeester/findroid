package dev.jdtech.jellyfin.presentation.setup.addresses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServerAddress
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.setup.presentation.addresses.ServerAddressesAction
import dev.jdtech.jellyfin.setup.presentation.addresses.ServerAddressesState
import dev.jdtech.jellyfin.setup.presentation.addresses.ServerAddressesViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ServerAddressesScreen(
    serverId: String,
    navigateBack: () -> Unit,
    viewModel: ServerAddressesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadAddresses(
            serverId,
        )
    }

    ServerAddressesLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is ServerAddressesAction.OnBackClick -> navigateBack()
                else -> Unit
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerAddressesLayout(
    state: ServerAddressesState,
    onAction: (ServerAddressesAction) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingTop = MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(CoreR.string.addresses))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(ServerAddressesAction.OnBackClick)
                        },
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
        Column(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = paddingStart + innerPadding.calculateStartPadding(layoutDirection),
                    top = paddingTop,
                    end = paddingEnd + innerPadding.calculateEndPadding(layoutDirection),
                    bottom = paddingBottom + innerPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            ) {
                items(
                    items = state.addresses,
                    key = { it.id },
                ) { address ->
                    Text(address.address)
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun ServerAddressesLayoutPreview() {
    FindroidTheme {
        ServerAddressesLayout(
            state = ServerAddressesState(
                addresses = listOf(dummyServerAddress),
            ),
            onAction = {},
        )
    }
}
