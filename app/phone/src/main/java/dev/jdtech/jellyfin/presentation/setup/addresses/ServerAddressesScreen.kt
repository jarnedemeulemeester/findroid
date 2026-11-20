package dev.jdtech.jellyfin.presentation.setup.addresses

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServerAddress
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.setup.R as SetupR
import dev.jdtech.jellyfin.setup.presentation.addresses.ServerAddressesAction
import dev.jdtech.jellyfin.setup.presentation.addresses.ServerAddressesState
import dev.jdtech.jellyfin.setup.presentation.addresses.ServerAddressesViewModel

@Composable
fun ServerAddressesScreen(
    serverId: String,
    navigateBack: () -> Unit,
    viewModel: ServerAddressesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadAddresses(serverId) }

    ServerAddressesLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is ServerAddressesAction.OnBackClick -> navigateBack()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerAddressesLayout(state: ServerAddressesState, onAction: (ServerAddressesAction) -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingTop = MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var selectedAddress by remember { mutableStateOf<ServerAddress?>(null) }
    var openAddDialog by remember { mutableStateOf(false) }
    var openDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(SetupR.string.addresses)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(ServerAddressesAction.OnBackClick) }) {
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(SetupR.string.add_address)) },
                icon = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_plus),
                        contentDescription = null,
                    )
                },
                onClick = { openAddDialog = true },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = paddingStart + innerPadding.calculateStartPadding(layoutDirection),
                        top = paddingTop,
                        end = paddingEnd + innerPadding.calculateEndPadding(layoutDirection),
                        bottom = paddingBottom + innerPadding.calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                item(key = "auto_select_fastest") {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(CardDefaults.outlinedShape)
                                .padding(MaterialTheme.spacings.small)
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth().padding(MaterialTheme.spacings.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text =
                                        stringResource(
                                            SetupR.string.auto_select_fastest_address
                                        ),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text =
                                        stringResource(
                                            SetupR.string.auto_select_fastest_address_summary
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.autoSelectFastest,
                                onCheckedChange = { enabled ->
                                    onAction(
                                        ServerAddressesAction.SetAutoSelectFastest(enabled)
                                    )
                                },
                            )
                        }
                    }
                }
                items(items = state.addresses, key = { it.id }) { address ->
                    val isCurrent = address.id == state.currentAddressId
                    val latency = state.latencies[address.id]
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(CardDefaults.outlinedShape)
                                .combinedClickable(
                                    onClick = {
                                        onAction(ServerAddressesAction.OnServerClick(address.id))
                                    },
                                    onLongClick = {
                                        selectedAddress = address
                                        openDeleteDialog = true
                                    },
                                )
                                .padding(MaterialTheme.spacings.small)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(address.address)
                                Text(
                                    text =
                                        when {
                                            latency != null ->
                                                stringResource(
                                                    SetupR.string.address_latency_ms,
                                                    latency,
                                                )
                                            state.isMeasuring ->
                                                stringResource(
                                                    SetupR.string.address_latency_measuring
                                                )
                                            else ->
                                                stringResource(SetupR.string.address_latency_timeout)
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_check),
                                    contentDescription =
                                        stringResource(SetupR.string.address_current),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (openAddDialog) {
        AddServerAddressDialog(
            onAdd = { address ->
                onAction(ServerAddressesAction.AddAddress(address))
                openAddDialog = false
            },
            onDismiss = { openAddDialog = false },
        )
    }

    if (openDeleteDialog && selectedAddress != null) {
        DeleteServerAddressDialog(
            address = selectedAddress!!.address,
            onConfirm = {
                onAction(ServerAddressesAction.DeleteAddress(selectedAddress!!.id))
                openDeleteDialog = false
            },
            onDismiss = { openDeleteDialog = false },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun ServerAddressesLayoutPreview() {
    FindroidTheme {
        ServerAddressesLayout(
            state = ServerAddressesState(addresses = listOf(dummyServerAddress)),
            onAction = {},
        )
    }
}
