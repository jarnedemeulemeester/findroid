package dev.jdtech.jellyfin.player.cast.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.player.core.R
import dev.jdtech.jellyfin.player.core.domain.PlaylistManager
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.PlayerMediaType
import dev.jdtech.jellyfin.player.core.domain.models.Trickplay
import dev.jdtech.jellyfin.player.core.domain.utils.ChapterUtils
import dev.jdtech.jellyfin.player.core.domain.utils.SegmentUtils
import dev.jdtech.jellyfin.player.core.domain.utils.SegmentUtils.getSkipButtonTextStringId
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.Boolean
import kotlin.Float
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CastPlayerViewModel
@Inject constructor(
    val castManager: CastManager,
    private val playlistManager: PlaylistManager,
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    data class CurrentItemTitle(
        val seriesName: String? = null,
        val episodeInfo: String? = null,
        val title: String,
    )

    data class UiState(
        val currentItemTitle: CurrentItemTitle,
        val currentItemPosterUrl: String?,
        val isMovie: Boolean,
        val defaultAspectRatio: Float,
        val trickplayAspectRatio: Float?,
        val currentSegment: FindroidSegment?,
        val currentSkipButtonStringRes: Int,
        val currentTrickplay: Trickplay?,
        val currentChapters: List<PlayerChapter>,
    )

    private val _uiState =
        MutableStateFlow(
            UiState(
                currentItemTitle = CurrentItemTitle(title = ""),
                currentItemPosterUrl = null,
                isMovie = false,
                defaultAspectRatio = 16f / 10f,
                trickplayAspectRatio = null,
                currentSegment = null,
                currentSkipButtonStringRes = R.string.player_controls_skip_intro,
                currentTrickplay = null,
                currentChapters = emptyList(),
            )
        )
    val uiState = _uiState.asStateFlow()

    private var currentMediaItemSegments: List<FindroidSegment> = emptyList()
    private var hasNextMediaItem = false

    init {
        viewModelScope.launch {
            castManager.currentItem.collect { item ->
                if (item != null) {
                    onMediaItemTransition(item)
                }
            }
        }

        if (
            appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButton) ||
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        ) {
            viewModelScope.launch {
                while (true) {
                    castManager.playbackState.collect { state ->
                        updateCurrentSegment(state.currentPosition)
                    }
                    delay(1000L.milliseconds)
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                updatePlaybackProgress()
                delay(5000L.milliseconds)
            }
        }
    }

    fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {
        viewModelScope.launch {
            val initialItem = playlistManager.getInitialItem(
                itemId = itemId,
                itemKind = BaseItemKind.fromName(itemKind),
                mediaSourceIndex = null,
                startFromBeginning = startFromBeginning
            )
            if (initialItem != null) {
                castManager.loadItem(initialItem, if (startFromBeginning) 0L else initialItem.playbackPosition)
            }
        }
    }

    private suspend fun onMediaItemTransition(item: PlayerItem) {
        Timber.d("Cast MediaItem transition: ${item.itemId}")
        val isMovie = item.mediaType == PlayerMediaType.MOVIE

        val defaultRatio = when (item.mediaType) {
            PlayerMediaType.EPISODE -> 16f / 9f
            PlayerMediaType.MOVIE -> 2f / 3f
            else -> 16f / 10f
        }

        val trickplayRatio = item.trickplayInfo?.let {
            if (it.width > 0 && it.height > 0) it.width.toFloat() / it.height.toFloat() else null
        }

        val itemTitle = if (item.parentIndexNumber != null && item.indexNumber != null) {
            val parentIndex = item.parentIndexNumber.toString().padStart(2, '0')
            val index = item.indexNumber.toString().padStart(2, '0')
            val episodeInfo = if (item.indexNumberEnd == null) {
                "S$parentIndex:E$index"
            } else {
                val indexEnd = item.indexNumberEnd.toString().padStart(2, '0')
                "S$parentIndex:E$index-$indexEnd"
            }

            CurrentItemTitle(
                seriesName = item.seriesName,
                episodeInfo = episodeInfo,
                title = item.name
            )
        } else {
            CurrentItemTitle(title = item.name)
        }

        _uiState.update {
            it.copy(
                currentItemTitle = itemTitle,
                currentItemPosterUrl = item.posterUrl,
                isMovie = isMovie,
                defaultAspectRatio = defaultRatio,
                trickplayAspectRatio = trickplayRatio,
                currentSegment = null,
                currentChapters = item.chapters,
            )
        }

        repository.postPlaybackStart(item.itemId)

        if (appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButton) ||
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        ) {
            getSegments(item.itemId)
        }

        if (appPreferences.getValue(appPreferences.playerTrickplay)) {
            getTrickplay(item)
        }

        playlistManager.setCurrentMediaItemIndex(item.itemId)
        hasNextMediaItem = playlistManager.getNextPlayerItem() != null
    }

    private suspend fun updatePlaybackProgress() {
        val item = castManager.currentItem.value ?: return
        val state = castManager.playbackState.value
        if (state.duration > 0) {
            try {
                repository.postPlaybackProgress(
                    item.itemId,
                    state.currentPosition.times(10000),
                    !state.isPlaying,
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun updateCurrentSegment(positionMs: Long) {
        if (currentMediaItemSegments.isEmpty()) return

        val currentSegment = currentMediaItemSegments.find { segment ->
            positionMs in segment.startTicks..<(segment.endTicks - 100L)
        }

        if (currentSegment == null) {
            if (_uiState.value.currentSegment != null) {
                _uiState.update { it.copy(currentSegment = null) }
            }
            return
        }

        val segmentsAutoSkip = appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        val segmentsAutoSkipTypes = appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkipType)
        val segmentsAutoSkipMode = appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkipMode)
        val segmentsSkipButtonTypes = appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButtonType)

        if (segmentsAutoSkip &&
            segmentsAutoSkipTypes.contains(currentSegment.type.toString()) &&
            segmentsAutoSkipMode == Constants.PlayerMediaSegmentsAutoSkip.ALWAYS
        ) {
            skipSegment(currentSegment)
        } else if (segmentsSkipButtonTypes.contains(currentSegment.type.toString())) {
            _uiState.update {
                it.copy(
                    currentSegment = currentSegment,
                    currentSkipButtonStringRes = getSkipButtonTextStringId(currentSegment, shouldSkipToNextEpisode(currentSegment)),
                )
            }
        } else {
            _uiState.update { it.copy(currentSegment = null) }
        }

        Timber.d("Updated current segment: ${uiState.value.currentSegment?.type}")
    }

    fun skipSegment(segment: FindroidSegment) {
        if (shouldSkipToNextEpisode(segment)) {
            // Handle next episode logic if needed
        } else {
            castManager.seekTo(segment.endTicks)
        }
        _uiState.update { it.copy(currentSegment = null) }
    }

    private fun shouldSkipToNextEpisode(segment: FindroidSegment): Boolean {
        return SegmentUtils.shouldSkipToNextEpisode(
            segment = segment,
            hasNextMediaItem = hasNextMediaItem,
            playerDurationMillis = castManager.playbackState.value.duration,
            nextEpisodeThreshold = appPreferences.getValue(appPreferences.playerMediaSegmentsNextEpisodeThreshold)
        )
    }

    private suspend fun getSegments(itemId: UUID) {
        try {
            currentMediaItemSegments = repository.getSegments(itemId)
        } catch (e: Exception) {
            currentMediaItemSegments = emptyList()
            Timber.e(e)
        }
    }

    private suspend fun getTrickplay(item: PlayerItem) {
        val trickplayInfo = item.trickplayInfo ?: return
        withContext(Dispatchers.Default) {
            try {
                val maxIndex = ceil(trickplayInfo.thumbnailCount.toDouble().div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)).toInt()
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0..maxIndex) {
                    repository.getTrickplayData(item.itemId, trickplayInfo.width, i)?.let { byteArray ->
                        val fullBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        for (offsetY in 0..<trickplayInfo.height * trickplayInfo.tileHeight step trickplayInfo.height) {
                            for (offsetX in 0..<trickplayInfo.width * trickplayInfo.tileWidth step trickplayInfo.width) {
                                if (bitmaps.size < trickplayInfo.thumbnailCount) {
                                    val bitmap = Bitmap.createBitmap(fullBitmap, offsetX, offsetY, trickplayInfo.width, trickplayInfo.height)
                                    bitmaps.add(bitmap)
                                }
                            }
                        }
                    }
                }
                _uiState.update {
                    it.copy(currentTrickplay = Trickplay(trickplayInfo.interval, bitmaps))
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun getCurrentChapterIndex(chapters: List<PlayerChapter>, currentPosition: Long): Int? {
        return ChapterUtils.getCurrentChapterIndex(chapters, currentPosition)
    }

    fun getNextChapterIndex(chapters: List<PlayerChapter>, currentPosition: Long): Int? {
        return ChapterUtils.getNextChapterIndex(chapters, currentPosition)
    }

    fun getPreviousChapterIndex(chapters: List<PlayerChapter>, currentPosition: Long): Int? {
        return ChapterUtils.getPreviousChapterIndex(chapters, currentPosition)
    }

    fun isLastChapter(chapters: List<PlayerChapter>, currentPosition: Long): Boolean {
        return ChapterUtils.isLastChapter(chapters, currentPosition)
    }
}