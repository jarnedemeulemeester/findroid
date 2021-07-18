package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.View
import org.jellyfin.sdk.model.api.BaseItemDto

fun BaseItemDto.toView(): View {
    return View(
        id = id,
        name = name
    )
}