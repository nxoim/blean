@file:OptIn(ExperimentalTime::class)

import com.nxoim.blean.byteCount.ByteCount
import com.nxoim.blean.repos.media.ExpiryPolicy
import com.nxoim.blean.repos.media.FileManager
import com.nxoim.blean.repos.media.FileManagerConfig
import com.nxoim.blean.repos.media.OkioFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class FileManagerTest {

    private lateinit var fs: FakeFileSystem
    private lateinit var fileManager: FileManager

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        fs = FakeFileSystem()
        val base = "/test".toPath()
        fs.createDirectories(base)

        fileManager = FileManager(
            fileStore = OkioFileStore("/test/fm", fileSystem = fs),
            config = FileManagerConfig()
        )

        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSaveAndRetrieve() = runTest {
        fileManager.createFolderIfMissing()

        val data = "hello".encodeToByteArray()

        fileManager.save("k1", flowOf(data))

        val read = fileManager.getBytes("k1").toList().filterNotNull()
        assertEquals(1, read.size)
        assertContentEquals(data, read.first())
    }

    @Test
    fun testDelete() = runTest {
        fileManager.createFolderIfMissing()

        fileManager.save("del", flowOf("x".encodeToByteArray()))
        assertNotNull(fileManager.getUri("del"))

        fileManager.delete("del")
        assertNull(fileManager.getUri("del"))
    }

    @Test
    fun testRename() = runTest {
        fileManager.createFolderIfMissing()

        fileManager.save("old", flowOf("xx".encodeToByteArray()))
        assertNotNull(fileManager.getUri("old"))

        fileManager.rename("old", "new")
        assertNull(fileManager.getUri("old"))
        assertNotNull(fileManager.getUri("new"))
    }

    @Test
    fun testDeleteAll() = runTest {
        fileManager.createFolderIfMissing()

        fileManager.save("a", flowOf("a".encodeToByteArray()))
        fileManager.save("b", flowOf("b".encodeToByteArray()))

        fileManager.deleteAll()

        val keys = fileManager.getAllKeys().toList()
        assertTrue(keys.isEmpty())
    }

    @Test
    fun testExpiryByAge() = runTest {
        val testClock = MutableTestClock(Instant.parse("2025-01-01T00:00:00Z"))

        val fs = FakeFileSystem(clock = testClock)
        fs.createDirectories("/test".toPath())

        val cfg = FileManagerConfig(
            defaultExpiryPolicies = listOf(ExpiryPolicy.ByAge(1.seconds))
        )

        val okio = OkioFileStore("/test/fmAge", fileSystem = fs)
        val fm = FileManager(fileStore = okio, config = cfg)

        fm.createFolderIfMissing()

        fm.save("old", flowOf("123".encodeToByteArray()))

        testClock.nowValue += 2.seconds

        fm.deleteOutdatedFiles(testClock)

        assertNull(fm.getUri("old"))
    }

    @Test
    fun testExpiryByFileCount() = runTest {
        val cfg = FileManagerConfig(
            defaultExpiryPolicies = listOf(ExpiryPolicy.ByFileCount(2))
        )
        fileManager = FileManager(
            fileStore = OkioFileStore("/test/fmCount", fileSystem = fs), cfg
        )
        fileManager.createFolderIfMissing()

        fileManager.save("f1", flowOf("1".encodeToByteArray()))
        fileManager.save("f2", flowOf("2".encodeToByteArray()))
        fileManager.save("f3", flowOf("3".encodeToByteArray()))

        fileManager.deleteOutdatedFiles()

        val keys = fileManager.getAllKeys().toList()
        // Oldest = f1
        assertFalse("f1" in keys)
        assertTrue("f2" in keys)
        assertTrue("f3" in keys)
    }

    @Test
    fun testExpiryBySize() = runTest {
        val cfg = FileManagerConfig(
            defaultExpiryPolicies = listOf(
                ExpiryPolicy.BySize(ByteCount(5))
            )
        )
        fileManager = FileManager(
            fileStore = OkioFileStore("/test/fmSize", fileSystem = fs), cfg
        )
        fileManager.createFolderIfMissing()

        fileManager.save("a", flowOf("AAA".encodeToByteArray())) // 3 bytes
        fileManager.save("b", flowOf("BB".encodeToByteArray()))  // 2 bytes (total=5)
        fileManager.save("c", flowOf("CCC".encodeToByteArray())) // 3 bytes (total=8)

        fileManager.deleteOutdatedFiles()

        val keys = fileManager.getAllKeys().toList()
        // Oldest "a" must be removed
        assertFalse("a" in keys)
        assertTrue("b" in keys)
        assertTrue("c" in keys)
    }
}

class MutableTestClock(var nowValue: Instant) : Clock {
    override fun now(): Instant = nowValue
}