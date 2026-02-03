import com.github.michaelbull.result.annotation.UnsafeResultValueAccess
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.SessionApi
import com.nxoim.blean.api.createClient
import com.nxoim.blean.api.models.session.CreatedSession
import com.nxoim.blean.api.utils.AuthenticationContext

fun createTestClient() = createClient("https://bsky.social")

private var bruh: AuthenticationContext.AccessJwt? = null

@OptIn(UnsafeResultValueAccess::class)
suspend fun SessionApi.getDefaultApiToken(onCreatedSession: (CreatedSession) -> Unit = {}): AuthenticationContext.AccessJwt =
    bruh ?: run {
        bruh = createNewSessionWithLoginPassword(bskyloginSecret, bskypasswordSecret)
            .onFailure { error("failed to get access jwt for test session. \n$it") }
            .map { session ->
                onCreatedSession(session)

                AccountApi(createTestClient().value)
                    .getDidDocumentFromPlcDirectory(session.did)
                    .onFailure { error("Failed to get the did document for the test session therefore unable to get the pds uri for authenticated requests") }
                    .map {
                        println(it)
                        AuthenticationContext.AccessJwt(
                            session.accessJwt,
                            it.service.first().serviceEndpoint
                        )
                    }
                    .value
            }
            .value

        bruh!!
    }