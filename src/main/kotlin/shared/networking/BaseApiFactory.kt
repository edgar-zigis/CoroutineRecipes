package shared.networking

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.serialization.json.Json
import shared.exceptions.ApiError

abstract class BaseApiFactory<T : BaseApiClient>(
    private val baseUrl: String,
    private val userAgent: String,
    private val timeout: Int? = null,
    private val logLevel: LogLevel = LogLevel.BODY
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

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

            install(ContentNegotiation) {
                gson {
                    serializeNulls()
                }
            }

            install(Logging) {
                logger = Logger.ANDROID
                level = logLevel
            }
        }
    }

    private suspend fun handleRequestError(requestError: Throwable) {
        val errorToThrow = when (requestError) {
            is ClientRequestException -> {
                val errorString = requestError.response.bodyAsText()
                try {
                    json.decodeFromString(ApiError.serializer(), errorString)
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