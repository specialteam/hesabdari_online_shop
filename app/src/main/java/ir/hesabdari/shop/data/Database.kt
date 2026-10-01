package ir.hesabdari.shop.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(SEP)

    @TypeConverter
    fun toTags(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(SEP)

    private companion object {
        const val SEP = "\u001F"
    }
}

@Dao
interface DealDao {
    @Query("SELECT * FROM deals ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<Deal>>

    @Query("SELECT * FROM deals WHERE id = :id")
    fun observe(id: Long): Flow<Deal?>

    @Query("SELECT * FROM deals WHERE id = :id")
    suspend fun get(id: Long): Deal?

    @Query("SELECT * FROM deals WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<Deal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(deal: Deal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deals: List<Deal>)

    @Query("DELETE FROM deals WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM deals")
    suspend fun clear()
}

@Database(entities = [Deal::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dealDao(): DealDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "hesabdari.db").build()
    }
}

class DealRepository(private val db: AppDatabase) {
    private val dao = db.dealDao()

    val deals: Flow<List<Deal>> = dao.observeAll()

    fun observe(id: Long): Flow<Deal?> = dao.observe(id)
    suspend fun get(id: Long): Deal? = dao.get(id)
    suspend fun getAll(ids: List<Long>): List<Deal> = dao.getAll(ids)
    suspend fun save(deal: Deal): Long = dao.upsert(deal)
    suspend fun delete(ids: List<Long>) = dao.deleteByIds(ids)

    /** Replaces everything (restore). */
    suspend fun replaceAll(deals: List<Deal>) = db.withTransaction {
        dao.clear()
        dao.insertAll(deals)
    }

    /** Adds deals as new rows (merge restore). */
    suspend fun addAll(deals: List<Deal>) = dao.insertAll(deals.map { it.copy(id = 0) })
}
