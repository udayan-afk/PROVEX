package com.provex.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Single-row table holding the self-reported career profile as JSON. */
@Entity(tableName = "profile")
data class ProfileEntity(@PrimaryKey val id: Int = 0, val json: String)

@Entity(tableName = "analyses")
data class AnalysisEntity(@PrimaryKey val id: String, val createdAt: Long, val json: String)

/** Immutable history record. Rows are only ever inserted, never replaced. */
@Entity(tableName = "snapshots", indices = [Index("analysisId")])
data class SnapshotEntity(
    @PrimaryKey val id: String,
    val analysisId: String,
    val createdAt: Long,
    val json: String,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 0")
    fun observe(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE id = :id")
    fun observe(id: String): Flow<AnalysisEntity?>

    @Query("SELECT * FROM analyses WHERE id = :id")
    suspend fun get(id: String): AnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AnalysisEntity)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SnapshotEntity>>

    /** Insert-only: a clashing id is a bug, not something to overwrite. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SnapshotEntity)
}

@Database(
    entities = [ProfileEntity::class, AnalysisEntity::class, SnapshotEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ProvexDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun snapshotDao(): SnapshotDao
}
