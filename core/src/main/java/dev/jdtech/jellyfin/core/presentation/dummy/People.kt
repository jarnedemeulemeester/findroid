package dev.jdtech.jellyfin.core.presentation.dummy

import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.models.FindroidPersonImage
import org.jellyfin.sdk.model.api.PersonKind
import java.util.UUID

val dummyPerson = FindroidPerson(
    id = UUID.randomUUID(),
    name = "Su Shangqing",
    type = PersonKind.ACTOR,
    role = "Cheng Xiaoshi (voice)",
    image = FindroidPersonImage(
        uri = null,
        blurHash = null,
    ),
)
