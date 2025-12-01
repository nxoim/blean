import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.SessionApi
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AccountApiTests {
    val sessionApi = SessionApi(createTestClient().value)
    val jwtAuthContext = runBlocking { sessionApi.getDefaultApiToken() }
    val accountApi = AccountApi(createTestClient().value)
//    val accountApi = MockAccountApi()

    @Test
    fun getProfile() = runTest {
        accountApi.getProfile(jwtAuthContext, AccountIdentificator.Handle("chetsucks.com"))
            .onSuccess {
                println("profile received $it")
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get profile. Error: $it") }
    }

    @Test
    fun getSuggestions() = runTest {
        accountApi.getSuggestions(jwtAuthContext)
            .onSuccess {
                println("suggestions received $it")
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get suggestions. Error: $it") }
    }

    @Test
    fun getPreferences() = runTest {
        accountApi.getPreferences(jwtAuthContext)
            .onSuccess {
                println("preferences received $it")
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get preferences. Error: $it") }
    }

    // no need to test it here because
    // its already part of creating the auth context
    // for tests
//    @Test
//    fun getDidDocumentFromPlcDirectory()

    // broken? https://github.com/bluesky-social/social-app/pull/2755
//    @Test
//    fun getProfiles() = runTest {
//        val response = accountApi.getProfiles(
//            AuthenticationMethod.AccessJwt(bskyapikey),
//            listOf("chetsucks.com", "darrylayo.bsky.social")
//        )
//
//        if (response !is GetProfiles.Response.Success) error("Could not get profiles. Error: $response")
//
//        println("profiles received $response")
//    }
}