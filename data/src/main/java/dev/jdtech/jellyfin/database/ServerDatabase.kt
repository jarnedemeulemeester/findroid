package dev.jdtech.jellyfin.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.jdtech.jellyfin.models.JellyCastEpisodeDto
import dev.jdtech.jellyfin.models.JellyCastMediaStreamDto
import dev.jdtech.jellyfin.models.JellyCastMovieDto
import dev.jdtech.jellyfin.models.JellyCastSeasonDto
import dev.jdtech.jellyfin.models.JellyCastSegmentDto
import dev.jdtech.jellyfin.models.JellyCastShowDto
import dev.jdtech.jellyfin.models.JellyCastSourceDto
import dev.jdtech.jellyfin.models.JellyCastTrickplayInfoDto
import dev.jdtech.jellyfin.models.JellyCastUserDataDto
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.User

@Database(
    entities = [Server::class, ServerAddress::class, User::class, JellyCastMovieDto::class, JellyCastShowDto::class, JellyCastSeasonDto::class, JellyCastEpisodeDto::class, JellyCastSourceDto::class, JellyCastMediaStreamDto::class, JellyCastUserDataDto::class, JellyCastTrickplayInfoDto::class, JellyCastSegmentDto::class],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = ServerDatabase.TrickplayMigration::class),
        AutoMigration(from = 5, to = 6, spec = ServerDatabase.IntrosMigration::class),
    ],
)
@TypeConverters(Converters::class)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun getServerDatabaseDao(): ServerDatabaseDao

    @DeleteTable(tableName = "trickPlayManifests")
    class TrickplayMigration : AutoMigrationSpec

    @DeleteTable(tableName = "intros")
    class IntrosMigration : AutoMigrationSpec
}

val MIGRATION_6_7 = object : Migration(startVersion = 6, endVersion = 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DROP TABLE segments",
        )
        db.execSQL(
            "CREATE TABLE segments (`itemId` TEXT NOT NULL, `type` TEXT NOT NULL, `startTicks` INTEGER NOT NULL, `endTicks` INTEGER NOT NULL, PRIMARY KEY(`itemId`, `type`), FOREIGN KEY(`itemId`) REFERENCES `episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }
}
