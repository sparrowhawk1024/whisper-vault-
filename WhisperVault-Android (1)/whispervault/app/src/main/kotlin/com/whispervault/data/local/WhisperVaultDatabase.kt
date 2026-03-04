package com.whispervault.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entity: Saved messages/media (local only, never synced) ──
@Entity(tableName = "saved_content")
data class SavedContentEntity(
    @PrimaryKey val id: String,
    val chatWithUserId: String,
    val encryptedData: String,   // AES-GCM encrypted content
    val encryptedIv: String,
    val type: String,            // "text" or "image"
    val savedAt: Long = System.currentTimeMillis()
)

// ── Entity: Connections (persisted for logged-in users) ──
@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val avatarId: Int,
    val mobileHash: String? = null,
    val localContactName: String? = null,
    val connectedAt: Long = System.currentTimeMillis()
)

// ── Entity: App preferences ──
@Entity(tableName = "app_prefs")
data class AppPrefEntity(
    @PrimaryKey val key: String,
    val value: String
)

// ── DAOs ──
@Dao
interface SavedContentDao {
    @Query("SELECT * FROM saved_content WHERE chatWithUserId = :userId ORDER BY savedAt DESC")
    fun getSavedForChat(userId: String): Flow<List<SavedContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: SavedContentEntity)

    @Delete
    suspend fun delete(content: SavedContentEntity)

    @Query("DELETE FROM saved_content WHERE chatWithUserId = :userId")
    suspend fun deleteAllForChat(userId: String)
}

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY COALESCE(localContactName, displayName, userId) ASC")
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conn: ConnectionEntity)

    @Query("DELETE FROM connections WHERE userId = :userId")
    suspend fun remove(userId: String)

    @Query("SELECT * FROM connections WHERE userId = :userId LIMIT 1")
    suspend fun getConnection(userId: String): ConnectionEntity?

    @Query("DELETE FROM connections")
    suspend fun clearAll()
}

@Dao
interface AppPrefDao {
    @Query("SELECT value FROM app_prefs WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(pref: AppPrefEntity)

    @Query("DELETE FROM app_prefs WHERE `key` = :key")
    suspend fun delete(key: String)
}

// ── Database ──
@Database(
    entities = [SavedContentEntity::class, ConnectionEntity::class, AppPrefEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WhisperVaultDatabase : RoomDatabase() {
    abstract fun savedContentDao(): SavedContentDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun appPrefDao(): AppPrefDao
}
