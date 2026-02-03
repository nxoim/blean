import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.io.File
import java.util.Base64

/**
 * Configures Android signing for Android and Compose Multiplatform androidTarget.
 *
 * Reads keystore and credentials from:
 *   - KEYSTORE_PATH        → literal path to keystore
 *   - KEYSTORE_BASE64      → base64 of keystore (written to temp file)
 *   - KEYSTORE_FILE        → literal path (alias of PATH)
 *
 *   - KEYSTORE_PASSWORD
 *   - KEY_ALIAS
 *   - KEY_PASSWORD
 *
 * Works in local.properties, environment variables, and -P Gradle props.
 */
inline fun ApplicationExtension.setupAndroidAppSigning(
    project: Project,
    signingConfigName: String = "release"
): Boolean = with(project) {
    val keystore = resolveAndroidKeystoreFile()
    val storePassword = getAnyCredentialRaw(
        "KEYSTORE_PASSWORD",
        "KEYSTORE_PASSWORD_BASE64"
    )
    val keyAlias = getAnyCredentialRaw(
        "KEY_ALIAS",
        "KEY_ALIAS_BASE64"
    )
    val keyPassword = getAnyCredentialRaw(
        "KEY_PASSWORD",
        "KEY_PASSWORD_BASE64"
    )

    if (
        keystore == null ||
        storePassword == null ||
        keyAlias == null ||
        keyPassword == null
    ) {
        println("Android signing NOT configured: missing required credentials")
        return false
    }

    configureAndroidSigning(
        signingConfigName,
        keystore,
        storePassword,
        keyAlias,
        keyPassword
    )

    println("> Android signing configured with keystore")
    return true
}

@PublishedApi
internal inline fun ApplicationExtension.configureAndroidSigning(
    configName: String,
    keystoreFile: File,
    storePassword: String,
    keyAlias: String,
    keyPassword: String
) {
    signingConfigs.maybeCreate(configName).apply {
        storeFile = keystoreFile
        this.storePassword = storePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName(configName)
        }
    }
}

/**
 * Resolves a keystore file using the correct binary-safe logic.
 *
 * Valid orders:
 *   1. KEYSTORE_PATH        → path to existing file
 *   2. KEYSTORE_BASE64      → decode → write to build/tmp/keystore.jks
 *   3. KEYSTORE_FILE        → literal fallback path
 */
@Suppress("NewApi")
fun Project.resolveAndroidKeystoreFile(): File? {
    getAnyCredentialRaw("KEYSTORE_PATH")?.let { raw ->
        val file = file(raw)
        if (file.exists()) return file
        println("KEYSTORE_PATH found but file does not exist")
    }

    getAnyCredentialRaw("KEYSTORE_BASE64")?.let { raw ->
        val bytes = runCatching { Base64.getDecoder().decode(raw) }.getOrNull()
        if (bytes != null) {
            val out = rootProject.layout.buildDirectory
                .file("tmp/generated_android_keystore.jks")
                .get()
                .asFile

            out.parentFile.mkdirs()
            out.writeBytes(bytes)
            return out
        }
        println("KEYSTORE_BASE64 present but could not decode")
    }

    getAnyCredentialRaw("KEYSTORE_FILE")?.let { raw ->
        return file(raw)
    }

    println("No keystore credentials resolved.")
    return null
}

/**
 * Returns credentials WITHOUT interpreting them.
 * For binary objects (keystore paths, base64 blobs).
 */
fun Project.getAnyCredentialRaw(vararg names: String): String? {
    for (name in names) {
        val v = getenv(name)
            ?: localProps.getProperty(name)?.trimAndNullIfEmpty()
            ?: projectProp(name)

        if (!v.isNullOrBlank()) return v.trim()
    }
    return null
}