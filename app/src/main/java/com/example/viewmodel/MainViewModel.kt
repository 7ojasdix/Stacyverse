package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val postDao = db.postDao()
    private val likeDao = db.likeDao()
    private val commentDao = db.commentDao()
    private val bookmarkDao = db.bookmarkDao()
    private val chatHistoryDao = db.chatHistoryDao()
    private val notificationDao = db.notificationDao()

    // Authentication States
    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) userDao.getUserFlow(email) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Splash / Intro States
    private val _introFinished = MutableStateFlow(false)
    val introFinished: StateFlow<Boolean> = _introFinished.asStateFlow()

    // Global Feed
    val allPosts: StateFlow<List<PostEntity>> = postDao.getAllPostsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab Navigation
    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Exploration & Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    val searchedPosts: StateFlow<List<PostEntity>> = combine(
        allPosts,
        _searchQuery,
        _selectedGenre
    ) { posts, query, genre ->
        posts.filter { post ->
            val matchesQuery = query.isEmpty() ||
                    post.title.contains(query, ignoreCase = true) ||
                    post.content.contains(query, ignoreCase = true) ||
                    post.authorName.contains(query, ignoreCase = true)
            val matchesGenre = genre == "All" ||
                    post.content.contains("#$genre", ignoreCase = true) ||
                    post.title.contains(genre, ignoreCase = true)
            matchesQuery && matchesGenre
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stacy AI State
    val chatHistory: StateFlow<List<ChatHistoryEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) chatHistoryDao.getChatHistoryFlow(email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    // Notifications State
    val notifications: StateFlow<List<NotificationEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) notificationDao.getNotificationsFlow(email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks State
    val bookmarkedPosts: StateFlow<List<PostEntity>> = _currentUserEmail
        .flatMapLatest { email ->
            if (email != null) bookmarkDao.getBookmarkedPostsFlow(email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Users List
    val allUsers: StateFlow<List<UserEntity>> = userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local reports for abuse moderation
    private val _reportedPostIds = MutableStateFlow<Set<Int>>(emptySet())
    val reportedPostIds: StateFlow<Set<Int>> = _reportedPostIds.asStateFlow()

    // General Message/Error Broadcast State
    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    init {
        // Pre-create standard default users and some exciting posts for rich visual layout
        viewModelScope.launch {
            seedInitialDatabase()
        }
    }

    private suspend fun seedInitialDatabase() {
        val count = db.openHelper.writableDatabase.compileStatement("SELECT COUNT(*) FROM users").simpleQueryForLong()
        if (count == 0L) {
            // Register Founder/Admin
            val adminUser = UserEntity(
                email = "ahambrahmasmi415@gmail.com",
                username = "NeoFounder",
                bio = "Creator of StacyVerse. Crafting screenplays on the edge of the galaxy.",
                profilePhotoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                role = "founder"
            )
            userDao.insertUser(adminUser)

            // Register standard user
            val standardUser = UserEntity(
                email = "user@stacyverse.com",
                username = "StellarScribe",
                bio = "Sci-fi writer looking for co-authors. Plot twist lover.",
                profilePhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                role = "standard"
            )
            userDao.insertUser(standardUser)

            // Seed some beautiful sci-fi posts
            postDao.insertPost(
                PostEntity(
                    authorEmail = "ahambrahmasmi415@gmail.com",
                    authorName = "NeoFounder",
                    authorPhotoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    title = "The Neon Monolith - Chapter 1",
                    content = "The rain in Sector 7 never stopped. It fell in thick oily droplets, refracting the pink neon glare of the floating advertisements above. Jax stared at the obsidian slab hummed softly in the center of the landfill. It wasn't there yesterday. #Cyberpunk #SpaceOpera",
                    likesCount = 45,
                    commentsCount = 2,
                    isPinned = true
                )
            )

            postDao.insertPost(
                PostEntity(
                    authorEmail = "user@stacyverse.com",
                    authorName = "StellarScribe",
                    authorPhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    title = "Beyond Event Horizon - Movie Concept",
                    content = "A high-concept thriller pitch: An astronomical team discovers that the supermassive black hole Sagittarius A* has stopped drawing objects in. Instead, it is emitting organized electromagnetic pulses. We decode it. It's a clock. And it is counting down. #Thriller #SciFi",
                    likesCount = 28,
                    commentsCount = 1,
                    isPinned = false
                )
            )
        }
    }

    // --- Authentication ---

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _messageEvent.emit("Please enter credentials.")
                onResult(false)
                return@launch
            }
            val user = userDao.getUserByEmail(email)
            if (user == null) {
                // To make first launch frictionless, we will auto-register user on first login with dummy password!
                val resolvedRole = if (email.equals("ahambrahmasmi415@gmail.com", ignoreCase = true)) "founder" else "standard"
                val newUser = UserEntity(
                    email = email,
                    username = email.substringBefore("@"),
                    bio = "New writer at StacyVerse.",
                    profilePhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    role = resolvedRole
                )
                userDao.insertUser(newUser)
                _currentUserEmail.value = email
                _messageEvent.emit("Welcome aboard, ${newUser.username}!")
                onResult(true)
            } else {
                if (user.isSuspended) {
                    _messageEvent.emit("Your account has been suspended by the Founder.")
                    onResult(false)
                } else {
                    _currentUserEmail.value = email
                    _messageEvent.emit("Welcome back, ${user.username}!")
                    onResult(true)
                }
            }
        }
    }

    fun signup(email: String, username: String, bio: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || username.isBlank()) {
                _messageEvent.emit("Please fill in required fields.")
                onResult(false)
                return@launch
            }
            val existing = userDao.getUserByEmail(email)
            if (existing != null) {
                _messageEvent.emit("An account with this email already exists.")
                onResult(false)
                return@launch
            }

            val resolvedRole = if (email.equals("ahambrahmasmi415@gmail.com", ignoreCase = true)) "founder" else "standard"
            val user = UserEntity(
                email = email,
                username = username,
                bio = bio.ifBlank { "Exploring the sci-fi boundaries." },
                profilePhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                role = resolvedRole
            )
            userDao.insertUser(user)
            _currentUserEmail.value = email
            _messageEvent.emit("Account synchronized. Welcome to StacyVerse!")
            onResult(true)
        }
    }

    fun logout() {
        _currentUserEmail.value = null
        _activeTab.value = "home"
    }

    fun forgotPassword(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                _messageEvent.emit("Please enter your email.")
                onResult(false)
                return@launch
            }
            _messageEvent.emit("Google Sign-In link sent to $email.")
            onResult(true)
        }
    }

    fun updateProfile(username: String, bio: String, photoUrl: String) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val user = userDao.getUserByEmail(email) ?: return@launch
            val updatedUser = user.copy(
                username = username,
                bio = bio,
                profilePhotoUrl = photoUrl.ifBlank { user.profilePhotoUrl }
            )
            userDao.updateUser(updatedUser)
            _messageEvent.emit("Neural signature updated.")
        }
    }

    // --- Cinematic Intro ---

    fun setIntroFinished() {
        _introFinished.value = true
    }

    // --- Navigation ---

    fun selectTab(tab: String) {
        _activeTab.value = tab
    }

    // --- Search / Filters ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenreFilter(genre: String) {
        _selectedGenre.value = genre
    }

    // --- Feed & Posts ---

    fun createPost(title: String, content: String, imageUri: String?) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val user = userDao.getUserByEmail(email) ?: return@launch

            if (title.isBlank() || content.isBlank()) {
                _messageEvent.emit("Stories require a title and body.")
                return@launch
            }

            val newPost = PostEntity(
                authorEmail = email,
                authorName = user.username,
                authorPhotoUrl = user.profilePhotoUrl,
                title = title,
                content = content,
                imageUri = imageUri
            )

            postDao.insertPost(newPost)
            // Increment local posts count
            userDao.updateUser(user.copy(postsCount = user.postsCount + 1))
            _messageEvent.emit("Story transmitted to feed.")
        }
    }

    fun deletePost(post: PostEntity) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val user = userDao.getUserByEmail(email) ?: return@launch

            // Authorization check
            if (user.isFounder || post.authorEmail == email) {
                postDao.deletePostById(post.id)
                // Decrement post count if author deleted it
                if (post.authorEmail == email) {
                    userDao.updateUser(user.copy(postsCount = (user.postsCount - 1).coerceAtLeast(0)))
                }
                _messageEvent.emit("Story removed from StacyVerse.")
            } else {
                _messageEvent.emit("Unauthorized transmission deletion blocked.")
            }
        }
    }

    fun toggleLike(post: PostEntity) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val liked = likeDao.isLiked(post.id, email)

            if (liked) {
                likeDao.deleteLike(post.id, email)
                postDao.updateLikesCount(post.id, -1)
            } else {
                likeDao.insertLike(LikeEntity(postId = post.id, userEmail = email))
                postDao.updateLikesCount(post.id, 1)

                // Add notification to author
                if (post.authorEmail != email) {
                    val user = userDao.getUserByEmail(email)
                    val senderName = user?.username ?: "An unknown explorer"
                    notificationDao.insertNotification(
                        NotificationEntity(
                            userEmail = post.authorEmail,
                            type = "LIKE",
                            title = "Story liked!",
                            content = "$senderName synchronized with your story \"${post.title}\"."
                        )
                    )
                }
            }
        }
    }

    fun isPostLiked(postId: Int): Flow<Boolean> {
        val email = _currentUserEmail.value ?: return flowOf(false)
        return likeDao.isLikedFlow(postId, email)
    }

    fun toggleBookmark(post: PostEntity) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val bookmarkedFlow = bookmarkDao.isBookmarkedFlow(post.id, email).first()

            if (bookmarkedFlow) {
                bookmarkDao.deleteBookmark(post.id, email)
                _messageEvent.emit("Unbookmarked.")
            } else {
                bookmarkDao.insertBookmark(BookmarkEntity(postId = post.id, userEmail = email))
                _messageEvent.emit("Bookmarked to database.")
            }
        }
    }

    fun isPostBookmarked(postId: Int): Flow<Boolean> {
        val email = _currentUserEmail.value ?: return flowOf(false)
        return bookmarkDao.isBookmarkedFlow(postId, email)
    }

    fun getComments(postId: Int): Flow<List<CommentEntity>> {
        return commentDao.getCommentsForPostFlow(postId)
    }

    fun addComment(postId: Int, content: String) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            if (content.isBlank()) return@launch

            val user = userDao.getUserByEmail(email) ?: return@launch
            val post = postDao.getPostById(postId) ?: return@launch

            val comment = CommentEntity(
                postId = postId,
                userEmail = email,
                username = user.username,
                content = content
            )

            commentDao.insertComment(comment)
            postDao.updateCommentsCount(postId, 1)

            // Notify Author
            if (post.authorEmail != email) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        userEmail = post.authorEmail,
                        type = "COMMENT",
                        title = "New Reply",
                        content = "${user.username} commented on \"${post.title}\"."
                    )
                )
            }
        }
    }

    fun deleteComment(post: PostEntity, comment: CommentEntity) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val user = userDao.getUserByEmail(email) ?: return@launch

            if (user.isFounder || comment.userEmail == email) {
                commentDao.deleteComment(comment.id)
                postDao.updateCommentsCount(post.id, -1)
                _messageEvent.emit("Comment purged.")
            } else {
                _messageEvent.emit("Unauthorized comment deletion blocked.")
            }
        }
    }

    fun togglePin(post: PostEntity) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val user = userDao.getUserByEmail(email) ?: return@launch

            if (user.isFounder) {
                val newStatus = !post.isPinned
                postDao.updatePinnedStatus(post.id, newStatus)
                _messageEvent.emit(if (newStatus) "Story pinned to transmissions top." else "Story unpinned.")
            } else {
                _messageEvent.emit("Only Founder/Admin can pin transmissions.")
            }
        }
    }

    // --- Abuse reporting ---

    fun reportPost(post: PostEntity) {
        viewModelScope.launch {
            _reportedPostIds.value = _reportedPostIds.value + post.id
            _messageEvent.emit("Abuse report submitted. Founder review pending.")

            // Notify Founder Admin of report
            notificationDao.insertNotification(
                NotificationEntity(
                    userEmail = "ahambrahmasmi415@gmail.com",
                    type = "MENTION",
                    title = "Content Report",
                    content = "Post ID ${post.id} (\"${post.title}\") was reported by a member."
                )
            )
        }
    }

    // --- Stacy AI Dialogs ---

    fun sendStacyMessage(text: String) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            if (text.isBlank()) return@launch

            // Insert User Message
            val userChat = ChatHistoryEntity(userEmail = email, message = text, isUser = true)
            chatHistoryDao.insertChat(userChat)

            _isAILoading.value = true

            // Form system prompt for Sci-fi Stacy AI
            val systemPrompt = """
                You are Stacy AI, the premium, futuristic creative partner for the StacyVerse platform.
                You help writers with:
                - Story writing, Screenplay writing, and Script editing.
                - Character development, Plot twists, and World building.
                - Marketing ideas, branding, copywriting, creative writing, and content writing.
                
                Guidelines:
                - Maintain a professional, supportive, and creative tone.
                - Generate well-structured, comprehensive, detailed, and context-aware responses.
                - Format code or scripts beautifully when requested. Use clear Markdown headings and bullet points.
                - Keep the creative integrity high. Speak with sci-fi flair where appropriate!
            """.trimIndent()

            // Ask Stacy API
            val aiResponse = GeminiApiClient.askStacy(text, systemPrompt)

            // Insert AI Response
            val aiChat = ChatHistoryEntity(userEmail = email, message = aiResponse, isUser = false)
            chatHistoryDao.insertChat(aiChat)

            _isAILoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            chatHistoryDao.clearChatHistory(email)
            _messageEvent.emit("AI interface neural cache cleared.")
        }
    }

    fun regenerateStacyAnswer() {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val history = chatHistory.value
            if (history.isEmpty()) return@launch

            // Find last user message
            val lastUserMessage = history.lastOrNull { it.isUser }?.message ?: return@launch

            // Clear last AI message if there is one
            _isAILoading.value = true

            val aiResponse = GeminiApiClient.askStacy(lastUserMessage, "You are Stacy AI, sci-fi companion. Help the writer creatively.")
            chatHistoryDao.insertChat(ChatHistoryEntity(userEmail = email, message = aiResponse, isUser = false))

            _isAILoading.value = false
        }
    }

    // --- Admin Operations ---

    fun toggleSuspendUser(targetUserEmail: String) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val admin = userDao.getUserByEmail(email) ?: return@launch

            if (admin.isFounder) {
                val target = userDao.getUserByEmail(targetUserEmail) ?: return@launch
                if (target.isFounder) {
                    _messageEvent.emit("Founder cannot suspend themselves.")
                    return@launch
                }
                val nextStatus = !target.isSuspended
                userDao.setUserSuspendedStatus(targetUserEmail, nextStatus)
                _messageEvent.emit("User suspension status synchronized to: $nextStatus")
            } else {
                _messageEvent.emit("Unauthorized founder access required.")
            }
        }
    }

    fun dismissReport(postId: Int) {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            val admin = userDao.getUserByEmail(email) ?: return@launch

            if (admin.isFounder) {
                _reportedPostIds.value = _reportedPostIds.value - postId
                _messageEvent.emit("Report dismissed.")
            }
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            val email = _currentUserEmail.value ?: return@launch
            notificationDao.markAllRead(email)
        }
    }
}
