package com.pocketclone.app.data.repository

import com.pocketclone.app.data.api.Article
import com.pocketclone.app.data.api.CreateArticleRequest
import com.pocketclone.app.data.api.PocketApi
import com.pocketclone.app.data.api.UpdateArticleRequest
import com.pocketclone.app.data.db.ArticleDao
import com.pocketclone.app.data.db.ArticleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val api: PocketApi,
    private val dao: ArticleDao
) {
    fun getArticles(archived: Boolean): Flow<List<Article>> {
        return dao.getArticles(archived).map { entities ->
            entities.map { it.toArticle() }
        }
    }

    fun searchLocal(query: String): Flow<List<Article>> {
        return dao.search(query).map { entities ->
            entities.map { it.toArticle() }
        }
    }

    suspend fun getArticle(id: Long): Article? {
        // Fetch full article from API (includes content)
        return try {
            val article = api.getArticle(id)
            dao.insertArticle(ArticleEntity.fromArticle(article))
            article
        } catch (e: Exception) {
            // Fallback to cached version
            dao.getArticle(id)?.toArticle()
        }
    }

    suspend fun refreshArticles(archived: Boolean? = null) {
        try {
            val articles = api.getArticles(archived = archived)
            dao.insertArticles(articles.map { ArticleEntity.fromArticle(it) })
        } catch (e: Exception) {
            // Ignore network errors, use cached data
        }
    }

    suspend fun saveArticle(url: String): Result<Article> {
        return try {
            val article = api.createArticle(CreateArticleRequest(url))
            dao.insertArticle(ArticleEntity.fromArticle(article))
            Result.success(article)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveArticle(id: Long, archived: Boolean): Result<Unit> {
        return try {
            api.updateArticle(id, UpdateArticleRequest(archived = archived))
            dao.updateArchived(id, archived)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteArticle(id: Long): Result<Unit> {
        return try {
            api.deleteArticle(id)
            dao.deleteArticle(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCache() {
        dao.deleteAll()
    }
}
