package com.example.policemobiledirectory.utils

object SearchUtils {
    /**
     * Generates a normalized searchable string (searchBlob) from provided fields.
     * Rules: lowercase, no special characters, space-separated.
     */
    fun generateSearchBlob(vararg fields: String?): String {
        return fields
            .filter { !it.isNullOrBlank() }
            .joinToString(" ") { it!!.trim().lowercase() }
            .replace(Regex("[^a-z0-9\\s]"), "") // Keep alphanumeric and spaces
    }
}
