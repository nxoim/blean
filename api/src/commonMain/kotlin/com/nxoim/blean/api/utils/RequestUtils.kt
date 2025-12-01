package com.nxoim.blean.api.utils

import com.nxoim.blean.api.api.oauthStuff.models.ECDSAP256InBase64Keys
import com.nxoim.blean.api.api.oauthStuff.utils.PDSRequestDPoPAuthenticationContext
import com.nxoim.blean.bskyPrimitives.Did
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.ParametersBuilder
import io.ktor.http.parameters
import kotlin.jvm.JvmInline

// for autocompletion purposes so a typo is less liekly
const val ContentType = "Content-Type"
const val Accept = "Accept"
const val XWWWFormUrlEncoded = "application/x-www-form-urlencoded; charset=UTF-8"
const val ApplicationJson = "application/json"
const val Authorization = "Authorization"

fun HeadersBuilder.appendContentTypeApplicationJson() = append(ContentType, ApplicationJson)
fun HeadersBuilder.appendAcceptApplicationJson() = append(Accept, ApplicationJson)
fun HeadersBuilder.appendXWWWFormUrlEncoded() = append(Accept, XWWWFormUrlEncoded)
fun HeadersBuilder.appendBearerToken(accessJwt: String) = append(Authorization, "Bearer $accessJwt")
fun HeadersBuilder.appendDpopToken(dpopAccessToken: String) = append(Authorization, "DPoP $dpopAccessToken")
fun HeadersBuilder.appendDpopProof(dpopProof: String) = append("DPoP", dpopProof)
fun HeadersBuilder.appendDpopTokenAndProof(
    dpopAccessToken: String,
    dpopProof: String
) {
    appendDpopToken(dpopAccessToken)
    appendDpopProof(dpopProof)
}
fun HeadersBuilder.appendAtprotoProxy(value: String) = append("atproto-proxy", value)
fun HeadersBuilder.appendAtprotoAcceptLabelers(labelers: List<Did>) = appendAll("atproto-accept-labelers", labelers.map { it.toString() })

/**
 * Holds things necessary for making an authenticated request
 */
sealed interface AuthenticationContext {
    class AccessJwt(
        val value: String,
        val pdsUrl: String
    ) : AuthenticationContext

    @JvmInline
    value class OAuthContextForPDS(val value: PDSRequestDPoPAuthenticationContext) : AuthenticationContext
}

// to be used in state flow to update the context without necessarily recreating i
/**
 * Holds data necessary for making or updating an [AuthenticationContext]
 */
sealed interface AuthenticationMethod {
    @JvmInline
    value class AccessJwt(val value: String) : AuthenticationMethod

    class OAuth(
        val accessToken: String,
        val keyPair: ECDSAP256InBase64Keys,
    ) : AuthenticationMethod
}

suspend fun HttpClient.performAuthorizedRequest(
    authenticationContext: AuthenticationContext,
    httpMethod: HttpMethod,
    endpoint: String,
    parameters: ParametersBuilder.() -> Unit = {},
    headers: HeadersBuilder.() -> Unit = {},
): HttpResponse {
    @Suppress("DEPRECATION")
    return when (authenticationContext) {
        is AuthenticationContext.AccessJwt -> {
            this.request(
                authenticationContext.pdsUrl + endpoint,
                method = httpMethod,
                io.ktor.http.headers {
                    appendContentTypeApplicationJson()
                    appendBearerToken(authenticationContext.value)
                    headers()
                },
                parameters { parameters() }
            )
        }

        is AuthenticationContext.OAuthContextForPDS -> {
            authenticationContext.value.performRequestWithDpopHandling {
                val requestUrl = "${this.pdsUrl}$endpoint"
                val accessToken = this.authMethod.accessToken

                request(
                    requestUrl,
                    method = httpMethod,
                    io.ktor.http.headers {
                        appendContentTypeApplicationJson()
                        appendDpopTokenAndProof(
                            accessToken,
                            generateProof(requestUrl, method = httpMethod.value).toString()
                        )
                        headers()
                    },
                    parameters(parameters)
                )
            }
        }
    }
}

suspend inline fun <reified T : Any> HttpClient.performAuthorizedRequest(
    authenticationContext: AuthenticationContext,
    httpMethod: HttpMethod,
    endpoint: String,
    crossinline parameters: ParametersBuilder.() -> Unit = {},
    crossinline headers: HeadersBuilder.() -> Unit = {},
    body: T
): HttpResponse {
    return when (authenticationContext) {
        is AuthenticationContext.AccessJwt -> {
            this.request(
                authenticationContext.pdsUrl + endpoint,
                method = httpMethod,
                io.ktor.http.headers {
                    appendContentTypeApplicationJson()
                    appendBearerToken(authenticationContext.value)
                    headers()
                },
                parameters { parameters() },
                body = body
            )
        }

        is AuthenticationContext.OAuthContextForPDS -> {
            authenticationContext.value.performRequestWithDpopHandling {
                val requestUrl = "${this.pdsUrl}$endpoint"
                val accessToken = this.authMethod.accessToken

                request(
                    requestUrl,
                    method = httpMethod,
                    io.ktor.http.headers {
                        appendContentTypeApplicationJson()
                        appendDpopTokenAndProof(
                            accessToken,
                            generateProof(requestUrl, method = httpMethod.value).toString()
                        )
                        headers()
                    },
                    parameters { parameters() },
                    body = body
                )
            }
        }
    }
}

