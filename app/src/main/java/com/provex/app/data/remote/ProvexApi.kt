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
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

import com.provex.app.data.remote.dto.RegisterRequestDto
import com.provex.app.data.remote.dto.RefreshTokenDto
import com.provex.app.data.remote.dto.LogoutRequestDto
import com.provex.app.data.remote.dto.UserDto

/** Contract for the PROVEX Python FastAPI backend. */
interface ProvexApi {
    @GET("api/v1/health")
    suspend fun health(): Map<String, String>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: CredentialsDto): AuthResponseDto

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body body: RefreshTokenDto): AuthResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): Map<String, String>

    @GET("api/v1/auth/me")
    suspend fun getMe(): UserDto

    @GET("api/v1/profile")
    suspend fun getProfile(): com.provex.app.data.remote.dto.CareerProfileDto

    @PUT("api/v1/profile")
    suspend fun updateProfile(@Body body: com.provex.app.data.remote.dto.CareerProfileUpdateDto): com.provex.app.data.remote.dto.CareerProfileDto

    @POST("api/v1/analyses")
    suspend fun submitJob(@Body body: JobSubmissionDto): AnalysisDto

    @GET("api/v1/analyses/{id}")
    suspend fun getAnalysis(@Path("id") id: String): AnalysisDto

    @GET("api/v1/analyses/{id}/evidence")
    suspend fun getEvidence(@Path("id") id: String): EvidenceResponseDto

    @POST("api/v1/github/authorize")
    suspend fun beginGithubAuthorization(): GithubAuthorizationDto

    @GET("api/v1/github/status")
    suspend fun getGithubStatus(): com.provex.app.data.remote.dto.GitHubStatusDto

    @DELETE("api/v1/github/disconnect")
    suspend fun disconnectGithub(): Map<String, String>

    @GET("api/v1/github/repositories")
    suspend fun getRepositories(): List<com.provex.app.data.remote.dto.GitHubRepoDto>

    @GET("api/v1/learning/recommendations")
    suspend fun getRecommendations(@Query("skill") skills: List<String>): List<RecommendationDto>

    @POST("api/v1/jobs/analyze")
    suspend fun analyzeJob(@Body body: com.provex.app.data.remote.dto.JobAnalysisRequestDto): com.provex.app.data.remote.dto.JobAnalysisResponseDto

    @POST("api/v1/jobs/match-github")
    suspend fun matchJobGithub(@Body body: com.provex.app.data.remote.dto.GitHubMatchRequestDto): com.provex.app.data.remote.dto.GitHubMatchResultDto
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
