package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.AppDatabase
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StacyVerseTheme {
                val viewModel: MainViewModel = viewModel()
                val context = LocalContext.current

                // Collect shared messages
                LaunchedEffect(Unit) {
                    viewModel.messageEvent.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepSpaceBlack
                ) {
                    AppContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppContent(viewModel: MainViewModel) {
    val introFinished by viewModel.introFinished.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Crossfade(targetState = Pair(introFinished, currentUser), label = "main_flow") { (finished, user) ->
        when {
            !finished -> {
                CinematicIntroScreen(onFinished = { viewModel.setIntroFinished() })
            }
            user == null -> {
                AuthScreen(viewModel)
            }
            else -> {
                DashboardScreen(viewModel, user)
            }
        }
    }
}

// --- CORE FEATURE 1: CINEMATIC STARTUP INTRO ---
@Composable
fun CinematicIntroScreen(onFinished: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // Particle animations
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "particle_motion"
    )

    // Fade-in animations
    val logoAlpha by animateFloatAsState(
        targetValue = if (step >= 2) 1f else 0f,
        animationSpec = tween(1500, easing = EaseInOutCirc), label = "logo_fade"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (step >= 3) 1f else 0f,
        animationSpec = tween(1200, easing = EaseInOutSine), label = "text_fade"
    )
    val zoomScale by animateFloatAsState(
        targetValue = if (step >= 2) 1.08f else 0.92f,
        animationSpec = tween(4000, easing = EaseOutQuad), label = "zoom_effect"
    )

    LaunchedEffect(Unit) {
        // Step 1: Black screen with ambient light beginning
        delay(800)
        step = 2 // Step 2: Particles converge & OG Studios logo fades in
        delay(1500)
        step = 3 // Step 3: "An OG Studios Production" fades in with shimmer
        delay(2200)
        onFinished() // Auto-transition to main screen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBlack)
            .testTag("cinematic_intro_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Converging particles drawing on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2

            // Draw 40 converging particles
            for (i in 0..40) {
                val seed = i * 293.4f
                val randomAngle = (seed % 360) * (Math.PI / 180f)
                val initialRadius = 450f + (seed % 200)

                // Particles slowly converge to center as particleProgress increases
                val currentRadius = initialRadius * (1f - particleProgress)
                val x = centerX + Math.cos(randomAngle).toFloat() * currentRadius
                val y = centerY + Math.sin(randomAngle).toFloat() * currentRadius

                val color = if (i % 2 == 0) ElectricBlue else CyberPurple
                val radius = 4.dp.toPx() * (1f - currentRadius / initialRadius)

                drawCircle(
                    color = color,
                    radius = radius.coerceAtLeast(1f),
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    alpha = (1f - particleProgress) * 0.8f
                )
            }
        }

        // Shimmering ambient background glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .alpha(0.12f)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricBlue, CyberPurple, Color.Transparent)
                    )
                )
        )

        // Logo and text container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(zoomScale)
                .graphicsLayer(alpha = logoAlpha)
        ) {
            // Metallic glowing sphere representing OG Studios logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ElectricBlue, CyberPurple)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OG",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OG STUDIOS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "An OG Studios Production",
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(textAlpha)
            )
        }

        // Skip button in top right
        Button(
            onClick = onFinished,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(16.dp)
                .testTag("skip_intro_button")
        ) {
            Text(
                text = "Skip",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- SECURE AUTHENTICATION FLOW ---
@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var isLoginMode by remember { mutableStateOf(true) }

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBlack)
    ) {
        // Glowing background elements
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .align(Alignment.TopCenter)
                .alpha(0.18f)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberPurple, DeepNavy, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Heading with branding
            Text(
                text = "STACYVERSE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                color = ElectricBlue,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "PREMIUM SCRIPT & SCI-FI STORIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = CyberPurple,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Glassmorphic Card containing input fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val title = if (isLoginMode) "Sign In with Google" else "Create Writer Profile"

                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Email input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("yourname@gmail.com") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = ElectricBlue,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isLoginMode) {
                        // Username input
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Writer Name / Username") },
                            placeholder = { Text("Pen name or real name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = GlassBorder,
                                focusedLabelColor = ElectricBlue,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = TextMuted,
                                unfocusedPlaceholderColor = TextMuted
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bio input
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Short Writer Bio") },
                            placeholder = { Text("Tell the community about your sci-fi interests") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = GlassBorder,
                                focusedLabelColor = ElectricBlue,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = TextMuted,
                                unfocusedPlaceholderColor = TextMuted
                            ),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Google Sign-In Action Button
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                viewModel.login(email, "google-oauth") { success ->
                                    if (success) {
                                        email = ""
                                    }
                                }
                            } else {
                                viewModel.signup(email, username, bio) { success ->
                                    if (success) {
                                        email = ""; username = ""; bio = ""
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_auth_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricBlue, CyberPurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = "Google Logo Simulation",
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                                )
                                Text(
                                    text = if (isLoginMode) "CONTINUE WITH GOOGLE" else "SIGN UP WITH GOOGLE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mode toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoginMode) "New to StacyVerse?" else "Already have an account?",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        TextButton(
                            onClick = {
                                isLoginMode = !isLoginMode
                            }
                        ) {
                            Text(
                                text = if (isLoginMode) "Create an Account" else "Sign In",
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Demo disclaimer matching prompt: "No dead-end UI affordances"
            Text(
                text = "Note: Signing in with Google connects you instantly. If logging in for the first time, a writer profile will be automatically created.",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// --- MAIN APPLICATION SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, user: UserEntity) {
    val activeTab by viewModel.activeTab.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifications = notifications.any { !it.isRead }

    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepSpaceBlack,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(ElectricBlue, CyberPurple)))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(DeepSpaceBlack)
                                ) {
                                    Text(
                                        "S",
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "STACYVERSE",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    },
                    actions = {
                        // If user is founder, show an elegant Admin toggle
                        if (user.isFounder) {
                            IconButton(onClick = { showAdminPanel = true }) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Console",
                                    tint = CyberPurple
                                )
                            }
                        }

                        // Notification alert
                        IconButton(onClick = { viewModel.selectTab("notifications") }) {
                            Box {
                                Icon(
                                    if (activeTab == "notifications") Icons.Default.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (activeTab == "notifications") ElectricBlue else Color.White
                                )
                                if (unreadNotifications) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(DangerNeon)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSpaceBlack)
                )
                HorizontalDivider(color = GlassBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = GlassBorder, thickness = 1.dp)
                NavigationBar(
                    containerColor = DeepSpaceBlack,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val tabs = listOf(
                        Triple("home", "Home", Icons.Default.Home),
                        Triple("explore", "Explore", Icons.Default.Search),
                        Triple("stacy_ai", "Stacy AI", Icons.Default.AutoAwesome),
                        Triple("profile", "Profile", Icons.Default.Person)
                    )

                    tabs.forEach { (tabId, label, icon) ->
                        val selected = activeTab == tabId
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tabId) },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (selected) ElectricBlue else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    color = if (selected) ElectricBlue else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab == "home") {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = ElectricBlue,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("create_post_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post Story")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                }, label = "tab_transition"
            ) { tab ->
                when (tab) {
                    "home" -> HomeScreen(viewModel, user)
                    "explore" -> ExploreScreen(viewModel)
                    "stacy_ai" -> StacyAIScreen(viewModel)
                    "profile" -> ProfileScreen(viewModel, user)
                    "notifications" -> NotificationsScreen(viewModel)
                    else -> HomeScreen(viewModel, user)
                }
            }
        }
    }

    // Modal Sheet for Post Creation
    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onSubmit = { title, body, imgUrl ->
                viewModel.createPost(title, body, imgUrl)
                showCreatePostDialog = false
            }
        )
    }

    // Admin Panel BottomSheet / Dialog
    if (showAdminPanel && user.isFounder) {
        AdminPanelDialog(
            viewModel = viewModel,
            onDismiss = { showAdminPanel = false }
        )
    }
}

// --- TABS IMPLEMENTATIONS ---

// 1. HOME TAB: COMMUNITY FEED
@Composable
fun HomeScreen(viewModel: MainViewModel, user: UserEntity) {
    val posts by viewModel.allPosts.collectAsState()
    val reportedPostIds by viewModel.reportedPostIds.collectAsState()

    // Filter out locally reported posts for standard users
    val visiblePosts = if (user.isFounder) {
        posts // Founder sees everything to moderate
    } else {
        posts.filter { !reportedPostIds.contains(it.id) }
    }

    if (visiblePosts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = "Empty",
                    tint = TextMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "The story feed is empty. Be the first to post!",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = rememberLazyListState()
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(visiblePosts, key = { it.id }) { post ->
                PostCard(post = post, viewModel = viewModel, user = user)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PostCard(post: PostEntity, viewModel: MainViewModel, user: UserEntity) {
    val isLiked by viewModel.isPostLiked(post.id).collectAsState(initial = false)
    val isBookmarked by viewModel.isPostBookmarked(post.id).collectAsState(initial = false)
    val reportedPostIds by viewModel.reportedPostIds.collectAsState()
    val isReported = reportedPostIds.contains(post.id)

    var showComments by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (post.isPinned) DeepNavy.copy(alpha = 0.5f) else GlassSurface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (post.isPinned) ElectricBlue.copy(alpha = 0.4f) else GlassBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Profile, Name, Pinned, and Options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = post.authorPhotoUrl,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassSurface)
                        .border(1.dp, ElectricBlue.copy(alpha = 0.5f), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        if (post.authorEmail == "ahambrahmasmi415@gmail.com") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Founder",
                                tint = ElectricBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "FOUNDER",
                                color = ElectricBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    val formattedTime = remember(post.timestamp) {
                        val diff = System.currentTimeMillis() - post.timestamp
                        when {
                            diff < 60000 -> "Just now"
                            diff < 3600000 -> "${diff / 60000}m ago"
                            diff < 86400000 -> "${diff / 3600000}h ago"
                            else -> "${diff / 86400000}d ago"
                        }
                    }
                    Text(
                        text = formattedTime,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                if (post.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned Story",
                        tint = ElectricBlue,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                }

                // Delete Action or Flag Action
                if (user.isFounder) {
                    // Admin can toggle Pin and Delete Any post
                    Row {
                        IconButton(onClick = { viewModel.togglePin(post) }) {
                            Icon(
                                if (post.isPinned) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "Toggle Pin",
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.deletePost(post) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Admin Delete",
                                tint = DangerNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Row {
                        if (post.authorEmail == user.email) {
                            IconButton(onClick = { viewModel.deletePost(post) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Post",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.reportPost(post) }) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = "Report post",
                                    tint = if (isReported) DangerNeon else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Text(
                text = post.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Content
            Text(
                text = post.content,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Attached image if exists
            if (!post.imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = post.imageUri,
                    contentDescription = "Story Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f), thickness = 1.dp)

            // Bottom Buttons (Like, Comment, Bookmark, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.toggleLike(post) }
                ) {
                    IconButton(onClick = { viewModel.toggleLike(post) }) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Button",
                            tint = if (isLiked) DangerNeon else Color.White
                        )
                    }
                    Text(
                        text = post.likesCount.toString(),
                        color = if (isLiked) DangerNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Comment Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showComments = !showComments }
                ) {
                    IconButton(onClick = { showComments = !showComments }) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = post.commentsCount.toString(),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Bookmark Button
                IconButton(onClick = { viewModel.toggleBookmark(post) }) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) ElectricBlue else Color.White
                    )
                }

                // Share Button (Mimic copy to clipboard)
                val context = LocalContext.current
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("StacyVerse Story Link", "https://stacyverse.com/post/${post.id}")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Transmitting content link cloned to bio-clipboard.", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }

            // Expandable Comments section
            AnimatedVisibility(visible = showComments) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    CommentsSection(post = post, viewModel = viewModel, user = user)
                }
            }
        }
    }
}

// COMMENTS EXPANDABLE SUBSECTION
@Composable
fun CommentsSection(post: PostEntity, viewModel: MainViewModel, user: UserEntity) {
    val comments by viewModel.getComments(post.id).collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }

    Column {
        if (comments.isEmpty()) {
            Text(
                "No comments. Wake up the conversation in this sector.",
                color = TextMuted,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            comments.forEach { comment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = comment.username,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            fontSize = 12.sp
                        )
                        Text(
                            text = comment.content,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Admin or Comment Owner can delete comments
                    if (user.isFounder || comment.userEmail == user.email) {
                        IconButton(
                            onClick = { viewModel.deleteComment(post, comment) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete comment",
                                tint = DangerNeon,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Comment input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Write a reply...", color = TextMuted, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        viewModel.addComment(post.id, commentText)
                        commentText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .width(50.dp)
                    .height(46.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// 2. EXPLORE TAB
@Composable
fun ExploreScreen(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val posts by viewModel.searchedPosts.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()

    val genres = listOf("All", "Cyberpunk", "SpaceOpera", "Thriller", "HardSciFi", "AI")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search stories, genres, authors...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Genre filter pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) ElectricBlue else GlassSurface)
                        .border(1.dp, if (isSelected) ElectricBlue else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { viewModel.setGenreFilter(genre) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "#$genre",
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (searchQuery.isNotEmpty() || selectedGenre != "All") "Search Results" else "Trending Today",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transmissions matched your query in this sector.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSearchQuery(post.title)
                            },
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = "Book",
                                tint = CyberPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    post.title,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "by ${post.authorName}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🔥 ${post.likesCount}",
                                color = ElectricBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. CORE FEATURE 2: STACY AI TAB
@Composable
fun StacyAIScreen(viewModel: MainViewModel) {
    val chatMessages by viewModel.chatHistory.collectAsState()
    val isAILoading by viewModel.isAILoading.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom on new messages
    LaunchedEffect(chatMessages.size, isAILoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // AI Status header with glowing gradient card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(ElectricBlue, CyberPurple)))
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Stacy",
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Stacy AI Story Architect",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isAILoading) "Calculating creative branch..." else "Connected / Online",
                        color = if (isAILoading) CyberPurple else ElectricBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Clean-up Neural Cache button
                IconButton(onClick = { viewModel.clearChatHistory() }) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clear Cache", tint = TextMuted)
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Greetings, Pilot Scribe.",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "I am Stacy. Let us construct worlds, formulate dramatic twists, or optimize scripts. What sector shall we explore?",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(chatMessages) { chat ->
                    val isUser = chat.isUser
                    val backgroundBrush = if (isUser) {
                        Brush.linearGradient(colors = listOf(CyberPurpleDim, CyberPurple))
                    } else {
                        Brush.linearGradient(colors = listOf(GlassSurface, DeepNavy))
                    }
                    val borderColor = if (isUser) CyberPurple.copy(alpha = 0.5f) else ElectricBlue.copy(alpha = 0.2f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .graphicsLayer(alpha = 0.95f),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            border = BorderStroke(1.dp, borderColor),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(backgroundBrush)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = chat.message,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )

                                    if (!isUser) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = GlassBorder.copy(alpha = 0.3f))
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Copy answer action
                                            val context = LocalContext.current
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Stacy Response", chat.message)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Transmitted intelligence copied.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = ElectricBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Regenerate action
                                            IconButton(
                                                onClick = { viewModel.regenerateStacyAnswer() },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Regenerate",
                                                    tint = CyberPurple,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isAILoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GlassSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Stacy is thinking",
                                    color = ElectricBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    color = ElectricBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Stacy for plot twists, script edit...", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendStacyMessage(textInput)
                        textInput = ""
                    }
                },
                enabled = !isAILoading,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_button")
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }
    }
}

// 4. USER PROFILE TAB
@Composable
fun ProfileScreen(viewModel: MainViewModel, user: UserEntity) {
    val myPosts by postDaoFlow(viewModel, user.email).collectAsState(initial = emptyList<PostEntity>())
    val savedPosts by viewModel.bookmarkedPosts.collectAsState()

    var activeProfileTab by remember { mutableStateOf("posts") }
    var isEditProfileOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // User Meta
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = user.profilePhotoUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, ElectricBlue, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    user.username,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 20.sp
                )
                Text(
                    user.email,
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { isEditProfileOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Edit Bio Signature", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bio
        Text(
            text = user.bio,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Analytics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(myPosts.count().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Posts", color = TextSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(user.followersCount.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Followers", color = TextSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(user.followingCount.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Following", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs Toggle (Posts vs Saved)
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { activeProfileTab = "posts" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeProfileTab == "posts") ElectricBlue else GlassSurface
                ),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("My Stories", color = if (activeProfileTab == "posts") Color.Black else Color.White)
            }
            Button(
                onClick = { activeProfileTab = "saved" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeProfileTab == "saved") ElectricBlue else GlassSurface
                ),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Saved/Drafts", color = if (activeProfileTab == "saved") Color.Black else Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentList = if (activeProfileTab == "posts") myPosts else savedPosts

        if (currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No stories in this segment.", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                currentList.forEach { post ->
                    PostCard(post = post, viewModel = viewModel, user = user)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Logout Button
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = DangerNeon.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, DangerNeon),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Text("DISCONNECT NEURAL SESSION (LOGOUT)", color = DangerNeon, fontWeight = FontWeight.Bold)
        }
    }

    if (isEditProfileOpen) {
        EditProfileDialog(
            user = user,
            onDismiss = { isEditProfileOpen = false },
            onSubmit = { username, bio, photo ->
                viewModel.updateProfile(username, bio, photo)
                isEditProfileOpen = false
            }
        )
    }
}

@Composable
fun postDaoFlow(viewModel: MainViewModel, email: String): Flow<List<PostEntity>> {
    val db = AppDatabase.getDatabase(LocalContext.current)
    return db.postDao().getPostsByAuthorFlow(email)
}

// 5. NOTIFICATIONS TAB
@Composable
fun NotificationsScreen(viewModel: MainViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.markNotificationsRead()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Telemetry Log Updates",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "All sectors quiet. No notification signals received.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { notification ->
                    val color = when (notification.type) {
                        "LIKE" -> DangerNeon
                        "COMMENT" -> ElectricBlue
                        else -> CyberPurple
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (notification.type) {
                                        "LIKE" -> Icons.Default.Favorite
                                        "COMMENT" -> Icons.AutoMirrored.Filled.Comment
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = "Notification type",
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    notification.title,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    notification.content,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- FLOATING POPUP / DIALOG HELPERS ---

@Composable
fun CreatePostDialog(onDismiss: () -> Unit, onSubmit: (String, String, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transmit Original Story/Script", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Story or Screenplay Title", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Unleash the words here... Use tags like #Cyberpunk #AI to categorize.", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    placeholder = { Text("Attached Image Link (Unsplash/Web)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, body, imageUrl.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Transmit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abort", color = TextSecondary)
            }
        },
        containerColor = DeepNavy,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EditProfileDialog(user: UserEntity, onDismiss: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var username by remember { mutableStateOf(user.username) }
    var bio by remember { mutableStateOf(user.bio) }
    var photoUrl by remember { mutableStateOf(user.profilePhotoUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Identity Matrix", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Pseudonym") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio Signature") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("Avatar Link") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(username, bio, photoUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Commit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DeepNavy,
        shape = RoundedCornerShape(20.dp)
    )
}

// --- CORE FEATURE 7: FOUNDER/ADMIN SYSTEM ---
@Composable
fun AdminPanelDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val users by viewModel.allUsers.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val reportedPostIds by viewModel.reportedPostIds.collectAsState()

    var activeAdminTab by remember { mutableStateOf("analytics") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = CyberPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("FOUNDER DASHBOARD", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                // Admin Tabs
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    TextButton(
                        onClick = { activeAdminTab = "analytics" },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (activeAdminTab == "analytics") ElectricBlue else TextSecondary)
                    ) {
                        Text("Analytics")
                    }
                    TextButton(
                        onClick = { activeAdminTab = "moderation" },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (activeAdminTab == "moderation") ElectricBlue else TextSecondary)
                    ) {
                        Text("Moderation (${reportedPostIds.count()})")
                    }
                    TextButton(
                        onClick = { activeAdminTab = "users" },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (activeAdminTab == "users") ElectricBlue else TextSecondary)
                    ) {
                        Text("Scribes")
                    }
                }

                HorizontalDivider(color = GlassBorder)

                Spacer(modifier = Modifier.height(12.dp))

                when (activeAdminTab) {
                    "analytics" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Platform Telemetry Stats", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Card(colors = CardDefaults.cardColors(containerColor = GlassSurface), modifier = Modifier.weight(1f).padding(4.dp)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(users.size.toString(), color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Total Users", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                Card(colors = CardDefaults.cardColors(containerColor = GlassSurface), modifier = Modifier.weight(1f).padding(4.dp)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(posts.size.toString(), color = CyberPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Total Posts", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Card(colors = CardDefaults.cardColors(containerColor = GlassSurface), modifier = Modifier.weight(1f).padding(4.dp)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        val totalLikes = posts.sumOf { it.likesCount }
                                        Text(totalLikes.toString(), color = DangerNeon, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Total Likes", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                Card(colors = CardDefaults.cardColors(containerColor = GlassSurface), modifier = Modifier.weight(1f).padding(4.dp)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(reportedPostIds.size.toString(), color = WarningNeon, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Reports Pending", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    "moderation" -> {
                        if (reportedPostIds.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No reported content signals. Platform clean.", color = TextMuted, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val reportedPosts = posts.filter { reportedPostIds.contains(it.id) }
                                items(reportedPosts) { post ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                                        border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Reported Story: \"${post.title}\"", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("by ${post.authorName}", color = TextSecondary, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                Button(
                                                    onClick = { viewModel.dismissReport(post.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("Dismiss", color = Color.White, fontSize = 10.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { viewModel.deletePost(post) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DangerNeon),
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("Purge Post", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "users" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(users) { u ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(GlassSurface)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(u.email, color = TextMuted, fontSize = 11.sp)
                                    }

                                    if (!u.isFounder) {
                                        Switch(
                                            checked = u.isSuspended,
                                            onCheckedChange = { viewModel.toggleSuspendUser(u.email) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = DangerNeon,
                                                checkedTrackColor = DangerNeon.copy(alpha = 0.5f)
                                            )
                                        )
                                    } else {
                                        Text("Founder", color = ElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)) {
                Text("Close Telemetry", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DeepSpaceBlack,
        shape = RoundedCornerShape(24.dp)
    )
}
