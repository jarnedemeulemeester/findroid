package dev.jdtech.jellyfin.di

import android.app.Application
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifestFactory
import dev.jdtech.jellyfin.offline.OfflineDownloadCoordinator
import dev.jdtech.jellyfin.offline.OfflineDownloadCoordinatorImpl
import dev.jdtech.jellyfin.offline.OfflineDownloadManager
import dev.jdtech.jellyfin.offline.OfflineDownloadManagerImpl
import dev.jdtech.jellyfin.offline.OfflineDownloadWorkScheduler
import dev.jdtech.jellyfin.offline.OfflineReadyVideoRepairer
import dev.jdtech.jellyfin.offline.WorkManagerOfflineDownloadWorkScheduler
import dev.jdtech.jellyfin.offline.DirectFileOfflineReadyVideoRepairer
import dev.jdtech.jellyfin.offline.storage.AllFilesAccessHelper
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetStore
import dev.jdtech.jellyfin.offline.storage.OfflineTempPackageCleaner
import dev.jdtech.jellyfin.offline.transfer.OfflineAssetTransferRunner
import dev.jdtech.jellyfin.offline.transfer.OfflineVideoPostProcessor
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.repository.OfflineTransferPlanner
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.Downloader
import dev.jdtech.jellyfin.utils.DownloaderImpl
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DownloaderModule {
    @Singleton
    @Provides
    fun provideOfflinePackageManifestFactory(): OfflinePackageManifestFactory {
        return OfflinePackageManifestFactory()
    }

    @Singleton
    @Provides
    fun provideDirectFileAssetStore(application: Application): DirectFileAssetStore {
        return DirectFileAssetStore(application)
    }

    @Singleton
    @Provides
    fun provideOfflineTempPackageCleaner(
        directFileAssetStore: DirectFileAssetStore
    ): OfflineTempPackageCleaner = directFileAssetStore

    @Singleton
    @Provides
    fun provideAllFilesAccessHelper(application: Application): AllFilesAccessHelper {
        return AllFilesAccessHelper(application)
    }

    @Singleton
    @Provides
    fun provideOfflineDownloadOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Singleton
    @Provides
    fun provideOfflineVideoPostProcessor(): OfflineVideoPostProcessor {
        return OfflineVideoPostProcessor()
    }

    @Singleton
    @Provides
    fun provideOfflineAssetTransferRunner(
        okHttpClient: OkHttpClient,
        directFileAssetStore: DirectFileAssetStore,
        offlineVideoPostProcessor: OfflineVideoPostProcessor,
    ): OfflineAssetTransferRunner {
        return OfflineAssetTransferRunner(
            okHttpClient = okHttpClient,
            directFileAssetStore = directFileAssetStore,
            offlineVideoPostProcessor = offlineVideoPostProcessor,
        )
    }

    @Singleton
    @Provides
    fun provideOfflineReadyVideoRepairer(
        directFileAssetStore: DirectFileAssetStore,
        offlineVideoPostProcessor: OfflineVideoPostProcessor,
    ): OfflineReadyVideoRepairer {
        return DirectFileOfflineReadyVideoRepairer(
            directFileAssetStore = directFileAssetStore,
            offlineVideoPostProcessor = offlineVideoPostProcessor,
        )
    }

    @Singleton
    @Provides
    fun provideOfflineDownloadCoordinator(
        offlinePackageRepository: OfflinePackageRepository,
        directFileAssetStore: DirectFileAssetStore,
        offlineAssetTransferRunner: OfflineAssetTransferRunner,
    ): OfflineDownloadCoordinator {
        return OfflineDownloadCoordinatorImpl(
            offlinePackageRepository = offlinePackageRepository,
            directFileAssetStore = directFileAssetStore,
            offlineAssetTransferRunner = offlineAssetTransferRunner,
        )
    }

    @Singleton
    @Provides
    fun provideOfflineDownloadWorkScheduler(workManager: WorkManager): OfflineDownloadWorkScheduler {
        return WorkManagerOfflineDownloadWorkScheduler(workManager)
    }

    @Singleton
    @Provides
    fun provideOfflineDownloadManager(
        offlineDownloadCoordinator: OfflineDownloadCoordinator,
        offlineDownloadWorkScheduler: OfflineDownloadWorkScheduler,
        offlinePackageRepository: OfflinePackageRepository,
        directFileAssetStore: DirectFileAssetStore,
    ): OfflineDownloadManager {
        return OfflineDownloadManagerImpl(
            offlineDownloadCoordinator = offlineDownloadCoordinator,
            offlineDownloadWorkScheduler = offlineDownloadWorkScheduler,
            offlinePackageRepository = offlinePackageRepository,
            directFileAssetStore = directFileAssetStore,
        )
    }

    @Singleton
    @Provides
    fun provideDownloader(
        application: Application,
        serverDatabase: ServerDatabaseDao,
        jellyfinRepository: JellyfinRepository,
        appPreferences: AppPreferences,
        workManager: WorkManager,
    ): Downloader {
        return DownloaderImpl(
            application,
            serverDatabase,
            jellyfinRepository,
            appPreferences,
            workManager,
        )
    }
}
