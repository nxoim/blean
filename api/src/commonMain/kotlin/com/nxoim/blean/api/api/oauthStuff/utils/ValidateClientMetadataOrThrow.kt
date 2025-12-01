package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.models.oauth.ClientMetadata
import com.nxoim.blean.api.models.oauth.scopes
import io.ktor.http.Url

/**
 * Validates the client metadata. Throws an error if the metadata is invalid.
 * [https://github.com/bluesky-social/cookbook/blob/03739f5268d5915e0dc7e8815723575f4b457bda/python-oauth-web-app/atproto_oauth.py#L14](https://github.com/bluesky-social/cookbook/blob/03739f5268d5915e0dc7e8815723575f4b457bda/python-oauth-web-app/atproto_oauth.py#L14)
 */
fun ClientMetadata.validateOrThrow(clientIdThatWasUsedToFetchClientMetadata: String): ClientMetadata {
    val errorMessages = mutableListOf<String>()
    val urlThatWasUsedToFetchClientMetadata = Url(clientIdThatWasUsedToFetchClientMetadata)

    if (this.clientUri == null) {
        errorMessages.add("Client uri is null")
    } else {
        val clientUrl = Url(this.clientUri)

        if (clientUrl.host != urlThatWasUsedToFetchClientMetadata.host)
            errorMessages.add("Client uri host in client metadata does not match the one in the url that was used to fetch the client metadata. Was ${clientUrl.host}")

        if (clientUrl.protocol.name != "https")
            errorMessages.add("Client uri protocol is not https. Was ${clientUrl.protocol}")

        if (clientUrl.specifiedPort != 0)
            errorMessages.add("Client uri port is not the default. Was ${clientUrl.specifiedPort}")

        if (!clientUrl.encodedPath.endsWith("/") && clientUrl.encodedPath.isNotBlank())
            errorMessages.add("Client uri path is not empty. Was ${clientUrl.encodedPath}")
    }

    if ("code" !in this.responseTypes)
        errorMessages.add("Response types must contain code. Was ${this.responseTypes}")

    if (!this.grantTypes.containsAll(listOf("authorization_code", "refresh_token")))
        errorMessages.add("Grant types must contain authorization_code and refresh_token. Was ${this.grantTypes}")


    if (!this.dpopBoundAccessTokens)
        errorMessages.add("DPoP bound access tokens must be true. Was ${this.dpopBoundAccessTokens}")

    if (!this.scopes.contains("atproto"))
        errorMessages.add("Scopes must contain atproto. Was ${this.scopes}")

    if (this.tokenEndpointAuthMethod != null)
        if (this.tokenEndpointAuthMethod !in listOf("none", "private_key_jwt"))
            errorMessages.add("Token endpoint auth method must be either none, or private_key_jwt. Was ${this.tokenEndpointAuthMethod}")


    if (this.tokenEndpointAuthSigningAlg != null && this.tokenEndpointAuthSigningAlg != "ES256")
        errorMessages.add("Token endpoint auth signing alg must be ES256. Was ${this.tokenEndpointAuthSigningAlg}")

    if (this.jwksUri != null && !this.jwksUri.startsWith("https"))
        errorMessages.add("Jwk uri's must start with https. Was ${this.jwksUri}")

    if (this.authorizationResponseIssParameterSupported != null && !this.authorizationResponseIssParameterSupported)
        errorMessages.add("Authorization response iss parameter supported must be true. Was ${this.authorizationResponseIssParameterSupported}")

    if (this.requirePushedAuthorizationRequests != null && !this.requirePushedAuthorizationRequests)
        errorMessages.add("Require pushed authorization requests must be true. Was ${this.requirePushedAuthorizationRequests}")

    if (this.clientIdMetadataDocumentSupported != null && !this.clientIdMetadataDocumentSupported)
        errorMessages.add("Client id metadata document supported must be true. Was ${this.clientIdMetadataDocumentSupported}")

    if (this.requireRequestUriRegistration != null && !this.requireRequestUriRegistration)
        errorMessages.add("Require request uri registration must be true. Was ${this.requireRequestUriRegistration}")

    if (this.codeChallengeMethodsSupported != null && !this.codeChallengeMethodsSupported.contains("S256"))
        errorMessages.add("Code challenge methods supported must contain S256. Was ${this.codeChallengeMethodsSupported}")


    if (this.dpopSigningAlgValuesSupported != null && !this.dpopSigningAlgValuesSupported.contains("ES256"))
        errorMessages.add("Dpop signing alg values supported must contain ES256. Was ${this.dpopSigningAlgValuesSupported}")

    if (this.redirectUris.isEmpty())
        errorMessages.add("Redirect uris must not be empty.")

    if (errorMessages.isNotEmpty())
        error(
            "Could not verify client metadata because: \n${
                errorMessages.joinToString(
                    separator = "\n- ",
                    prefix = "- ",
                    postfix = "\n"
                )
            }"
        )
    return this
}
