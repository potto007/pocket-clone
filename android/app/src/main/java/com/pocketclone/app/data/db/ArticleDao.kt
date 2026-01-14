package com.pocketclone.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles WHERE archived = :archived ORDER BY id DESC")
    fun getArticles(archived: Boolean): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY id DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticle(id: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET archived = :archived WHERE id = :id")
    suspend fun updateArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteArticle(id: Long)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR excerpt LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<ArticleEntity>>
}
