package com.nxoim.blean.api.utils

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.nxoim.blean.api.api.oauthStuff.models.ResponseWithDPoPNonce
import com.nxoim.blean.api.api.oauthStuff.utils.internal.getDpopNonceFromHeaders
import com.nxoim.blean.api.api.oauthStuff.utils.internal.headersContainUseDpopNonceError
import com.nxoim.blean.commonThingsDumpster.RequestError
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlin.jvm.JvmInline


typealias RequestResult<SuccessType> =
        Result<SuccessType, RequestError<Nothing>>

typealias RequestResultPuperUltra<SuccessType, AdditionalErrorType> =
        Result<SuccessType, RequestError<AdditionalErrorType>>

// ---------------------------------------------------------------------

typealias OAuthRelatedResponseResult<SuccessType> =
        RequestResultPuperUltra<SuccessType, OAuthRequestError>

sealed interface OAuthRequestError {
    @JvmInline
    value class UseDPopNonce(val nonce: String) : OAuthRequestError

    @JvmInline
    value class InvalidGrant(val reason: String) : OAuthRequestError

    // more in RFC 6749 https://datatracker.ietf.org/doc/html/rfc6749
    // invalid_request
    //               The request is missing a required parameter, includes an
    //               unsupported parameter value (other than grant type),
    //               repeats a parameter, includes multiple credentials,
    //               utilizes more than one mechanism for authenticating the
    //               client, or is otherwise malformed.
    //
    //         invalid_client
    //               Client authentication failed (e.g., unknown client, no
    //               client authentication included, or unsupported
    //               authentication method).  The authorization server MAY
    //               return an HTTP 401 (Unauthorized) status code to indicate
    //               which HTTP authentication schemes are supported.  If the
    //               client attempted to authenticate via the "Authorization"
    //               request header field, the authorization server MUST
    //               respond with an HTTP 401 (Unauthorized) status code and
    //               include the "WWW-Authenticate" response header field
    //               matching the authentication scheme used by the client.
    //
    //         invalid_grant
    //               The provided authorization grant (e.g., authorization
    //               code, resource owner credentials) or refresh token is
    //               invalid, expired, revoked, does not match the redirection
    //               URI used in the authorization request, or was issued to
    //               another client.
    //
    //         unauthorized_client
    //               The authenticated client is not authorized to use this
    //               authorization grant type.
    //
    //         unsupported_grant_type
    //               The authorization grant type is not supported by the
    //               authorization server.
    //          invalid_scope
    //               The requested scope is invalid, unknown, malformed, or
    //               exceeds the scope granted by the resource owner
}

// Needs to be inlined for ios as of kotlin 2.0.20. Otherwise
// will crash with kotlin.native.internal.IrLinkageError: Function
// 'bodyNullable' can not be called: Suspend function can be
// called only from a coroutine or another suspend function


private suspend fun <R, O> handleRequest(
    block: suspend () -> HttpResponse,
    successHandler: suspend (HttpResponse) -> R,
    httpErrorHandler: suspend (HttpResponse) -> RequestError<O> = {
        RequestError.Http(
            it.status.value,
            it.status.description,
            it.bodyAsText()
        )
    }
) = handleRequestInline({ block() }, { successHandler(it) }, { httpErrorHandler(it) })

private inline fun <R, O> handleRequestInline(
    block: () -> HttpResponse,
    successHandler: (HttpResponse) -> R,
    httpErrorHandler: (HttpResponse) -> RequestError<O>
): Result<R, RequestError<O>> {
    return try {
        val response = block()

        if (response.status.isSuccess()) {
            Ok(successHandler(response))
        } else {
            Err(httpErrorHandler(response))
        }
    } catch (e: UnresolvedAddressException) {
        Err(RequestError.UnresolvedAddress())
    } catch (e: JsonConvertException) {
        Err(RequestError.Internal(e))
    } catch (e: HttpRequestTimeoutException) {
        Err(RequestError.Timeout())
    } catch (e: ConnectTimeoutException) {
        Err(RequestError.Timeout())
    } catch (e: Exception) {
        if (e.isNoRouteHostException())
            Err(RequestError.CantConnectToInternet())
        else
            Err(RequestError.Internal(e))
    }
}

private suspend fun HttpResponse.toRequestErrorWithOauth(): RequestError<OAuthRequestError> {
    val bodyText = this.bodyAsText()

    return if (
        this.headersContainUseDpopNonceError() ||
        bodyText.contains(""""error":"use_dpop_nonce"""")
    ) {
        RequestError.Other(
            OAuthRequestError.UseDPopNonce(
                this.getDpopNonceFromHeaders()
                    ?: error("Cant extract dpop nonce despite the header containing it. Weird")
            )
        )
    } else if (
        bodyText.contains(""""error":"invalid_grant"""")
    ) {
        RequestError.Other(
            OAuthRequestError.InvalidGrant(
                reason = bodyText
                    .replace(""""error":"invalid_grant","error_description":"""", "")
                    .replace("\"", "")
                    .replace("{", "")
                    .replace("}", "")
            )
        )
    } else {
        RequestError.Http(
            this.status.value,
            this.status.description,
            this.bodyAsText()
        )
    }
}

internal suspend inline fun <reified T> runRequestCatching(
    noinline block: suspend () -> HttpResponse
): RequestResult<T> = handleRequest(
    block = block,
    successHandler = { it.body() },
)

internal suspend inline fun <reified T, O> runRequestCatching(
    noinline errorMapping: suspend (HttpResponse) -> RequestError<O>,
    noinline block: () -> HttpResponse
):  Result<T, RequestError<O>> = handleRequest(
    block = block,
    successHandler = { it.body() },
    httpErrorHandler = errorMapping
)

internal suspend inline fun <reified T> runOauthedRequestCatching(
    noinline block: suspend () -> HttpResponse
): OAuthRelatedResponseResult<T> = handleRequest(
    block = block,
    successHandler = { it.body() },
    httpErrorHandler = { it.toRequestErrorWithOauth() }
)

internal suspend inline fun <reified T> runOauthedRequestWithDpopNonceCatching(
    noinline block: suspend () -> HttpResponse
): OAuthRelatedResponseResult<ResponseWithDPoPNonce<T>> = handleRequest(
    block = block,
    successHandler = {
        ResponseWithDPoPNonce(
            it.getDpopNonceFromHeaders()
                ?: error("Expected a DPoP nonce in the response headers"),
            it.body<T>()
        )
    },
    httpErrorHandler = { it.toRequestErrorWithOauth() }
)