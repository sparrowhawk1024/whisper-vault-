package com.whispervault.di

import android.content.Context
import androidx.room.Room
import com.whispervault.BuildConfig
import com.whispervault.data.local.WhisperVaultDatabase
import com.whispervault.data.remote.WhisperVaultApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): WhisperVaultApi = retrofit.create(WhisperVaultApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WhisperVaultDatabase {
        return Room.databaseBuilder(context, WhisperVaultDatabase::class.java, "whispervault.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSavedContentDao(db: WhisperVaultDatabase) = db.savedContentDao()

    @Provides
    fun provideConnectionDao(db: WhisperVaultDatabase) = db.connectionDao()

    @Provides
    fun provideAppPrefDao(db: WhisperVaultDatabase) = db.appPrefDao()
}
