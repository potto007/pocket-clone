package com.pocketclone.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pocketclone.app.data.api.Article

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: Long,
    val url: String,
    val title: String,
    val excerpt: String?,
    val content: String?,
    val author: String?,
    val image: String?,
    val archived: Boolean,
    val createdAt: String?
) {
    fun toArticle(): Article = Article(
        id = id,
        url = url,
        title = title,
        excerpt = excerpt,
        content = content,
        author = author,
        image = image,
        archived = archived,
        createdAt = createdAt
    )

    companion object {
        fun fromArticle(article: Article): ArticleEntity = ArticleEntity(
            id = article.id,
            url = article.url,
            title = article.title,
            excerpt = article.excerpt,
            content = article.content,
            author = article.author,
            image = article.image,
            archived = article.archived,
            createdAt = article.createdAt
        )
    }
}
