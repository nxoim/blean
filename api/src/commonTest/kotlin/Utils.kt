import com.nxoim.blean.api.models.modelsJsonConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.SYSTEM

val fileSystem = FileSystem.SYSTEM
val testFolder = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("auvau6t6a7v444")

inline fun <reified T> saveToJsonInBuildCache(
    name: String,
    value: T,
    serializer: KSerializer<T> = serializer()
) {
    fileSystem.run {
        val filePath = testFolder.resolve("$name.so")

        if (!exists(testFolder)) { createDirectories(testFolder) }

        write(filePath) { writeUtf8(Json.encodeToString(serializer, value)) }

        require(exists(filePath)) { "File was not saved" }

        println("file saved to $filePath")
    }
}

inline fun <reified T> readFromJsonInTempFolder(
    name: String,
    serializer: KSerializer<T> = serializer()
): T {
    var value: T? = null
    val filePath = testFolder.resolve("$name.so")

    fileSystem.run {
        require(exists(filePath)) { "File does not exist" }

        read(filePath) { value = Json.decodeFromString(serializer, readUtf8()) }
    }

    return value ?: error("Failed to read value from cache")
}

/**
 * Will throw if it cant serialize to strinng and deserialize back.
 * This can happen if the polymorphic structure does not store the
 * type/discriminator/whatever,
 */
inline fun <reified T : Any> checkSerializationValidity(value: T) {
    try {
        modelsJsonConfig.decodeFromString<T>(modelsJsonConfig.encodeToString<T>(value))
    } catch (e: SerializationException) {
        error("""Cannot verify serialization validity of ${T::class}. 
            |Check the polymorphic structures inside the model for '@SerialName annotations and add where missing. 
            |Make a custom polymorphic serializer if not possible. 
            |${e.stackTraceToString()}""".trimMargin())
    }
}