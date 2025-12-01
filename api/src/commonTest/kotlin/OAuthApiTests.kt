@file:OptIn(ExperimentalTime::class)

import com.github.michaelbull.result.andThenRecover
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.OAuthApi
import com.nxoim.blean.api.api.oauthStuff.models.AuthorizationFromWebviewResult
import com.nxoim.blean.api.api.oauthStuff.models.DPoPProof
import com.nxoim.blean.api.api.oauthStuff.models.ECDSAP256InBase64Keys
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeChallenge
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeVerifier
import com.nxoim.blean.api.api.oauthStuff.models.OAuthStateToken
import com.nxoim.blean.api.api.oauthStuff.utils.PDSRequestDPoPAuthenticationContext
import com.nxoim.blean.api.api.oauthStuff.utils.buildAuthorizationWebviewUrl
import com.nxoim.blean.api.api.oauthStuff.utils.generate
import com.nxoim.blean.api.api.oauthStuff.utils.generateForTokenRequest
import com.nxoim.blean.api.api.oauthStuff.utils.parseAuthorizationWebviewUrlResult
import com.nxoim.blean.api.api.oauthStuff.utils.signWithECDSAP256InSHA256
import com.nxoim.blean.api.api.oauthStuff.utils.toCodeChallenge
import com.nxoim.blean.api.api.oauthStuff.utils.validateOrThrow
import com.nxoim.blean.api.api.oauthStuff.utils.validateSafetyOrThrow
import com.nxoim.blean.api.api.oauthStuff.utils.validateWebviewAuthResultOrThrow
import com.nxoim.blean.api.api.oauthStuff.utils.verifySafetyOrThrow
import com.nxoim.blean.api.models.oauth.AuthorizationServerMetadata
import com.nxoim.blean.api.models.oauth.ClientMetadata
import com.nxoim.blean.api.models.oauth.OAuthTokenData
import com.nxoim.blean.api.models.oauth.scopes
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.AuthenticationMethod
import com.nxoim.blean.api.utils.OAuthRequestError
import com.nxoim.blean.commonThingsDumpster.RequestError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class OAuthTestSession(
    val oAuthStateToken: OAuthStateToken = OAuthStateToken.generate(),
    val codeVerifier: OAuthCodeVerifier = OAuthCodeVerifier.generate(),
    val keyPair: ECDSAP256InBase64Keys = ECDSAP256InBase64Keys.generate(),
    val codeChallenge: OAuthCodeChallenge = codeVerifier.toCodeChallenge(),
    var clientMetadata: ClientMetadata? = null,
    var authServer: AuthorizationServerMetadata? = null,
    var pushedAuthRequestDpopNonce: String? = null,
    var tokenEndpointDpopNonce: String? = null,
    var pdsDpopNonce: String? = null,
    var webviewAuthResult: AuthorizationFromWebviewResult.Success? = null,
)

class OAuthApiTests {
    val httpClient = createTestClient()
    val oauthApi = OAuthApi(httpClient.value)
    val accountApi = AccountApi(httpClient.value)

    // step 1
    // get client metadata and validate it
    // get oauth auth server
    // make a pushed auth request and get dpop nonce header
    // build the authorization webview url from pusbed auth result
    // cache all the data
    @Test
    fun testFlowUpToWebviewAuth() = runTest {
        val testSession = OAuthTestSession() // new
        val clientMetadata =
            oauthApi.getClientMetadata(clientIdSecret)
                .onSuccess {
                    println(it)
                    checkSerializationValidity(it)
                    it.validateOrThrow(clientIdSecret)
                    testSession.clientMetadata = it
                }
                .getOrElse { error("Could not get client metadata. Response: $it") }

        val authServer = oauthApi.getAuthorizationServerMetadata("https://bsky.social")
            .onSuccess {
                it.verifySafetyOrThrow()
                checkSerializationValidity(it)
                println(it)
                testSession.authServer = it
            }
            .getOrElse { error("Could not get client metadata. Response: $it") }

        val pushedAuthorizationRequest = oauthApi.getPushedAuthorizationRequest(
            pushedAuthRequestFullUrl = authServer.pushedAuthorizationRequestEndpoint,
            responseType = clientMetadata.responseTypes.first(),
            clientId = clientMetadata.clientId,
            redirectUri = clientMetadata.redirectUris.first(),
            scopes = clientMetadata.scopes,
            state = testSession.oAuthStateToken,
            codeChallenge = testSession.codeChallenge,
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it.response)
                testSession.pushedAuthRequestDpopNonce = it.dpopNonce
            }
            .getOrElse { error("Could not get pushed authorization. Response: $it") }

        val webviewAuthPageUrl = buildAuthorizationWebviewUrl(
            authServer.authorizationEndpoint,
            clientMetadata.clientId,
            pushedAuthorizationRequest.response.requestUri
        )

        println(webviewAuthPageUrl)
        cacheOAuthTestSession(testSession)
    }

    // step 2
    // get the webview authentication result
    // get the cached session data
    // parse and validate the web view auth result
    // generate the dpop header for initial oauth tokens request
    // get the initial oauth tokens
    // get the profile for the received did in oauth tokens response
    // verify that the pds of the profile matches the oauth tokens issuer/pds
    // test getting data from an endpoint that requires accessJwt in the header
    // cache all the data
    @OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class)
    @Test
    fun testFlowFromWebviewAuthResultUpToGettingToken() = runTest {
        // manually paste the result here
        val webviewAuthResult =
            "PASTE HERE"
        val testSession = loadOAuthTestSession()
        val clientMetadata = testSession.clientMetadata
            ?: error("Previous tests werent completed and required state is unavailabke")
        val authServer = testSession.authServer
            ?: error("Previous tests werent completed and required state is unavailabke")

        val validatedParsedAuthResult = validateWebviewAuthResultOrThrow(
            parsedAuthResult = parseAuthorizationWebviewUrlResult(webviewAuthResult),
            authServer = authServer,
            oauthStateToken = testSession.oAuthStateToken
        )

        testSession.webviewAuthResult = validatedParsedAuthResult

        val publicKeyXY = testSession.keyPair.extractedXYFromPublicKey
        val dpopProof = DPoPProof.generateForTokenRequest(
            clientId = clientMetadata.clientId,
            tokenEndpoint = authServer.tokenEndpoint,
            tokenEndpointDpopNonce = testSession.pushedAuthRequestDpopNonce ?: error("Dpop nonce was null"),
            publicKey= publicKeyXY,
            timeEpochSeconds = Clock.System.now().epochSeconds,
            sign = { it.signWithECDSAP256InSHA256(testSession.keyPair.private) }
        )

        val tokens = oauthApi.getInitialTokens(
            redirectUrl = clientMetadata.redirectUris.first(),
            code = validatedParsedAuthResult.code,
            codeVerifier = testSession.codeVerifier,
            clientId = clientMetadata.clientId,
            tokenEndpointFullUrl = authServer.tokenEndpoint,
            dpopProof = dpopProof
        )
            .onSuccess {
                it.response.validateSafetyOrThrow(authServer.issuer)
                checkSerializationValidity(it.response)
                println("initial tokens validated $it")

                testSession.tokenEndpointDpopNonce = it.dpopNonce
            }
            .getOrElse { error("Could not get token. Response: $it") }
            .response

        performAuthorizedRequestToPds(testSession, tokens)

        cacheOAuthTestSession(testSession)
        cacheOAuthToken(tokens)
    }

    // step 3
    // get the cached session data
    // make a new dpop header just like for the initial request
    // try refreshing the token and if the new tokens dont contain a new refresh token - keep the old one
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun refreshTokens() = runTest {
        val testSession = loadOAuthTestSession()
        val authServer = testSession.authServer
            ?: error("Previous tests werent completed and required state is unavailabke")
        val oldOAuthToken = loadOAuthToken()
        var dpopNonce = "initial"

        val keysXY = testSession.keyPair.extractedXYFromPublicKey
        // generating new dpop proof for each request, otherwise will get bad request errors and whatever
        val newDpopProof = {
            DPoPProof.generateForTokenRequest(
                clientId = testSession.clientMetadata!!.clientId,
                tokenEndpoint = authServer.tokenEndpoint,
                tokenEndpointDpopNonce = dpopNonce,
                publicKey= keysXY,
                timeEpochSeconds = Clock.System.now().epochSeconds,
                sign = { it.signWithECDSAP256InSHA256(testSession.keyPair.private) }
            )
        }

        val getRefreshedToken = suspend {
            oauthApi.getRefreshedToken(
                clientId = testSession.clientMetadata?.clientId
                    ?: error("client metadata must not be null when refreshing token"),
                refreshToken = oldOAuthToken.refreshToken
                    ?: error("cant refresh the token without a refresh token"),
                tokenEndpointFullUrl = testSession.authServer!!.tokenEndpoint,
                dpopProof = newDpopProof()
            )
        }

        getRefreshedToken()
            .andThenRecover {
                // save dpop and repeat
                if (it is RequestError.Other<OAuthRequestError>){
                    if (it.value is OAuthRequestError.UseDPopNonce)
                        dpopNonce = (it.value as OAuthRequestError.UseDPopNonce).nonce
                }

                getRefreshedToken()
            }
            .onSuccess {
                println("refreshed token received $it")

                it.validateSafetyOrThrow(authServer.issuer)
                checkSerializationValidity(it)

                cacheOAuthTestSession(testSession)
                cacheOAuthToken(
                    if (it.refreshToken == null)
                        it.copy(refreshToken = oldOAuthToken.refreshToken)
                    else
                        it
                )
            }
            .onFailure { error("Could not refresh token. Response: $it") }
    }

    @Test
    fun refreshTokensAndPerformAuthorizedRequest() = runTest {
        refreshTokens()

        performAuthorizedRequestToPds(
            testSession = loadOAuthTestSession(),
            tokens = loadOAuthToken()
        )
    }

    suspend fun performAuthorizedRequestToPds(
        testSession: OAuthTestSession,
        tokens: OAuthTokenData
    ) {
        val context = PDSRequestDPoPAuthenticationContext(
            beforeRequestHappens = { },
            onCurrentTimeEpochSeconds = { Clock.System.now().epochSeconds },
            onInvalidAuthToken = { error("Invalid token performAuthorizedRequestToPds") },
            onRequestAuthMethod = {
                AuthenticationMethod.OAuth(tokens.accessToken, testSession.keyPair)
            }
        )

        accountApi.getProfile(AuthenticationContext.OAuthContextForPDS(context), ownHandleSecret)
            .onFailure { error("Cant perform the authorized thing. Response: \n $it") }
    }
}

fun cacheOAuthTestSession(testSession: OAuthTestSession) =
    saveToJsonInBuildCache("iyvd78", testSession)

fun loadOAuthTestSession() = readFromJsonInTempFolder<OAuthTestSession>("iyvd78")

fun cacheOAuthToken(value: OAuthTokenData) = saveToJsonInBuildCache("ihvi87", value = value)

fun loadOAuthToken() = readFromJsonInTempFolder("ihvi87", serializer = OAuthTokenData.serializer())