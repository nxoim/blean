import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.GraphApi
import com.nxoim.blean.api.api.SessionApi
import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GraphTests {
    val sessionApi = SessionApi(createTestClient().value)
    val jwtAuthContext = runBlocking { sessionApi.getDefaultApiToken() }
    val graphApi = GraphApi(createTestClient().value)

    // todo jsOn LiTeRAL
    @Test
    fun getList() = runTest {
        graphApi.getList(
            jwtAuthContext,
            AtUri("at://did:plc:bfjoqzne3tm5yxvpybdfahzo/app.bsky.graph.list/3jzmevbfqkj2u")
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get list. Response: $it") }
    }
}