package com.cy.loxia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WardrobeEntity::class,
        DressItemEntity::class,
        OutfitRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LoXiaDatabase : RoomDatabase() {
    abstract fun loXiaDao(): LoXiaDao

    companion object {
        @Volatile
        private var INSTANCE: LoXiaDatabase? = null

        /**
         * Migration: 版本 1 → 2
         * 将 wardrobes.updatedAt 从 TEXT 迁移到 INTEGER (epoch millis)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加临时列，复制数据，删除旧列，重命名
                db.execSQL("ALTER TABLE wardrobes ADD COLUMN updatedAt_new INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE wardrobes SET updatedAt_new = 0")  // 旧数据设为 0
                db.execSQL("ALTER TABLE wardrobes DROP COLUMN updatedAt")
                db.execSQL("ALTER TABLE wardrobes RENAME COLUMN updatedAt_new TO updatedAt")
            }
        }

        /**
         * Migration: 版本 2 → 3
         * 新增发货/收货时间字段
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dress_items ADD COLUMN shipmentDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dress_items ADD COLUMN receivedDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dress_items ADD COLUMN expectedShipmentDate TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): LoXiaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LoXiaDatabase::class.java,
                    "loxia_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
