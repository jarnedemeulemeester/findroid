package dev.jdtech.jellyfin.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ServerDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(server: Server)

    @Update
    fun update(server: Server)

    @Query("select * from servers where id = :id")
    fun get(id: String): Server?

    @Query("delete from servers")
    fun clear()

    @Query("select * from servers")
    fun getAllServers(): LiveData<List<Server>>

    @Query("select * from servers")
    fun getAllServersSync(): List<Server>

    @Query("delete from servers where id = :id")
    fun delete(id: String)
}