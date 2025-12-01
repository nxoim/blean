import org.gradle.api.Project
import java.util.Properties

var _localProps: Properties? = null

val Project.localProps: Properties
    get() = if (_localProps != null) {
        _localProps!!
    } else {
        Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }.also { _localProps = it }
    }

fun String?.trimAndNullIfEmpty(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun getenv(name: String): String? = System.getenv(name)?.trimAndNullIfEmpty()

fun Project.projectProp(name: String): String? =
    (findProperty(name) as? String).trimAndNullIfEmpty()