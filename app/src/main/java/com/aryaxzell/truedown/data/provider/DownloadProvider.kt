package com.aryaxzell.truedown.data.provider

import com.aryaxzell.truedown.domain.model.ResolvedPost

interface DownloadProvider {
    suspend fun resolve(url: String): Result<ResolvedPost>
    fun getCached(url: String): ResolvedPost?
}
