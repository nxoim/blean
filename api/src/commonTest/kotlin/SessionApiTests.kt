import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.SessionApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SessionApiTests {
    val sessionApi = SessionApi(createTestClient().value)
    val jwtAuthContext = runBlocking { sessionApi.getDefaultApiToken() }

    @Test
    fun testGetSession() = runTest {
        sessionApi.getSession(jwtAuthContext.value)
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get session. Response: $it") }
    }
}