package com.pocketclone.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketclone.app.data.api.Article
import com.pocketclone.app.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val articles: StateFlow<List<Article>> = combine(
        _showArchived,
        _searchQuery
    ) { archived, query ->
        Pair(archived, query)
    }.flatMapLatest { (archived, query) ->
        if (query.isNotEmpty()) {
            repository.searchLocal(query)
        } else {
            repository.getArticles(archived)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refresh()
    }

    fun setShowArchived(archived: Boolean) {
        _showArchived.value = archived
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshArticles()
            _isRefreshing.value = false
        }
    }

    fun archiveArticle(id: Long) {
        viewModelScope.launch {
            repository.archiveArticle(id, true)
        }
    }

    fun unarchiveArticle(id: Long) {
        viewModelScope.launch {
            repository.archiveArticle(id, false)
        }
    }

    fun deleteArticle(id: Long) {
        viewModelScope.launch {
            repository.deleteArticle(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onSearch = { showSearch = false },
                    active = true,
                    onActiveChange = { if (!it) showSearch = false },
                    placeholder = { Text("Search articles...") },
                    leadingIcon = {
                        IconButton(onClick = { showSearch = false; viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Search suggestions could go here
                }
            } else {
                TopAppBar(
                    title = { Text(if (showArchived) "Archive" else "Pocket Clone") },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Article, "Unread") },
                    label = { Text("Unread") },
                    selected = !showArchived,
                    onClick = { viewModel.setShowArchived(false) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Archive, "Archive") },
                    label = { Text("Archive") },
                    selected = showArchived,
                    onClick = { viewModel.setShowArchived(true) }
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (articles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (showArchived) Icons.Default.Archive else Icons.Default.Article,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No results found"
                                   else if (showArchived) "No archived articles"
                                   else "No articles saved",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = articles,
                        key = { it.id }
                    ) { article ->
                        SwipeableArticleCard(
                            article = article,
                            onClick = { onArticleClick(article.id) },
                            onArchive = {
                                if (showArchived) viewModel.unarchiveArticle(article.id)
                                else viewModel.archiveArticle(article.id)
                            },
                            onDelete = { viewModel.deleteArticle(article.id) },
                            isArchived = showArchived
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableArticleCard(
    article: Article,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    isArchived: Boolean,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onArchive()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.surface
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Archive
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
            }
        }
    ) {
        com.pocketclone.app.ui.components.ArticleCard(
            article = article,
            onClick = onClick
        )
    }
}
