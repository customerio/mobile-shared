package io.customer.shared.tracking.api

import io.customer.shared.tracking.api.model.BatchTrackingRequestBody
import io.customer.shared.tracking.api.model.BatchTrackingResponse
import io.customer.shared.tracking.api.model.BatchTrackingResponseBody
import io.customer.shared.tracking.api.model.TrackingRequest
import io.customer.shared.util.Logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

internal interface TrackingHttpClient {
    suspend fun track(batch: List<TrackingRequest>): Result<BatchTrackingResponse>
}

internal class TrackingHttpClientImpl(
    private val logger: Logger,
    private val httpClient: HttpClient,
) : TrackingHttpClient {
    override suspend fun track(batch: List<TrackingRequest>): Result<BatchTrackingResponse> {
        val result = kotlin.runCatching {
            logger.debug("Batching track events: ${batch.size}")
            val httpResponse = httpClient.post {
                url { path(URLPaths.BATCH_TASKS) }
                setBody(body = BatchTrackingRequestBody(batch = batch))
            }
            val responseBody = httpResponse.body<BatchTrackingResponseBody>()
            val response = BatchTrackingResponse(
                statusCode = httpResponse.status.value,
                errors = responseBody.errors ?: emptyList(),
            )
            logger.debug("Batch tracking completed with code: ${response.statusCode}")
            return@runCatching response
        }
        result.onFailure { ex ->
            logger.error("Batch tracking failed with error: ${ex.message}")
            ex.printStackTrace()
        }
        return result
    }
}
