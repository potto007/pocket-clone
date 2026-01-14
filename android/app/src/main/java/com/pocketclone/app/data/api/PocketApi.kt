package com.pocketclone.app.data.api

import retrofit2.http.*

interface PocketApi {

    @POST("api/articles")
    suspend fun createArticle(@Body request: CreateArticleRequest): Article

    @GET("api/articles")
    suspend fun getArticles(
        @Query("archived") archived: Boolean? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<Article>

    @GET("api/articles/{id}")
    suspend fun getArticle(@Path("id") id: Long): Article

    @PATCH("api/articles/{id}")
    suspend fun updateArticle(
        @Path("id") id: Long,
        @Body request: UpdateArticleRequest
    )

    @DELETE("api/articles/{id}")
    suspend fun deleteArticle(@Path("id") id: Long)

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): List<Article>
}
