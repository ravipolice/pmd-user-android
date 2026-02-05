package com.example.policemobiledirectory.utils

object SearchUtils {
    /**
     * Generates a normalized searchable string (searchBlob) from provided fields.
     * Rules: 
     * 1. Lowercase everything
     * 2. Include original text
     * 3. Include text without spaces (e.g. "ravikumar")
     * 4. Include text without dots (e.g. "bmravi")
     * 5. Include only alphanumeric characters
     * 6. Handle +91 for mobile numbers
     */
    fun generateSearchBlob(vararg fields: String?): String {
        val nonNullFields = fields.filterNotNull().filter { it.isNotBlank() }
        
        val variations = nonNullFields.flatMap { field ->
            val clean = field.trim().lowercase()
            listOf(
                clean,                                      // original lowercase
                clean.replace(" ", ""),    // no spaces
                clean.replace(".", ""),    // no dots
                clean.replace(Regex("[^a-z0-9]"), "") // purely alphanumeric
            )
        }

        // Specific handling for +91 in mobile numbers
        val mobileVariations = nonNullFields
            .filter { it.contains("+91") }
            .map { it.replace("+91", "").trim().lowercase() }

        return (variations + mobileVariations)
            .distinct()
            .joinToString(" ")
    }
}
