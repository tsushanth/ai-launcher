package com.launcher.data.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false,
    val installTime: Long = 0,
    val lastUsedTime: Long = 0,
    val launchCount: Int = 0
) {
    /**
     * Calculate search relevance score for fuzzy matching
     */
    fun calculateSearchScore(query: String): Float {
        if (query.isBlank()) return 0f

        val lowerQuery = query.lowercase()
        val lowerName = name.lowercase()
        val lowerPackage = packageName.lowercase()

        var score = 0f

        // Exact match (highest priority)
        if (lowerName == lowerQuery) {
            score += 100f
        }

        // Starts with query (high priority)
        if (lowerName.startsWith(lowerQuery)) {
            score += 50f
        }

        // Contains query (medium priority)
        if (lowerName.contains(lowerQuery)) {
            score += 25f
        }

        // Acronym match (e.g., "gm" matches "Gmail")
        val acronym = name.split(" ")
            .mapNotNull { it.firstOrNull()?.lowercase() }
            .joinToString("")

        if (acronym.contains(lowerQuery)) {
            score += 15f
        }

        // Package name match (low priority)
        if (lowerPackage.contains(lowerQuery)) {
            score += 5f
        }

        // Boost for frequently used apps
        if (launchCount > 10) {
            score += 5f
        }

        return score
    }
}
