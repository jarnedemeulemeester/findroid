package dev.jdtech.jellyfin.player.core.domain.utils

import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter

object ChapterUtils {
    fun getCurrentChapterIndex(chapters: List<PlayerChapter>, currentPosition: Long): Int? {
        for (i in chapters.indices.reversed()) {
            if (chapters[i].startPosition < currentPosition) {
                return i
            }
        }
        return null
    }

    fun getNextChapterIndex(chapters: List<PlayerChapter>, currentPosition: Long): Int? {
        val currentChapterIndex = getCurrentChapterIndex(chapters, currentPosition) ?: return null
        return minOf(chapters.size - 1, currentChapterIndex + 1)
    }

    fun getPreviousChapterIndex(chapters: List<PlayerChapter>, currentPosition: Long): Int? {
        val currentChapterIndex = getCurrentChapterIndex(chapters, currentPosition) ?: return null
        if (currentPosition > chapters[currentChapterIndex].startPosition + 5000L) {
            return currentChapterIndex
        }
        return maxOf(0, currentChapterIndex - 1)
    }

    fun isLastChapter(chapters: List<PlayerChapter>, currentPosition: Long): Boolean {
        val currentChapterIndex = getCurrentChapterIndex(chapters, currentPosition) ?: return false
        return currentChapterIndex == chapters.size - 1
    }
}
