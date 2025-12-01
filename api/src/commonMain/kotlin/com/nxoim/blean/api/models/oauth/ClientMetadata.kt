package com.nxoim.blean.api.models.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientMetadata(
    @SerialName("client_id") val clientId: String,
    @SerialName("application_type") val applicationType: String? = null,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("client_uri") val clientUri: String? = null,
    @SerialName("dpop_bound_access_tokens")
    val dpopBoundAccessTokens: Boolean,
    @SerialName("grant_types") val grantTypes: List<String>,
    @SerialName("redirect_uris") val redirectUris: List<String>,
    @SerialName("response_types") val responseTypes: List<String>,
    @SerialName("scope") val scope: String,
    @SerialName("token_endpoint_auth_method")
    val tokenEndpointAuthMethod: String? = null,
    @SerialName("token_endpoint_auth_signing_alg")
    val tokenEndpointAuthSigningAlg: String? = null,
    @SerialName("jwks") val jwks: Jwks? = null,
    @SerialName("jwks_uri") val jwksUri: String? = null,
    @SerialName("logo_uri") val logoUri: String? = null,
    @SerialName("tos_uri") val tosUri: String? = null,
    @SerialName("policy_uri") val policyUri: String? = null,

    @SerialName("authorization_response_iss_parameter_supported")
    val authorizationResponseIssParameterSupported: Boolean? = null,
    @SerialName("pushed_authorization_request_endpoint")
    val pushedAuthorizationRequestEndpoint: String? = null,
    @SerialName("require_pushed_authorization_requests")
    val requirePushedAuthorizationRequests: Boolean? = null,
    @SerialName("require_request_uri_registration")
    val requireRequestUriRegistration: Boolean? = null,
    @SerialName("client_id_metadata_document_supported")
    val clientIdMetadataDocumentSupported: Boolean? = null,
    @SerialName("code_challenge_methods_supported")
    val codeChallengeMethodsSupported: List<String>? = null,
    @SerialName("scopes_supported") val scopesSupported: List<String>? = null,
    @SerialName("dpop_signing_alg_values_supported")
    val dpopSigningAlgValuesSupported: List<String>? = null,
    @SerialName("token_endpoint_auth_methods_supported")
    val tokenEndpointAuthMethodsSupported: List<String>? = null,
    @SerialName("token_endpoint_auth_signing_alg_values_supported")
    val tokenEndpointAuthSigningAlgValuesSupported: List<String>? = null
)

val ClientMetadata.scopes get() = scope.split(" ")