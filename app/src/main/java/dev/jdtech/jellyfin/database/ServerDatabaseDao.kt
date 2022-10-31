package dev.jdtech.jellyfin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.models.ServerWithAddressesAndUsers
import java.util.UUID

@Dao
interface ServerDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertServer(server: Server)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertServerAddress(address: ServerAddress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User)

    @Update
    fun update(server: Server)

    @Query("select * from servers where id = :id")
    fun get(id: String): Server?

    @Query("select * from users where id = :id")
    fun getUser(id: UUID): User?

    @Transaction
    @Query("select * from servers where id = :id")
    fun getServerWithAddresses(id: String): ServerWithAddresses

    @Transaction
    @Query("select * from servers where id = :id")
    fun getServerWithUsers(id: String): ServerWithAddresses

    @Transaction
    @Query("select * from servers where id = :id")
    fun getServerWithAddressesAndUsers(id: String): ServerWithAddressesAndUsers?

    @Query("delete from servers")
    fun clear()

    @Query("select * from servers")
    fun getAllServers(): LiveData<List<Server>>

    @Query("select * from servers")
    fun getAllServersSync(): List<Server>

    @Query("select count(*) from servers")
    fun getServersCount(): Int

    @Query("delete from servers where id = :id")
    fun delete(id: String)
}
