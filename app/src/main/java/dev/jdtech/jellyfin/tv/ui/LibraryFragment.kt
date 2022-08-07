package dev.jdtech.jellyfin.tv.ui

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel

@AndroidEntryPoint
class LibraryFragment : BrowseSupportFragment() {
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        headersState = HEADERS_DISABLED
    }
}