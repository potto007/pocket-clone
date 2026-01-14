package com.pocketclone.app.ui.screens

import android.text.Html
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.pocketclone.app.data.api.Article
import com.pocketclone.app.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ArticleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val articleId: Long = savedStateHandle["articleId"] ?: 0L

    private val _article = MutableStateFlow<Article?>(null)
    val article: StateFlow<Article?> = _article

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _textSize = MutableStateFlow(16)
    val textSize: StateFlow<Int> = _textSize

    init {
        loadArticle()
    }

    private fun loadArticle() {
        viewModelScope.launch {
            _isLoading.value = true
            _article.value = repository.getArticle(articleId)
            _isLoading.value = false
        }
    }

    fun increaseTextSize() {
        if (_textSize.value < 24) _textSize.value += 2
    }

    fun decreaseTextSize() {
        if (_textSize.value > 12) _textSize.value -= 2
    }

    fun archiveArticle() {
        viewModelScope.launch {
            _article.value?.let { article ->
                repository.archiveArticle(article.id, !article.archived)
                _article.value = article.copy(archived = !article.archived)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val uriHandler = LocalUriHandler.current
    var showTextSizeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = article?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTextSizeMenu = true }) {
                        Icon(Icons.Default.TextFormat, "Text size")
                    }
                    DropdownMenu(
                        expanded = showTextSizeMenu,
                        onDismissRequest = { showTextSizeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Increase text size") },
                            onClick = { viewModel.increaseTextSize() },
                            leadingIcon = { Icon(Icons.Default.Add, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Decrease text size") },
                            onClick = { viewModel.decreaseTextSize() },
                            leadingIcon = { Icon(Icons.Default.Remove, null) }
                        )
                    }

                    article?.let { art ->
                        IconButton(onClick = { viewModel.archiveArticle() }) {
                            Icon(
                                if (art.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                                if (art.archived) "Unarchive" else "Archive"
                            )
                        }
                        IconButton(onClick = { uriHandler.openUri(art.url) }) {
                            Icon(Icons.Default.OpenInBrowser, "Open in browser")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (article == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Article not found")
            }
        } else {
            article?.let { art ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero image
                    if (!art.image.isNullOrEmpty()) {
                        AsyncImage(
                            model = art.image,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title
                        Text(
                            text = art.title,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        // Author
                        if (!art.author.isNullOrEmpty()) {
                            Text(
                                text = "By ${art.author}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Divider()

                        // Content
                        val htmlContent = art.content ?: art.excerpt ?: ""
                        val plainText = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT).toString()

                        Text(
                            text = plainText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textSize.sp,
                                lineHeight = (textSize * 1.6).sp
                            )
                        )
                    }
                }
            }
        }
    }
}
