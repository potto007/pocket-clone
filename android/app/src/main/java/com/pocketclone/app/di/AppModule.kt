package com.pocketclone.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.pocketclone.app.data.api.PocketApi
import com.pocketclone.app.data.db.AppDatabase
import com.pocketclone.app.data.db.ArticleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(dataStore: DataStore<Preferences>): OkHttpClient {
        val dynamicBaseUrlInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // Get current server URL from DataStore
            val serverUrl = runBlocking {
                val prefs = dataStore.data.first()
                prefs[SettingsKeys.SERVER_URL] ?: "http://10.0.2.2:8080"
            }.removeSuffix("/")

            val newUrl = serverUrl.toHttpUrlOrNull()
            if (newUrl != null) {
                val newRequest = originalRequest.newBuilder()
                    .url(
                        originalRequest.url.newBuilder()
                            .scheme(newUrl.scheme)
                            .host(newUrl.host)
                            .port(newUrl.port)
                            .build()
                    )
                    .build()
                chain.proceed(newRequest)
            } else {
                chain.proceed(originalRequest)
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // Placeholder base URL - the interceptor will override it
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePocketApi(retrofit: Retrofit): PocketApi {
        return retrofit.create(PocketApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pocket_clone_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideArticleDao(database: AppDatabase): ArticleDao {
        return database.articleDao()
    }
}

object SettingsKeys {
    val SERVER_URL = androidx.datastore.preferences.core.stringPreferencesKey("server_url")
}
