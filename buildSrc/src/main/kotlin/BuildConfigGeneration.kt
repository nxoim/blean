import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// 🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑
// NOTE This is only configured to function like a single, global
// scope for generation of build configs. Scope declaration is
// not implemented because theres no need for scopes (yet).
// (i could spend a million years perfecting this)
// 🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑🛑

val receiverModules = ConcurrentHashMap<String, Project>()

// needs to be inline otherwise it cant find KotlinMultiplatformExtension
// foer whatever reason
/**
 * Generates (both build time and compile time) a BuildConfig
 * object in build/generated/source/buildConfig/commonMain
 * and adds it to the source set
 */
inline fun Project.generateAppBuildConfig(
    isDebug: Boolean,
    verboseLogs: Boolean,
    versionString: String = baseAppVersionString,
    oauthEndpoint: String = getFromEnvironment(oauthEndpointEnvs),
    bskyServerEndpoint: String = getFromEnvironment(defaultBskyServerEndpointEnvs)
) = with(_BuildConfigGeneratorContext(this)) {
    val isAReceiverModule = receiverModules[this.path] != null
    val failHeader = if (isAReceiverModule) _BuildConfigGeneratorContext.failHeaderTag else ""

    val variantBuildConfigContents = """
        |$failHeader
        |// this file was generated automatically
        |object BuildConfig {
        |
        |   const val isDebug = $isDebug
        |   const val verboseLogs = $verboseLogs
        |
        |   object Info {
        |       const val version =  "$versionString"
        |   }
        |
        |   object Environment {
        |       const val defaultOauthEndpoint = "$oauthEndpoint"
        |       const val defaultBskyServerEndpoint = "$bskyServerEndpoint"
        |   }
        |
        |   object FeatureFlags {
        |
        |   }
        |
        |}
    """.trimMargin()

    // save in the variant's build cache (where source set is configured)
    writeBuildConfigToFile(this, variantBuildConfigContents, this.path)

    if (!isAReceiverModule) {
        val copyToReceiverModulesTask = createCopyToReceiverModuleTask(
            variantName = this.path,
            contents = variantBuildConfigContents
        )

        // if no receiver modules are specified then
        // we need to include the variants build config
        if (receiverModules.isEmpty()) includeBuildConfigInTheSourceSet()

        // bruteforce way but ok. at least we dont need to know
        // which task specifically is the first one
        tasks.forEach {
            if (it.name != copyToReceiverModulesTask.name) {
                it.dependsOn(copyToReceiverModulesTask)
            }
        }
    } else {
        // only set up the source set for receiver modules.
        // variant modules will depend on receiver modules and
        // therefore be able to access the config (eg :release
        // depending on :common). by the time compilation
        // happens - the active variant's config will be copied
        // over to receiver modules
        includeBuildConfigInTheSourceSet()
    }

    createGenerateBuildConfigOnBuildTask(variantBuildConfigContents)
}

/**
 * Generates (both build time and compile time) a BuildConfig
 * object in build/generated/source/buildConfig/commonMain
 * and adds it to the source set
 */
inline fun Project.generateDefaultBuildConfigAndSetModuleAsReceiver(
) = generateDefaultBuildConfigAndSetModuleAsReceiver(failCompilationIfNotOverridden = true)

/**
 * Generates (both build time and compile time) a BuildConfig
 * object in build/generated/source/buildConfig/commonMain
 * and adds it to the source set
 */
@Deprecated(
    "Please don't set failCompilationIfNotOverridden to false. Remove the line to remove the deprecation warning. Thank you",
)
inline fun Project.generateDefaultBuildConfigAndSetModuleAsReceiver(
    failCompilationIfNotOverridden: Boolean
) {
    receiverModules[this.path] = this

    if (failCompilationIfNotOverridden) {
        // its ok to create a new one
        _BuildConfigGeneratorContext(this)
            .failCompilationOnFailHeaderTag()
    }

    generateAppBuildConfig(
        isDebug = false,
        versionString = "invalid. report to the developer",
        verboseLogs = false
    )
}

@PublishedApi
internal class _BuildConfigGeneratorContext(private val project: Project) : Project by project {
    inline fun includeBuildConfigInTheSourceSet() = configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonMain") {
            kotlin.srcDir(getOrCreateBuildConfigDir())
        }
    }

    inline fun createGenerateBuildConfigOnBuildTask(
        contents: String
    ) {
        tasks.register(generateBuildConfigTaskName) {
            doFirst {
                logger.quiet("> Making sure build config in ${this@_BuildConfigGeneratorContext.path} is up to date")

                writeBuildConfigToFile(
                    project = this@_BuildConfigGeneratorContext,
                    configContents = contents,
                    variantNameForLogging = this@_BuildConfigGeneratorContext.path
                )
            }
        }
    }

    inline fun createCopyToReceiverModuleTask(
        variantName: String,
        contents: String
    ) = tasks.register("copyBuildConfigToReceivers") {
        dependsOn(generateBuildConfigTaskName)

        doFirst {
            logger.quiet("> Copying '$variantName' build config to ${receiverModules.size} receiver modules: ${receiverModules.values.joinToString { "\n    - " + it.path }}")

            receiverModules.forEach { receiverProjectMapEntry ->
                writeBuildConfigToFile(
                    project = receiverProjectMapEntry.value,
                    configContents = contents,
                    variantNameForLogging = variantName
                )
            }

            println("> BuildConfig copied successfully")
        }
    }

    inline fun failCompilationOnFailHeaderTag() = tasks.withType<AbstractKotlinCompile<*>>() {
        doFirst {
            val file = getOrCreateBuildConfigDir()
                .resolve("$buildConfigBaseName.kt")

            require(file.exists()) {
                """For some reason the receiver module ${this@_BuildConfigGeneratorContext.path}
                    |does not contain a build config. 
                """.trimMargin()
            }

            require(!file.readLines().firstOrNull().contentEquals(failHeaderTag)) {
                """The receiver module ${this@_BuildConfigGeneratorContext.path} did not receive a 
                    |variant of BuildConfig and was set up to fail compilation in such cases. 
                    |Please replace `generateDefaultBuildConfigAndSetModuleAsReceiver` with 
                    |`generateBuildConfig` or, if you truly need a default build config, set 
                    |`failCompilationIfNotOverridden` in`generateDefaultBuildConfigAndSetModuleAsReceiver` 
                    |to false. 
                    |
                    |Using `generateBuildConfig` is recommended""".trimMargin()
            }
        }
    }

    fun Project.getOrCreateBuildConfigDir() = layout.buildDirectory
        .dir(buildConfigGeneratedSourcePath)
        .get()
        .asFile
        .also { it.mkdirs() }

    inline fun writeBuildConfigToFile(
        project: Project,
        configContents: String,
        variantNameForLogging: String
    ) {
        try {
            File(
                /* parent = */ project.getOrCreateBuildConfigDir(),
                /* child = */ "$buildConfigBaseName.kt"
            )
                .writeText(configContents, Charsets.UTF_8)

        } catch (e: Exception) {
            throw GradleException("Could not write build config for '$variantNameForLogging' to source set of ${project.path}. \n ${e.stackTraceToString()}")
        }
    }

    companion object {
        const val buildConfigBaseName = "BuildConfig"
        const val buildConfigGeneratedSourcePath = "generated/source/buildConfig/commonMain"
        const val failHeaderTag = "// default config"
        const val generateBuildConfigTaskName = "generateBuildConfigOnCompile"
    }
}

@PublishedApi
internal val oauthEndpointEnvs = arrayOf(
    "defaultOauthEndpoint",
    "DEFAULT_OAUTH_ENDPOINT"
)

@PublishedApi
internal val defaultBskyServerEndpointEnvs = arrayOf(
    "defaultBskyServerEndpoint",
    "DEFAULT_BSKY_SERVER_ENDPOINT"
)


@PublishedApi
internal fun Project.getFromEnvironment(strings: Array<String>): String =
    getAnyCredentialRaw(*strings)
        ?.takeIf { it.isNotEmpty() }
        ?: error(
            "Unable to generate build config because none of the " +
                    "following were found in the environment or local " +
                    "properties: \n-${strings.joinToString("\n-")}"
        )