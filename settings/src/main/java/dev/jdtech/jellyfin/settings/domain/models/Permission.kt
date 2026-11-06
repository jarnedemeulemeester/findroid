package dev.jdtech.jellyfin.settings.domain.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class Permission(
    val id: String,
    val minSdk: Int,
    @param:StringRes val nameStringResource: Int,
    @param:StringRes val descriptionStringRes: Int? = null,
    @param:DrawableRes val iconDrawableId: Int,
    @param:StringRes val rationaleStringRes: Int,
)
