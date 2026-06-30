package com.cy.loxia.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoXiaDao {
    // ==================== Wardrobe Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWardrobe(wardrobe: WardrobeEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWardrobes(wardrobes: List<WardrobeEntity>)

    @Update
    suspend fun updateWardrobe(wardrobe: WardrobeEntity)

    @Delete
    suspend fun deleteWardrobe(wardrobe: WardrobeEntity)

    @Query("DELETE FROM wardrobes WHERE id = :wardrobeId")
    suspend fun deleteWardrobeById(wardrobeId: String)

    @Query("SELECT * FROM wardrobes ORDER BY sortOrder ASC")
    suspend fun getAllWardrobes(): List<WardrobeEntity>

    /** 响应式查询：衣柜列表 Flow，数据库变化时自动通知 */
    @Query("SELECT * FROM wardrobes ORDER BY sortOrder ASC")
    fun getAllWardrobesFlow(): Flow<List<WardrobeEntity>>

    @Query("SELECT * FROM wardrobes WHERE id = :id")
    suspend fun findWardrobeById(id: String): WardrobeEntity?

    @Query("SELECT COUNT(*) FROM wardrobes")
    suspend fun getWardrobeCount(): Int

    // ==================== DressItem Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDressItem(item: DressItemEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDressItems(items: List<DressItemEntity>)

    @Update
    suspend fun updateDressItem(item: DressItemEntity)

    @Delete
    suspend fun deleteDressItem(item: DressItemEntity)

    @Query("DELETE FROM dress_items WHERE id = :itemId")
    suspend fun deleteDressItemById(itemId: String)

    @Query("DELETE FROM dress_items WHERE wardrobeId = :wardrobeId")
    suspend fun deleteDressItemsByWardrobeId(wardrobeId: String)

    @Query("SELECT * FROM dress_items")
    suspend fun getAllDressItems(): List<DressItemEntity>

    /** 响应式查询：裙子列表 Flow，数据库变化时自动通知 */
    @Query("SELECT * FROM dress_items")
    fun getAllDressItemsFlow(): Flow<List<DressItemEntity>>

    @Query("SELECT * FROM dress_items WHERE id = :id")
    suspend fun findDressItemById(id: String): DressItemEntity?

    @Query("SELECT * FROM dress_items WHERE wardrobeId = :wardrobeId")
    suspend fun getDressItemsByWardrobeId(wardrobeId: String): List<DressItemEntity>

    /** 获取指定柜子中最新的 addTime（用于推断 updatedAt） */
    @Query("SELECT MAX(addTime) FROM dress_items WHERE wardrobeId = :wardrobeId")
    suspend fun getLatestAddTimeByWardrobeId(wardrobeId: String): Long?

    @Query("SELECT * FROM dress_items WHERE wardrobeId = :wardrobeId ORDER BY pinned DESC, sortOrder ASC, buyDate DESC")
    suspend fun getSortedDressItemsByWardrobeId(wardrobeId: String): List<DressItemEntity>

    @Query("SELECT COUNT(*) FROM dress_items")
    suspend fun getDressItemCount(): Int

    @Query("SELECT COUNT(*) FROM dress_items WHERE wardrobeId = :wardrobeId")
    suspend fun getDressItemCountByWardrobeId(wardrobeId: String): Int

    // ==================== Special Queries ====================

    @Query("SELECT * FROM dress_items ORDER BY addTime ASC LIMIT 1")
    suspend fun getFirstDress(): DressItemEntity?

    @Query("SELECT * FROM dress_items ORDER BY addTime DESC LIMIT :limit")
    suspend fun getRecentDresses(limit: Int): List<DressItemEntity>

    // ==================== OutfitRecord Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfitRecord(record: OutfitRecordEntity)

    @Update
    suspend fun updateOutfitRecord(record: OutfitRecordEntity)

    @Delete
    suspend fun deleteOutfitRecord(record: OutfitRecordEntity)

    @Query("SELECT * FROM outfit_records ORDER BY date DESC")
    suspend fun getAllOutfitRecords(): List<OutfitRecordEntity>

    @Query("SELECT * FROM outfit_records WHERE id = :id")
    suspend fun findOutfitRecordById(id: Long): OutfitRecordEntity?

    @Query("SELECT * FROM outfit_records WHERE date = :date")
    suspend fun getOutfitRecordsByDate(date: String): List<OutfitRecordEntity>
}
