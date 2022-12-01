package io.customer.shared.tracking.api

import io.customer.shared.sdk.meta.IdentityType
import io.customer.shared.tracking.api.model.BatchTrackingRequestBody
import io.customer.shared.tracking.api.model.BatchTrackingResponse
import io.customer.shared.tracking.api.model.BatchTrackingResponseBody
import io.customer.shared.tracking.api.model.toTrackingRequest
import io.customer.shared.tracking.model.Activity
import io.customer.shared.util.Logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * HTTP client interface responsible for making all network calls in the SDK.
 */
internal interface TrackingHttpClient {
    suspend fun track(
        identityType: IdentityType,
        profileIdentifier: String,
        activities: List<Activity>,
    ): Result<BatchTrackingResponse>
}

internal class TrackingHttpClientImpl(
    private val logger: Logger,
    private val httpClient: HttpClient,
) : TrackingHttpClient {
    override suspend fun track(
        identityType: IdentityType,
        profileIdentifier: String,
        activities: List<Activity>,
    ): Result<BatchTrackingResponse> {
        val result = kotlin.runCatching {
            logger.debug("Batching ${activities.size} track events")
            val requestBody = BatchTrackingRequestBody(
                batch = activities.map { activity ->
                    activity.toTrackingRequest(
                        identityType = identityType,
                        profileIdentifier = profileIdentifier,
                    )
                },
            )
            val httpResponse = httpClient.post {
                url { path(URLPaths.BATCH_TASKS) }
                setBody(body = requestBody)
            }
            val responseBody = httpResponse.body<BatchTrackingResponseBody>()
            val response = BatchTrackingResponse(
                statusCode = httpResponse.status.value,
                errors = responseBody.errors ?: emptyList(),
            )
            logger.debug("Batch tracking successful with code: ${response.statusCode}")
            return@runCatching response
        }
        result.onFailure { ex ->
            logger.error("Batch tracking failed with error: ${ex.message}")
            ex.printStackTrace()
        }
        return result
    }
}
