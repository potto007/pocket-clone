package com.pocketclone.app.data.api

import com.google.gson.annotations.SerializedName

data class Article(
    @SerializedName("id") val id: Long,
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("excerpt") val excerpt: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("author") val author: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("archived") val archived: Boolean,
    @SerializedName("created_at") val createdAt: String?
)

data class CreateArticleRequest(
    @SerializedName("url") val url: String
)

data class UpdateArticleRequest(
    @SerializedName("archived") val archived: Boolean? = null
)
