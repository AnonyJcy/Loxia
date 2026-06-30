package com.cy.loxia

import org.json.JSONException
import org.json.JSONObject

data class Wardrobe @JvmOverloads constructor(
    val id: String,
    val name: String,
    val count: Int = 0,
    val updatedAt: Long = 0L,
    val isDemo: Boolean = false,
    val cover: String = "",
    val sortOrder: Int = 0
) {
    @Throws(JSONException::class)
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("count", count)
            put("updatedAt", updatedAt)
            put("isDemo", isDemo)
            put("cover", cover)
            put("sortOrder", sortOrder)
        }
    }

    companion object {
        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(json: JSONObject): Wardrobe {
            return Wardrobe(
                id = json.optString("id", ""),
                name = json.optString("name", ""),
                count = json.optInt("count", 0),
                updatedAt = json.optLong("updatedAt", 0L),
                isDemo = json.optBoolean("isDemo", false),
                cover = json.optString("cover", ""),
                sortOrder = json.optInt("sortOrder", 0)
            )
        }
    }
}
