package com.pocketclone.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.pocketclone.app.data.repository.ArticleRepository
import com.pocketclone.app.ui.navigation.NavGraph
import com.pocketclone.app.ui.theme.PocketCloneTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: ArticleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle share intent
        handleIntent(intent)

        setContent {
            PocketCloneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                saveArticle(extractUrl(sharedText))
            }
        }
    }

    private fun extractUrl(text: String): String {
        // Try to extract URL from shared text
        val urlPattern = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        val match = urlPattern.find(text)
        return match?.value ?: text
    }

    private fun saveArticle(url: String) {
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Saving article...", Toast.LENGTH_SHORT).show()

            val result = repository.saveArticle(url)

            result.fold(
                onSuccess = { article ->
                    Toast.makeText(
                        this@MainActivity,
                        "Saved: ${article.title}",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@MainActivity,
                        "Failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}
