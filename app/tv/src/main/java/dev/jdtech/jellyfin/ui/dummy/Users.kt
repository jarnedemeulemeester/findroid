package dev.jdtech.jellyfin.ui.dummy

import dev.jdtech.jellyfin.models.User
import java.util.UUID

val dummyUser = User(
    id = UUID.randomUUID(),
    name = "Username",
    serverId = ""
)

val dummyUsers = listOf(dummyUser)