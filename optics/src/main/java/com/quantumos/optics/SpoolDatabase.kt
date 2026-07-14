package com.quantumos.optics

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "spool_logs")
data class SpoolLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val timestamp: Long,
    val iso: Int,
    val shutterSpeed: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Float,
    val pitch: Float,
    val filmProfile: String,
    val isDeveloping: Boolean = false,
    val devStartTime: Long = 0L
)

@Dao
interface SpoolLogDao {
    @Query("SELECT * FROM spool_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SpoolLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SpoolLog): Long

    @Query("UPDATE spool_logs SET isDeveloping = 0 WHERE id = :id")
    suspend fun completeDevelopment(id: Long)

    @Query("DELETE FROM spool_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM spool_logs")
    suspend fun deleteAllLogs()
}

@Database(entities = [SpoolLog::class], version = 2, exportSchema = false)
abstract class SpoolDatabase : RoomDatabase() {
    abstract fun spoolLogDao(): SpoolLogDao

    companion object {
        @Volatile
        private var INSTANCE: SpoolDatabase? = null

        fun getDatabase(context: Context): SpoolDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpoolDatabase::class.java,
                    "spool_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
