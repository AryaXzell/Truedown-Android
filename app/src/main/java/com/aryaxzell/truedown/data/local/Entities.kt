package com.aryaxzell.truedown.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(
    tableName = "posts",
    indices = [Index(value = ["createdAt"])]
)
data class PostEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val title: String,
    val authorName: String,
    val authorHandle: String,
    val sourceUrl: String,
    val hasAudio: Boolean,
    val photoCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["status"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postId: String,
    val kind: String,
    val itemIndex: Int = 0,
    val fileName: String,
    val mediaStoreUri: String = "",
    val status: String,
    val quality: String = "STANDARD",
    val sizeBytes: Long = 0L,
    val completedAt: Long = 0L,
    val errorReason: String? = null
)

data class PostWithMedia(
    @Embedded
    val post: PostEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "postId"
    )
    val mediaItems: List<MediaItemEntity>
)

fun PostWithMedia.toResolvedPost(): com.aryaxzell.truedown.domain.model.ResolvedPost {
    val photoUrls = this.mediaItems
        .filter { it.kind == "PHOTO" }
        .sortedBy { it.itemIndex }
        .map { it.mediaStoreUri.ifBlank { it.fileName } }
    val videoItem = this.mediaItems.firstOrNull { it.kind == "VIDEO" }
    val audioItem = this.mediaItems.firstOrNull { it.kind == "AUDIO" }
    return com.aryaxzell.truedown.domain.model.ResolvedPost(
        id = this.post.id,
        type = com.aryaxzell.truedown.domain.model.PostType.valueOf(this.post.type),
        title = this.post.title,
        authorName = this.post.authorName,
        authorHandle = this.post.authorHandle,
        videoStandardUrl = videoItem?.mediaStoreUri,
        videoHdUrl = videoItem?.mediaStoreUri,
        photoUrls = photoUrls,
        audioUrl = audioItem?.mediaStoreUri,
        coverUrl = videoItem?.mediaStoreUri ?: photoUrls.firstOrNull(),
        durationSec = 0,
        sourceUrl = this.post.sourceUrl
    )
}
