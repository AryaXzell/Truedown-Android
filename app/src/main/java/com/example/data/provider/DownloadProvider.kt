package com.example.data.provider

import com.example.domain.model.ResolvedPost

interface DownloadProvider {
    suspend fun resolve(url: String): Result<ResolvedPost>
    fun getCached(url: String): ResolvedPost?
}
