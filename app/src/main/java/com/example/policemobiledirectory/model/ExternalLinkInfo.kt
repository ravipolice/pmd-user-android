package com.example.policemobiledirectory.model

data class ExternalLinkInfo(
    val name: String = "",
    val playStoreUrl: String = "",
    val apkUrl: String? = null,
    val iconUrl: String? = null,
    val category: String = "General",
    val isActive: Boolean = true,
    val documentId: String? = null
)
