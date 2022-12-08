package io.customer.shared.tracking.api

import io.customer.shared.tracking.api.model.BatchTrackingRequestBody
import io.customer.shared.tracking.api.model.BatchTrackingResponse
import io.customer.shared.tracking.api.model.BatchTrackingResponseBody
import io.customer.shared.tracking.api.model.toTrackingRequest
import io.customer.shared.tracking.model.Task
import io.customer.shared.util.Logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * HTTP client interface responsible for making all network calls in the SDK.
 *
 * The class doesn't perform any validation and is only responsible for transforming and passing
 * filtered data to the server.
 */
internal interface TrackingHttpClient {
    /**
     * Dispatch all tasks passed to the server in a single batch call.
     */
    suspend fun track(tasks: List<Task>): Result<BatchTrackingResponse>

    /**
     * Object class to hold URL constants in single place.
     */
    object URLPaths {
        const val BATCH_TASKS = "/alpha-api/v2/batch"
    }
}

internal class TrackingHttpClientImpl(
    private val logger: Logger,
    private val httpClient: HttpClient,
) : TrackingHttpClient {
    override suspend fun track(tasks: List<Task>): Result<BatchTrackingResponse> {
        return kotlin.runCatching {
            val requestBody = BatchTrackingRequestBody(
                batch = tasks.map { task ->
                    // Ideally, tasks passed here should never have invalid identifier
                    task.activity.toTrackingRequest(
                        identityType = task.identityType,
                        profileIdentifier = task.profileIdentifier.orEmpty(),
                    )
                },
            )
            val httpResponse = httpClient.post {
                url { path(TrackingHttpClient.URLPaths.BATCH_TASKS) }
                setBody(body = requestBody)
            }
            val responseBody = httpResponse.body<BatchTrackingResponseBody>()
            return@runCatching BatchTrackingResponse(
                statusCode = httpResponse.status.value,
                errors = responseBody.errors ?: emptyList(),
            )
        }
    }
}
