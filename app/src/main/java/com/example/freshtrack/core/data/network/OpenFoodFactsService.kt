package com.example.freshtrack.core.data.network

import com.example.freshtrack.core.model.ProductInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks a barcode up against the public Open Food Facts database.
 * Used for packaged goods (anything with a real UPC/EAN barcode).
 */
object OpenFoodFactsService {

    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            if (json.optInt("status") != 1) return@withContext null

            val product = json.getJSONObject("product")
            ProductInfo(
                barcode = barcode,
                name = product.optString("product_name").ifBlank { return@withContext null },
                category = product.optStringOrNull("categories_tags_en")
                    ?.split(",")?.firstOrNull()?.replace("en:", ""),
                brand = product.optStringOrNull("brands"),
                imageUrl = product.optStringOrNull("image_front_url"),
                source = "OpenFoodFacts",
            )
        } catch (e: Exception) {
            null
        }
    }

    /** optString() with no fallback returns "" when missing; turn that into a real null. */
    private fun JSONObject.optStringOrNull(key: String): String? =
        optString(key).takeIf { it.isNotBlank() }
}
