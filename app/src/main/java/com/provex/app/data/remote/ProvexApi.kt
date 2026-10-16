package com.provex.app.data.remote

import com.provex.app.BuildConfig
import com.provex.app.data.remote.dto.AnalysisDto
import com.provex.app.data.remote.dto.AuthResponseDto
import com.provex.app.data.remote.dto.CredentialsDto
import com.provex.app.data.remote.dto.EvidenceResponseDto
import com.provex.app.data.remote.dto.GithubAuthorizationDto
import com.provex.app.data.remote.dto.JobSubmissionDto
import com.provex.app.data.remote.dto.RecommendationDto
import com.provex.app.data.remote.dto.RepositoryDto
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Contract for the planned Python FastAPI backend. */
interface ProvexApi {
    @POST("v1/auth/login")
    suspend fun login(@Body body: CredentialsDto): AuthResponseDto

    @POST("v1/auth/register")
    suspend fun register(@Body body: CredentialsDto): AuthResponseDto

    @POST("v1/analyses")
    suspend fun submitJob(@Body body: JobSubmissionDto): AnalysisDto

    @GET("v1/analyses/{id}")
    suspend fun getAnalysis(@Path("id") id: String): AnalysisDto

    @GET("v1/analyses/{id}/evidence")
    suspend fun getEvidence(@Path("id") id: String): EvidenceResponseDto

    @POST("v1/github/authorize")
    suspend fun beginGithubAuthorization(): GithubAuthorizationDto

    @GET("v1/github/repositories")
    suspend fun getRepositories(): List<RepositoryDto>

    @GET("v1/learning/recommendations")
    suspend fun getRecommendations(@Query("skill") skills: List<String>): List<RecommendationDto>
}

/** Holds the PROVEX session token in memory only. */
class SessionTokenHolder {
    @Volatile
    var token: String? = null
}

object NetworkModule {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** Configured via the `provex.apiBaseUrl` Gradle property; blank means no backend. */
    val baseUrl: String get() = BuildConfig.API_BASE_URL.trim()
    val isConfigured: Boolean get() = baseUrl.isNotEmpty()

    fun createApi(tokens: SessionTokenHolder): ProvexApi? {
        if (!isConfigured) return null
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val builder = chain.request().newBuilder()
                tokens.token?.let { builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            })
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProvexApi::class.java)
    }
}
