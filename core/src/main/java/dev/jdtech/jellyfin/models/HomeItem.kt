package dev.jdtech.jellyfin.models

import java.util.UUID

sealed class HomeItem {
    data class Libraries(val section: HomeSection) : HomeItem() {
        override val id = section.id
    }

    data class Section(val homeSection: HomeSection) : HomeItem() {
        override val id = homeSection.id
    }

    data class ViewItem(val view: View) : HomeItem() {
        override val id = view.id
    }

    abstract val id: UUID
}
