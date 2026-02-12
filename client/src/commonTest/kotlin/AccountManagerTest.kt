import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.value
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onFailure
import com.nxoim.blean.api.BleanApi
import com.nxoim.blean.api.BleanKtorClient
import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.api.OAuthApi
import com.nxoim.blean.api.api.RepoApi
import com.nxoim.blean.api.api.SessionApi
import com.nxoim.blean.api.api.mock._MockAccountApi
import com.nxoim.blean.api.api.mock._MockFeedApi
import com.nxoim.blean.api.api.mock._MockOAuthApi
import com.nxoim.blean.api.api.mock._MockRepoApi
import com.nxoim.blean.api.api.mock._MockSessionApi
import com.nxoim.blean.api.api.oauthStuff.models.DPoPProof
import com.nxoim.blean.api.api.oauthStuff.models.DecodedOAuthToken
import com.nxoim.blean.api.api.oauthStuff.models.ECDSAP256InBase64Keys
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeChallenge
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeVerifier
import com.nxoim.blean.api.api.oauthStuff.models.OAuthStateToken
import com.nxoim.blean.api.api.oauthStuff.models.ResponseWithDPoPNonce
import com.nxoim.blean.api.api.oauthStuff.utils.generate
import com.nxoim.blean.api.api.oauthStuff.utils.internal.invalidTokenHeaders
import com.nxoim.blean.api.models.account.Profile
import com.nxoim.blean.api.models.oauth.AuthorizationServerMetadata
import com.nxoim.blean.api.models.oauth.ClientMetadata
import com.nxoim.blean.api.models.oauth.OAuthTokenData
import com.nxoim.blean.api.models.oauth.PushedAuthorizationRequest
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.OAuthRelatedResponseResult
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.client.ATProtoOAuthClient
import com.nxoim.blean.client.AccountManager
import com.nxoim.blean.client.AccountsState
import com.nxoim.blean.client.AuthenticationManager
import com.nxoim.blean.client.BleanClient
import com.nxoim.blean.client.ContentRepositories
import com.nxoim.blean.client.UserDataRepositories
import com.nxoim.blean.client.UserRepositories
import com.nxoim.blean.draft.DraftsRoomDatabase
import com.nxoim.blean.models.LoggedInUserBasicDetails
import com.nxoim.blean.outbox.OutboxRoomDatabase
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentialsDao
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentialsDatabase
import com.nxoim.blean.platformCredentialsManagement.RoomCredentialsRepository
import com.nxoim.blean.repos.ApiResponseRoomDatabase
import com.nxoim.blean.repos.LoggedInUserBasicDetailsDao
import com.nxoim.blean.repos.LoggedInUsersRepository
import com.nxoim.blean.repos.LoggedInUsersRoomDatabase
import com.nxoim.blean.repos.MutedWordsRoomDatabase
import com.nxoim.blean.repos.OAuthAuthenticationAttemptRepository
import com.nxoim.blean.repos.OAuthConfigSettingsDao
import com.nxoim.blean.repos.OAuthConfigSettingsDatabase
import com.nxoim.blean.repos.SavedFeedsRoomDatabase
import com.nxoim.blean.repos.inMemoryDatabaseBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class AccountManagerTest {
    @Test
    fun `overall initialization`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val expectedUsers = List(5) {
            createMockUser(did = "did:plc:$it", handle = "test.handle.$it")
        }

        expectedUsers.forEach {
            userRepo.saveOrUpdateLoggedInUser(it)
        }

        userRepo.setupMockUserWithCredentials(expectedUsers.first())

        val accountManager = createMockAccountManager(userRepo)

        assertTrue("Initial account state must be Loading") {
            accountManager.accountsState.value is AccountsState.Loading
        }

        accountManager.loadInitialAccounts()

        assertTrue(
            "After loading initial accounts at least 1 account must be soft logged out, 1 logged in"
        ) {
            val state = accountManager.accountsState.value

            state is AccountsState.Initialized &&
                    state.accounts.value.values.all { it.basicDetails.value in expectedUsers } &&
                    state.accounts.value.values.any { it.client.value is BleanClient.SoftLoggedOut } &&
                    state.accounts.value.values.any { it.client.value is BleanClient.LoggedIn }
        }
    }

    @Test
    fun `soft logout removes credentials but keeps account in records`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val expectedUser = createMockUser()

        userRepo.setupMockUserWithCredentials(expectedUser)

        val fileSystem = FakeFileSystem()
        val accountManager = createMockAccountManager(
            userRepo,
            repositories = createMockUserRepositories(did = expectedUser.did, fileSystem = fileSystem)
        )

        accountManager.loadInitialAccounts()

        accountManager.accountsState.value.let {
            assertTrue(it is AccountsState.Initialized)
            assertEquals(it.accounts.value.size, 1)
            assertEquals(it.accounts.value.values.first().basicDetails.value, expectedUser)
        }

        val softLogoutResult = accountManager.softLogout(expectedUser.did)
            .onFailure { println(it.stackTraceToString()) }

        assertTrue(softLogoutResult.isOk)

        accountManager.accountsState.value.let {
            assertTrue(it is AccountsState.Initialized)
            assertEquals(it.accounts.value.size, 1)
            assertEquals(it.accounts.value.values.first().basicDetails.value, expectedUser)
            assertTrue(it.accounts.value.values.first().client.value is BleanClient.SoftLoggedOut)
        }

        assertTrue(
            fileSystem.exists(mockUserDataFolderPath(expectedUser.did)),
            "Soft log out is supposed to keep user data folders"
        )

        assertTrue(
            fileSystem.exists(mockUserCacheFolderPath(expectedUser.did)),
            "Soft log out is supposed to keep user cache folders"
        )
    }

    @Test
    fun `hard logout removes account completely`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val expectedUser = createMockUser()

        userRepo.setupMockUserWithCredentials(expectedUser)

        val fileSystem = FakeFileSystem()
        val accountManager = createMockAccountManager(
            userRepo,
            repositories = createMockUserRepositories(did = expectedUser.did, fileSystem = fileSystem)
        )

        accountManager.loadInitialAccounts()

        accountManager.accountsState.value.let {
            assertTrue(it is AccountsState.Initialized)
            assertEquals(it.accounts.value.size, 1)
            assertEquals(it.accounts.value.values.first().basicDetails.value, expectedUser)
        }

        val logoutResult = accountManager.logout(expectedUser.did)
            .onFailure { println(it.stackTraceToString()) }

        assertTrue(logoutResult.isOk)

        accountManager.accountsState.value.let {
            assertTrue(it is AccountsState.Initialized)
            assertEquals(emptyMap(), it.accounts.value)
        }

        assertFalse(
            fileSystem.exists(mockUserDataFolderPath(expectedUser.did)),
            "Log out is supposed to delete user data folders"
        )

        assertFalse(
            fileSystem.exists(mockUserCacheFolderPath(expectedUser.did)),
            "Log out is supposed to delete user cache folders"
        )
    }

    @Test
    fun `OAuth flow creates and initializes new account`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val oauthAttemptRepo = MockOAuthAuthenticationAttemptRepository()
        val oauthConfig = createMockOAuthConfig()

        val mockInitialTokens = ResponseWithDPoPNonce(
            "anotherFakeDpopNonce",
            OAuthTokenData(
                createMockOAuthToken(
                    iss = oauthConfig.authServerMetadata.issuer,
                    sub = Did.Plc("did:plc:fakeSub"),
                    clientId = oauthConfig.clientMetadata.clientId,
                    aud = Did.Web("did:web:pds.example.com"),
                    scope = oauthConfig.clientMetadata.scope
                ),
                "fakeTokenType",
                "fakeRefreshToken",
                oauthConfig.clientMetadata.scope,
                -1,
                Did.Plc("did:plc:fakeSub")
            )
        )

        val mockProfile = Profile(
            AccountIdentificator.Did(Did.Plc("did:plc:fakeProfile")),
            handle = AccountIdentificator.Handle("fake.profile")
        )

        val mockOauthApi = object : _MockOAuthApi() {
            override suspend fun getClientMetadata(fullUrl: String) =
                Ok(oauthConfig.clientMetadata)

            override suspend fun getAuthorizationServerMetadata(entrypoint: String) =
                Ok(oauthConfig.authServerMetadata)

            override suspend fun getPushedAuthorizationRequest(
                pushedAuthRequestFullUrl: String,
                responseType: String,
                clientId: String,
                redirectUri: String,
                state: OAuthStateToken,
                scopes: List<String>,
                codeChallenge: OAuthCodeChallenge?,
                loginHint: String?,
                clientAssertion: String?
            ) = Ok(oauthConfig.par)

            override suspend fun getInitialTokens(
                redirectUrl: String,
                codeVerifier: OAuthCodeVerifier,
                code: String,
                tokenEndpointFullUrl: String,
                clientId: String,
                dpopProof: DPoPProof
            ) = Ok(mockInitialTokens)
        }

        val mockAccountApi = object : _MockAccountApi() {
            override suspend fun getProfile(
                authenticationContext: AuthenticationContext,
                targetHandleOrDid: AccountIdentificator
            ) = Ok(mockProfile)
        }

        val oauthClient = ATProtoOAuthClient(
            mockOauthApi,
            mockAccountApi,
            oauthConfig.clientMetadata.clientId
        )

        val accountManager = createMockAccountManager(
            userRepo,
            authManager = MockAuthenticationManager(userRepo, oauthAttemptRepo, oauthClient)
        )

        accountManager.loadInitialAccounts()
        assertTrue(accountManager.accountsState.value is AccountsState.Initialized)

        val beginningResult = accountManager.beginOauthAuthorization("https://mock.auth.server.entry")
            .onFailure { println(it.toString()) }

        assertTrue(beginningResult.isOk)

        assertEquals(
            oauthConfig.clientMetadata.clientId,
            oauthAttemptRepo.getClientId().firstOrNull()
        )
        assertEquals(
            oauthConfig.authServerMetadata.authorizationEndpoint,
            oauthAttemptRepo.getAuthorizationServer().firstOrNull()
        )

        val state = oauthAttemptRepo.getStateToken().firstOrNull()
        assertNotNull(state)

        val endResult = accountManager.continueOauthAuthorization(
            "${oauthConfig.clientMetadata.redirectUris.first()}?state=$state&iss=${oauthConfig.authServerMetadata.issuer}&code=cod-7927e1642fde08e5c5b780fefd751198e9f2e8b8aaf2ef0e0ff877dc60311cd2"
        ).onFailure { println(it.toString()) }

        assertTrue(endResult.isOk)

        oauthAttemptRepo.run {
            assertNull(getStateToken().firstOrNull())
            assertNull(getCodeVerifier().firstOrNull())
            assertNull(getAuthorizationServer().firstOrNull())
            assertNull(getClientId().firstOrNull())
            assertNull(getPushedAuthorizationRequestDpopNonce().firstOrNull())
        }

        accountManager.accountsState.value.let {
            assertTrue(it is AccountsState.Initialized)
            assertTrue(mockProfile.did in it.accounts.value)

            val client = it.accounts.value.values.first().client.value
            assertEquals(mockProfile.did, client.usersDid)
            assertTrue(client is BleanClient.LoggedIn)
        }
    }

    @Test
    fun `OAuth flow updates existing account credentials`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val oauthAttemptRepo = MockOAuthAuthenticationAttemptRepository()
        val oauthConfig = createMockOAuthConfig()

        val existingUser = createMockUser(did = "did:plc:existingUser", handle = "existing.user")
        userRepo.saveOrUpdateLoggedInUser(existingUser)


        val mockInitialTokens = ResponseWithDPoPNonce(
            "anotherFakeDpopNonce",
            OAuthTokenData(
                createMockOAuthToken(
                    iss = oauthConfig.authServerMetadata.issuer,
                    sub = existingUser.did.value,
                    clientId = oauthConfig.clientMetadata.clientId,
                    aud = Did.Web("did:web:pds.example.com"),
                    scope = oauthConfig.clientMetadata.scope
                ),
                "fakeTokenType",
                "fakeRefreshToken",
                oauthConfig.clientMetadata.scope,
                -1,
                existingUser.did.value
            )
        )

        val mockProfile = Profile(existingUser.did, handle = existingUser.handle)

        val mockOauthApi = object : _MockOAuthApi() {
            override suspend fun getClientMetadata(fullUrl: String) =
                Ok(oauthConfig.clientMetadata)

            override suspend fun getAuthorizationServerMetadata(entrypoint: String) =
                Ok(oauthConfig.authServerMetadata)

            override suspend fun getPushedAuthorizationRequest(
                pushedAuthRequestFullUrl: String,
                responseType: String,
                clientId: String,
                redirectUri: String,
                state: OAuthStateToken,
                scopes: List<String>,
                codeChallenge: OAuthCodeChallenge?,
                loginHint: String?,
                clientAssertion: String?
            ) = Ok(oauthConfig.par)

            override suspend fun getInitialTokens(
                redirectUrl: String,
                codeVerifier: OAuthCodeVerifier,
                code: String,
                tokenEndpointFullUrl: String,
                clientId: String,
                dpopProof: DPoPProof
            ) = Ok(mockInitialTokens)
        }

        val mockAccountApi = object : _MockAccountApi() {
            override suspend fun getProfile(
                authenticationContext: AuthenticationContext,
                targetHandleOrDid: AccountIdentificator
            ) = Ok(mockProfile)
        }

        val oauthClient = ATProtoOAuthClient(
            mockOauthApi,
            mockAccountApi,
            oauthConfig.clientMetadata.clientId
        )

        val accountManager = createMockAccountManager(
            userRepo,
            authManager = MockAuthenticationManager(userRepo, oauthAttemptRepo, oauthClient)
        )

        accountManager.loadInitialAccounts()

        accountManager.accountsState.value.let {
            assertTrue(it is AccountsState.Initialized)
            assertEquals(1, it.accounts.value.size)
            assertTrue(existingUser.did in it.accounts.value)

            val client = it.accounts.value[existingUser.did]?.client?.value
            assertTrue(client is BleanClient.SoftLoggedOut)
        }

        val beginningResult = accountManager.beginOauthAuthorization("https://mock.auth.server.entry")
            .onFailure { println(it.toString()) }

        assertTrue(beginningResult.isOk)

        val state = oauthAttemptRepo.getStateToken().firstOrNull()
        assertNotNull(state)

        val endResult = accountManager.continueOauthAuthorization(
            "${oauthConfig.clientMetadata.redirectUris.first()}?state=$state&iss=${oauthConfig.authServerMetadata.issuer}&code=cod-7927e1642fde08e5c5b780fefd751198e9f2e8b8aaf2ef0e0ff877dc60311cd2"
        ).onFailure { println(it.toString()) }

        assertTrue(endResult.isOk)

        accountManager.accountsState.value.let { state ->
            assertTrue(state is AccountsState.Initialized)
            assertEquals(1, state.accounts.value.size)
            assertTrue(existingUser.did in state.accounts.value)

            val account = state.accounts.value[existingUser.did]
            assertNotNull(account)

            val client = account.client.firstOrNull { it is BleanClient.LoggedIn }
            assertTrue(client is BleanClient.LoggedIn)
            assertEquals(existingUser.did, client.usersDid)
        }

        val credentials = userRepo.getCredentials(existingUser.did).firstOrNull()
        assertNotNull(credentials)
        assertTrue(credentials.isOk)
    }

    @Test
    fun `4xx token refresh error triggers automatic soft logout`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val expectedUser = createMockUser()
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(invalidTokenHeaders)
            )
        }
        val mockClient = HttpClient(mockEngine)

        userRepo.setupMockUserWithCredentials(expectedUser, clientId = mockClientId)

        val accountManager = AccountManager(
            logger = Logger,
            scope = backgroundScope,
            bleanApi = BleanApi(BleanKtorClient(mockClient)),
            rootUserRepository = userRepo,
            authenticationManager = MockAuthenticationManager(
                userRepo,
                oauthClient = ATProtoOAuthClient(
                    OAuthApi(BleanKtorClient(mockClient)),
                    AccountApi(mockClient),
                    clientId = mockClientId
                )
            ),
            userRepositoriesFactory = { createMockUserRepositories() }
        )

        accountManager.loadInitialAccounts()

        val state = accountManager.accountsState.value
        assertTrue(state is AccountsState.Initialized)
        val account = state.accounts.value[expectedUser.did]
        assertNotNull(account)

        val loggedInClient = account.client.value as BleanClient.LoggedIn

        loggedInClient.feed.loadAndCachePost(AtUri("at://test"))
            .onFailure { println(it) }

        account.client.firstOrNull { it is BleanClient.SoftLoggedOut }
        assertTrue(account.client.value is BleanClient.SoftLoggedOut)
    }

    @Test
    fun `concurrent requests during token refresh share single refresh operation`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val expectedUser = createMockUser()
        val refreshCallCount = AtomicInt(0)
        val oauthConfig = createMockOAuthConfig()

        val mockOauthApi = object : _MockOAuthApi() {
            override suspend fun getClientMetadata(fullUrl: String) =
                Ok(oauthConfig.clientMetadata)

            override suspend fun getAuthorizationServerMetadata(entrypoint: String) =
                Ok(oauthConfig.authServerMetadata)

            override suspend fun getRefreshedToken(
                clientId: String,
                refreshToken: String,
                tokenEndpointFullUrl: String,
                dpopProof: DPoPProof
            ): OAuthRelatedResponseResult<OAuthTokenData> {
                refreshCallCount.incrementAndGet()

                return Ok(
                    OAuthTokenData(
                        accessToken = createMockOAuthToken(
                            iss = oauthConfig.authServerMetadata.issuer,
                            sub = expectedUser.did.value,
                            clientId = oauthConfig.clientMetadata.clientId,
                            aud = Did.Web("did:web:pds.example.com"),
                            scope = oauthConfig.clientMetadata.scope
                        ),
                        tokenType = "Bearer",
                        refreshToken = "newRefreshToken",
                        scope = oauthConfig.clientMetadata.scope,
                        expiresIn = 3600,
                        sub = expectedUser.did.value
                    )
                )
            }
        }

        val oauthClient = ATProtoOAuthClient(
            mockOauthApi,
            _MockAccountApi(),
            oauthConfig.clientMetadata.clientId
        )

        userRepo.setupMockUserWithCredentials(
            expectedUser,
            authServerUrl = oauthConfig.authServerMetadata.issuer,
            clientId = oauthConfig.clientMetadata.clientId
        )

        val accountManager = createMockAccountManager(
            userRepo,
            authManager = MockAuthenticationManager(userRepo, oauthClient = oauthClient)
        )

        accountManager.loadInitialAccounts()

        val state = accountManager.accountsState.value
        assertTrue(state is AccountsState.Initialized)

        val client = state.accounts.value[expectedUser.did]?.client?.value
        assertTrue(client is BleanClient.LoggedIn)

        val results = List(5) {
            async {
                accountManager.forceRefreshOrAwait(expectedUser.did)
            }
        }.awaitAll()

        assertEquals(5, results.size)
        assertEquals(1, refreshCallCount.value)
        assertTrue(results.all { it.isOk })

        val updatedClient = state.accounts.value[expectedUser.did]?.client?.value
        assertTrue(updatedClient is BleanClient.LoggedIn)
    }

    @Test
    fun `network errors during refresh do not trigger logout`() = runTest {
        val userRepo = MockLoggedInUsersRepository()
        val expectedUser = createMockUser()

        var requestCount = 0
        val mockEngine = MockEngine { request ->
            requestCount++
            when {
                // return 400 to trigger refresh
                requestCount == 1 -> respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(invalidTokenHeaders)
                )
                // then network error
                else -> throw ConnectTimeoutException("Network unreachable")
            }
        }
        val mockClient = HttpClient(mockEngine)

        userRepo.setupMockUserWithCredentials(expectedUser, clientId = mockClientId)

        val accountManager = AccountManager(
            logger = Logger,
            scope = backgroundScope,
            bleanApi = BleanApi(BleanKtorClient(mockClient)),
            rootUserRepository = userRepo,
            authenticationManager = MockAuthenticationManager(
                userRepo,
                oauthClient = ATProtoOAuthClient(
                    OAuthApi(BleanKtorClient(mockClient)),
                    AccountApi(mockClient),
                    clientId = mockClientId
                )
            ),
            userRepositoriesFactory = { createMockUserRepositories() }
        )

        accountManager.loadInitialAccounts()

        val state = accountManager.accountsState.value
        assertTrue(state is AccountsState.Initialized)
        val account = state.accounts.value[expectedUser.did]
        assertNotNull(account)

        val client = account.client.value
        assertTrue(client is BleanClient.LoggedIn)

        val loadResult = client.feed.loadAndCachePost(AtUri("at://test"))
            .onFailure { println(it) }

        assertTrue(loadResult.isErr)

        assertTrue(account.client.value is BleanClient.LoggedIn)
    }
}

private fun mockUserCacheFolderPath(
    did: AccountIdentificator.Did
): Path = "/testCache".toPath().resolve("$did")

private fun mockUserDataFolderPath(
    did: AccountIdentificator.Did
): Path = "/testData".toPath().resolve("$did")

private fun createMockUserRepositories(
    did: AccountIdentificator.Did = AccountIdentificator.Did(Did.Plc("did:plc:test")),
    fileSystem: FileSystem = FakeFileSystem()
) = UserRepositories(
    UserDataRepositories(
        feedsDb = inMemoryDatabaseBuilder<SavedFeedsRoomDatabase>().build(),
        mutedWordsDb = inMemoryDatabaseBuilder<MutedWordsRoomDatabase>().build(),
        draftsDb = inMemoryDatabaseBuilder<DraftsRoomDatabase>().build(),
        postInteractionsOutboxDb = inMemoryDatabaseBuilder<OutboxRoomDatabase>().build(),
        baseContentSpecificDbUri = mockUserDataFolderPath(did).toString(),
        mediaFileSystem = fileSystem
    ),
    ContentRepositories(
        baseContentSpecificDbUri = mockUserCacheFolderPath(did).toString(),
        logger = Logger,
        apiResponseCacheDb = inMemoryDatabaseBuilder<ApiResponseRoomDatabase>().build(),
        mediaFileSystem = fileSystem
    ),
    mockUserDataFolderPath(did).toString(),
    mockUserCacheFolderPath(did).toString(),
    fileSystem
)


private fun TestScope.createMockAccountManager(
    userRepo: LoggedInUsersRepository,
    authManager: AuthenticationManager = MockAuthenticationManager(userRepo),
    bleanApi: BleanApi = MockBleanApi(),
    repositories: UserRepositories = createMockUserRepositories()
) = AccountManager(
    logger = Logger,
    scope = backgroundScope,
    bleanApi = bleanApi,
    rootUserRepository = userRepo,
    authenticationManager = authManager,
    userRepositoriesFactory = { repositories }
)

private fun createMockUser(
    did: String = "did:plc:test",
    handle: String = "test.handle.test"
) = LoggedInUserBasicDetails(
    did = AccountIdentificator.Did(Did.Plc(did)),
    handle = AccountIdentificator.Handle(handle),
    displayName = null,
    avatarUrl = null
)

private suspend fun LoggedInUsersRepository.setupMockUserWithCredentials(
    user: LoggedInUserBasicDetails,
    authServerUrl: String = "test.test",
    clientId: String = mockClientId
) {
    val oauthConfig = createMockOAuthConfig()

    val token = createMockOAuthToken(
        iss = oauthConfig.authServerMetadata.issuer,
        sub = user.did.value,
        clientId = oauthConfig.clientMetadata.clientId,
        aud = Did.Web("did:web:pds.example.com"),
        scope = oauthConfig.clientMetadata.scope
    )

    saveOrUpdateLoggedInUser(user)
    saveOrUpdateOAuthCredentials(
        user.did,
        accessToken = token,
        refreshToken = token,
        authorizationServerUrl = authServerUrl,
        clientId = clientId,
        keyPair = ECDSAP256InBase64Keys.generate()
    )
}

private data class MockOAuthConfig(
    val clientMetadata: ClientMetadata,
    val authServerMetadata: AuthorizationServerMetadata,
    val par: ResponseWithDPoPNonce<PushedAuthorizationRequest>
)

private fun createMockOAuthConfig() = MockOAuthConfig(
    clientMetadata = ClientMetadata(
        clientId = mockClientId,
        dpopBoundAccessTokens = true,
        grantTypes = listOf("authorization_code", "refresh_token"),
        redirectUris = listOf("https://mock.client.id/oauth2/callback"),
        scope = "atproto transition:generic transition:chat.bsky",
        responseTypes = listOf("code"),
        clientUri = "https://mock.client.id"
    ),
    authServerMetadata = AuthorizationServerMetadata(
        issuer = "https://fake.issuer",
        scopesSupported = arrayListOf(),
        subjectTypesSupported = arrayListOf(),
        responseTypesSupported = arrayListOf(),
        responseModesSupported = arrayListOf(),
        grantTypesSupported = arrayListOf(),
        codeChallengeMethodsSupported = arrayListOf(),
        uiLocalesSupported = arrayListOf(),
        displayValuesSupported = arrayListOf(),
        authorizationResponseIssParameterSupported = false,
        requestObjectSigningAlgValuesSupported = arrayListOf(),
        requestObjectEncryptionAlgValuesSupported = arrayListOf(),
        requestObjectEncryptionEncValuesSupported = arrayListOf(),
        requestParameterSupported = false,
        requestUriParameterSupported = false,
        requireRequestUriRegistration = false,
        jwksUri = "",
        authorizationEndpoint = "https://fake.auth.endpoint",
        tokenEndpoint = "https://fake.token.endpoint",
        tokenEndpointAuthMethodsSupported = arrayListOf(),
        tokenEndpointAuthSigningAlgValuesSupported = arrayListOf(),
        revocationEndpoint = "https://fake.token.endpoint",
        pushedAuthorizationRequestEndpoint = "",
        requirePushedAuthorizationRequests = false,
        dpopSigningAlgValuesSupported = arrayListOf(),
        clientIdMetadataDocumentSupported = false,
    ),
    par = ResponseWithDPoPNonce(
        dpopNonce = "fakeDpopNonce",
        PushedAuthorizationRequest("https://fake.webview.login.uri")
    )
)

fun MockCredentialsRepository(
    dao: PlatformCredentialsDao =
        inMemoryDatabaseBuilder<PlatformCredentialsDatabase>().build().dao()
) = RoomCredentialsRepository(dao, Logger)

fun MockLoggedInUsersRepository(
    userDao: LoggedInUserBasicDetailsDao =
        inMemoryDatabaseBuilder<LoggedInUsersRoomDatabase>().build().dao(),
    credentialRepo: RoomCredentialsRepository = MockCredentialsRepository()
) = LoggedInUsersRepository(userDao, credentialRepo)

fun MockOAuthAuthenticationAttemptRepository(
    dao: OAuthConfigSettingsDao =
        inMemoryDatabaseBuilder<OAuthConfigSettingsDatabase>().build().dao()
) = OAuthAuthenticationAttemptRepository(dao)

fun MockAuthenticationManager(
    rootUserRepo: LoggedInUsersRepository = MockLoggedInUsersRepository(),
    oauthAttemptRepo: OAuthAuthenticationAttemptRepository = MockOAuthAuthenticationAttemptRepository(),
    oauthClient: ATProtoOAuthClient = ATProtoOAuthClient(
        _MockOAuthApi(),
        _MockAccountApi(),
        mockClientId
    )
) = AuthenticationManager(
    rootUserRepository = rootUserRepo,
    oauthAttemptRepo,
    oauthClient,
    Logger
)

class MockBleanApi(
    override val account: AccountApi = _MockAccountApi(),
    override val feed: FeedApi = _MockFeedApi(),
    override val repo: RepoApi = _MockRepoApi(),
    override val session: SessionApi = _MockSessionApi()
) : BleanApi

fun createMockOAuthToken(
    aud: Did,
    sub: Did,
    clientId: String,
    scope: String,
    iss: String,
    iat: Long = Clock.System.now().toEpochMilliseconds(),
    exp: Long = (Clock.System.now().toEpochMilliseconds() / 1000) + 3600,
): String {
    val payload = DecodedOAuthToken(
        aud = aud,
        iat = iat,
        exp = exp,
        sub = sub,
        clientId = clientId,
        scope = scope,
        iss = iss
    )

    val payloadJson = Json.encodeToString(payload)
    val encodedPayload = Base64.encode(payloadJson.encodeToByteArray())

    val fakeHeader = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    val fakeSignature = "fake_signature_here"

    return "$fakeHeader.$encodedPayload.$fakeSignature"
}

const val mockClientId = "https://mock.client.id/client-metadata.json"