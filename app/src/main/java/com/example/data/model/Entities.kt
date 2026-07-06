package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val username: String,
    val bio: String,
    val profilePhotoUrl: String,
    val followersCount: Int = 120,
    val followingCount: Int = 85,
    val postsCount: Int = 0,
    val isSuspended: Boolean = false,
    val role: String = "standard" // "standard" or "founder"
) {
    val isFounder: Boolean
        get() = email.equals("ahambrahmasmi415@gmail.com", ignoreCase = true)
}

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorEmail: String,
    val authorName: String,
    val authorPhotoUrl: String,
    val title: String,
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isPinned: Boolean = false
)

@Entity(tableName = "likes", primaryKeys = ["postId", "userEmail"])
data class LikeEntity(
    val postId: Int,
    val userEmail: String
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val userEmail: String,
    val username: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks", primaryKeys = ["postId", "userEmail"])
data class BookmarkEntity(
    val postId: Int,
    val userEmail: String
)

@Entity(tableName = "chat_history")
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String, // Recipient
    val type: String, // "LIKE", "COMMENT", "FOLLOW", "MENTION"
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
