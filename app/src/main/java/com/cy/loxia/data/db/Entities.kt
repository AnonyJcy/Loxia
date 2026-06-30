package com.cy.loxia.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "wardrobes",
    indices = [Index(value = ["sortOrder"])]
)
data class WardrobeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val count: Int = 0,
    val updatedAt: Long = 0L,
    val isDemo: Boolean = false,
    val cover: String = "",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "dress_items",
    foreignKeys = [
        ForeignKey(
            entity = WardrobeEntity::class,
            parentColumns = ["id"],
            childColumns = ["wardrobeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["wardrobeId"]),
        Index(value = ["sortOrder"]),
        Index(value = ["addTime"])
    ]
)
@TypeConverters(Converters::class)
data class DressItemEntity(
    @PrimaryKey
    val id: String,
    val wardrobeId: String,
    val name: String,
    val store: String,
    val channel: String,
    val price: Double,
    val buyDate: String,
    val status: String,
    val remindAt: String,
    val remark: String,
    val earnestMoney: Double = 0.0,
    val isFullPayment: Boolean = false,
    val fullPaymentAmount: Double = 0.0,
    val tailPayment: Double = 0.0,
    val imageUri: String = "",
    val shippingFee: String = "包邮",
    val deposit: Double = 0.0,
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val daiqiangDate: String = "",
    val yixiangDate: String = "",
    val dingjinDate: String = "",
    val buweikuanDate: String = "",
    val isWishlist: Boolean = false,
    val addTime: Long = System.currentTimeMillis(),
    val shipmentDate: String = "",
    val receivedDate: String = "",
    val expectedShipmentDate: String = ""
)

@Entity(
    tableName = "outfit_records",
    indices = [Index(value = ["date"])]
)
@TypeConverters(Converters::class)
data class OutfitRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val dressIds: List<String>,
    val photoPath: String = "",
    val mood: String = "",
    val createTime: Long = System.currentTimeMillis()
)
