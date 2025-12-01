import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BleanClientManagerTest {
//    val clientManager = NEWClientManager("build/test_cache/blean_test")

//    @Test
//    fun createAndRestoreSession() = runTest {
//        clientManager.createClientViaLoginPassword(bskylogin, bskypassword)
//        println("created client. ${clientManager.clients.value!!.keys.joinToString()}")
//        clientManager.removeAllClients()
//        println("removed all clients. ${clientManager.clients.value!!.keys.joinToString()}")
//        clientManager.restoreClients()
//        println("restored clients. ${clientManager.clients.value!!.keys.joinToString()}")
//    }

    @Test
    fun beginSessionCreation() = runTest {
        // todo bring back tests
//        clientManager.beginOauthAuthorization(
//            onStateChange = { println(it) }
//        )
    }
}