package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserFlow(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET isSuspended = :suspended WHERE email = :email")
    suspend fun setUserSuspendedStatus(email: String, suspended: Boolean)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY isPinned DESC, timestamp DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE authorEmail = :email ORDER BY timestamp DESC")
    fun getPostsByAuthorFlow(email: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Int): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: Int)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("UPDATE posts SET likesCount = likesCount + :delta WHERE id = :postId")
    suspend fun updateLikesCount(postId: Int, delta: Int)

    @Query("UPDATE posts SET commentsCount = commentsCount + :delta WHERE id = :postId")
    suspend fun updateCommentsCount(postId: Int, delta: Int)

    @Query("UPDATE posts SET isPinned = :pinned WHERE id = :postId")
    suspend fun updatePinnedStatus(postId: Int, pinned: Boolean)
}

@Dao
interface LikeDao {
    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE postId = :postId AND userEmail = :email)")
    fun isLikedFlow(postId: Int, email: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE postId = :postId AND userEmail = :email)")
    suspend fun isLiked(postId: Int, email: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE postId = :postId AND userEmail = :email")
    suspend fun deleteLike(postId: Int, email: String)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPostFlow(postId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Int)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: Int)
}

@Dao
interface BookmarkDao {
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE postId = :postId AND userEmail = :email)")
    fun isBookmarkedFlow(postId: Int, email: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE postId = :postId AND userEmail = :email")
    suspend fun deleteBookmark(postId: Int, email: String)

    @Query("SELECT * FROM posts WHERE id IN (SELECT postId FROM bookmarks WHERE userEmail = :email) ORDER BY timestamp DESC")
    fun getBookmarkedPostsFlow(email: String): Flow<List<PostEntity>>
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history WHERE userEmail = :email ORDER BY timestamp ASC")
    fun getChatHistoryFlow(email: String): Flow<List<ChatHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatHistoryEntity)

    @Query("DELETE FROM chat_history WHERE userEmail = :email")
    suspend fun clearChatHistory(email: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getNotificationsFlow(email: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userEmail = :email")
    suspend fun markAllRead(email: String)
}

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        LikeEntity::class,
        CommentEntity::class,
        BookmarkEntity::class,
        ChatHistoryEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun likeDao(): LikeDao
    abstract fun commentDao(): CommentDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stacyverse_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
