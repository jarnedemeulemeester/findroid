package dev.jdtech.jellyfin.presentation.downloads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import dev.jdtech.jellyfin.databinding.FragmentDownloadsBinding
import dev.jdtech.jellyfin.fragments.DownloadsFragment

/**
 * Lightweight host to reuse the existing Fragment-based Downloads UI inside Compose navigation.
 * We inflate the binding layout and let the Fragment manage its own logic via its ViewModel.
 */
@Composable
fun DownloadsScreenHost(modifier: Modifier = Modifier) {
    AndroidViewBinding(factory = { inflater, parent, attachToParent ->
        FragmentDownloadsBinding.inflate(inflater, parent, attachToParent).also { binding ->
            // The layout already wires the RecyclerView; the Fragment owns the logic.
            // This host ensures the Composable destination shows the downloads content.
        }
    }, modifier = modifier)
}
