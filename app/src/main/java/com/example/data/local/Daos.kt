package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Transaction
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPostsWithMedia(): Flow<List<PostWithMedia>>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun getAllPostsWithMediaSync(): List<PostWithMedia>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentPostsWithMedia(limit: Int): Flow<List<PostWithMedia>>

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostWithMediaById(postId: String): PostWithMedia?

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :postId")
    fun observePostWithMediaById(postId: String): Flow<PostWithMedia?>

    @Query("SELECT * FROM posts")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>): List<Long>

    @Update
    suspend fun updateMediaItem(item: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE postId = :postId ORDER BY itemIndex ASC")
    fun getMediaItemsByPost(postId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE postId = :postId AND kind = :kind AND itemIndex = :itemIndex LIMIT 1")
    suspend fun findMediaItem(postId: String, kind: String, itemIndex: Int): MediaItemEntity?

    @Query("UPDATE media_items SET status = :status, mediaStoreUri = :uri, sizeBytes = :sizeBytes, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatusSuccess(id: Long, status: String, completedAt: Long, sizeBytes: Long, uri: String)

    @Query("UPDATE media_items SET status = :status, errorReason = :errorReason WHERE id = :id")
    suspend fun updateStatusFailed(id: Long, status: String, errorReason: String?)

    @Query("UPDATE media_items SET status = :newStatus WHERE status = :oldStatus")
    suspend fun reconcileOrphanStatus(oldStatus: String, newStatus: String)

    suspend fun reconcileStuckDownloadingItems() {
        reconcileOrphanStatus("DOWNLOADING", "FAILED")
    }

    @Query("DELETE FROM media_items WHERE postId = :postId")
    suspend fun deleteMediaItemsByPost(postId: String)

    @Query("DELETE FROM media_items")
    suspend fun deleteAllMediaItems()
}
