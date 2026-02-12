package com.nxoim.blean.api.api.oauthStuff.utils

import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.AtomicReference
import com.github.michaelbull.result.Result
import com.nxoim.blean.api.api.oauthStuff.models.DPoPProof
import com.nxoim.blean.api.api.oauthStuff.models.SignedJWTMessage
import com.nxoim.blean.api.api.oauthStuff.models.UnsignedJWTMessage
import com.nxoim.blean.api.api.oauthStuff.models.pdsUrlOrThrow
import com.nxoim.blean.api.api.oauthStuff.utils.internal.containsInvalidTokenError
import com.nxoim.blean.api.api.oauthStuff.utils.internal.getDpopNonceFromHeaders
import com.nxoim.blean.api.api.oauthStuff.utils.internal.headersContainUseDpopNonceError
import com.nxoim.blean.api.utils.AuthenticationMethod
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Deferred

private const val logTag = "PDSRequestDPoPAuthenticationContext"

/**
 * TODO explain whats expected behavior and use
 * This gets created with the client after authorization. The OAuth thing
 * allows to get the user's pds url and that pds url must be used to
 * perform authenticated requests.
 *
 * An instance of this class must be held separately from
 * credentials flow so the Deferred<*> of onInvalidAuthToken
 * also remains single, so refresh attempts
 */
class PDSRequestDPoPAuthenticationContext(
    private val onCurrentTimeEpochSeconds: () -> Long,
    private val beforeRequestHappens: suspend () -> Unit,
    private val onInvalidAuthToken: suspend () -> Deferred<Result<*, *>>,
    private val onRequestAuthMethod: () -> AuthenticationMethod.OAuth,
    private val logger: Logger = Logger,
    private val onSignDpopProofJwtMessageRequest: (
        AuthenticationMethod.OAuth,
        UnsignedJWTMessage
    ) -> SignedJWTMessage = { method, message ->
        // its fine to have a default here since this is made specific to bluesky
        message.signWithECDSAP256InSHA256(method.keyPair.private)
    },
) {
    /**
     * Is specific to this PDS. Can be incorrect and it's changes don't need to be observed.
     * Requests will update this value.
     */
    private val latestDpopNonce = AtomicReference("initialForThisSession")
    val authMethod get() = onRequestAuthMethod()
    val decodedAccessToken get() = decodeAuthToken(authMethod.accessToken)
    val pdsUrl get() = decodedAccessToken.pdsUrlOrThrow

    fun generateProof(
        url: String,
        method: String,
    ): DPoPProof = DPoPProof.generateForResourceRequest(
        url = url,
        method = method,
        dPoPNonce = latestDpopNonce.get(),
        accessToken = authMethod.accessToken,
        authorizationServerIssuer = decodedAccessToken.iss,
        publicKey = authMethod.keyPair.extractedXYFromPublicKey,
        timeEpochSeconds = onCurrentTimeEpochSeconds(),
        sign = { onSignDpopProofJwtMessageRequest(authMethod, it) }
    )

    suspend fun performRequestWithDpopHandling(
        makeRequest: suspend PDSRequestDPoPAuthenticationContext.() -> HttpResponse
    ): HttpResponse = recursivelyRetriableRequest(maxAttempts = 3, makeRequest)

    private suspend fun recursivelyRetriableRequest(
        maxAttempts: Int,
        makeRequest: suspend PDSRequestDPoPAuthenticationContext.() -> HttpResponse,
        currentAttempt: Int = 1
    ): HttpResponse {
        beforeRequestHappens()
        val response = makeRequest()

        return when {
            response.status.isSuccess()
                    || currentAttempt >= maxAttempts -> response

            response.headersContainUseDpopNonceError() -> {
                logger.i(tag = logTag) { "Updating dpop nonce for next attempt" }
                response.getDpopNonceFromHeaders()?.let { latestDpopNonce.set(it) }

                recursivelyRetriableRequest(
                    maxAttempts = maxAttempts,
                    makeRequest = makeRequest,
                    currentAttempt = currentAttempt + 1
                )
            }

            response.containsInvalidTokenError() -> {
                logger.i(tag = logTag) { "Invalid token error" }
                val refreshJob = this.onInvalidAuthToken().await()

                return if (refreshJob.isErr) {
                    response
                } else {
                    recursivelyRetriableRequest(
                        maxAttempts = maxAttempts,
                        makeRequest = makeRequest,
                        currentAttempt = currentAttempt + 1
                    )
                }
            }

            else -> response
        }
    }
}