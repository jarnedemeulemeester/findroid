package dev.jdtech.jellyfin.offline.download

data class OfflineTransferRequest(val url: String, val headers: Map<String, String> = emptyMap())
