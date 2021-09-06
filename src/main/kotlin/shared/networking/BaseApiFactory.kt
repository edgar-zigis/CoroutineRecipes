package shared.networking

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import shared.exceptions.ApiError

abstract class BaseApiFactory<T : BaseApiClient>(
    private val baseUrl: String,
    private val userAgent: String,
    private val timeout: Int? = null,
    private val logLevel: LogLevel = LogLevel.BODY
) {
    abstract fun createClient(): T

    protected fun createKtorClient(): HttpClient {
        return HttpClient(Java) {

            defaultRequest {
                url {
                    takeFrom(URLBuilder().takeFrom(baseUrl).apply {
                        encodedPath += encodedPath
                    })
                }
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.UserAgent, userAgent)
            }

            HttpResponseValidator {
                handleResponseException {
                    handleRequestError(it)
                }
            }

            timeout?.let {
                install(HttpTimeout) {
                    requestTimeoutMillis = it.toLong()
                    socketTimeoutMillis = it.toLong()
                    connectTimeoutMillis = it.toLong()
                }
            }

            install(JsonFeature) {
                serializer = createSerializer()
            }

            install(Logging) {
                logger = Logger.ANDROID
                level = logLevel
            }
        }
    }

    protected open fun createSerializer(): KotlinxSerializer {
        return KotlinxSerializer(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }

    private suspend fun handleRequestError(requestError: Throwable) {
        val errorToThrow = when (requestError) {
            is ClientRequestException -> {
                val errorString = requestError.response.readText()
                try {
                    Json {
                        ignoreUnknownKeys = true
                    }.decodeFromString(ApiError.serializer(), errorString)
                } catch (e: Exception) {
                    ApiError(message = requestError.message)
                }
            }
            else -> {
                ApiError(message = requestError.message ?: "")
            }
        }
        throw errorToThrow
    }
}