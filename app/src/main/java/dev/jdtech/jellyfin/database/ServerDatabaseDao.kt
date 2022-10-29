package dev.jdtech.jellyfin.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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

    @Query("select count(*) from servers")
    fun getServersCount(): Int

    @Query("delete from servers where id = :id")
    fun delete(id: String)
}
