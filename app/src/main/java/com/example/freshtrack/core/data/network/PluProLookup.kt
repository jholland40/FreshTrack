package com.example.freshtrack.core.data.network

import android.content.Context
import com.example.freshtrack.core.model.ProductInfo
import org.json.JSONObject

/**
 * Looks a PLU sticker code up against a small bundled list of common
 * produce codes. Loose produce has no scannable barcode, so this is a
 * manual-entry lookup, not something that goes through the camera scanner.
 */
object PluProLookup {

    fun lookup(context: Context, pluCode: String): ProductInfo? {
        return try {
            val json = context.assets.open("plu_codes.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            if (!root.has(pluCode)) return null
            val entry = root.getJSONObject(pluCode)
            ProductInfo(
                barcode = pluCode,
                name = entry.getString("name"),
                category = entry.optString("category", "Produce"),
                source = "PLUPro",
            )
        } catch (e: Exception) {
            null
        }
    }
}
