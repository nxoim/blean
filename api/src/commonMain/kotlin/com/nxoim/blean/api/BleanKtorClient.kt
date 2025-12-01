package com.nxoim.blean.api

import com.nxoim.blean.api.models.modelsJsonConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.job
import kotlin.jvm.JvmInline

fun createClient(
    baseServerUrl: String,
) = BleanKtorClient(
    HttpClient() {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL


            this.filter {
                println("🌐${it.method} ${it.url.buildString()}")
                print(it.headers.entries().joinToString(" ---- "))
                println("\n")

                true
            }
        }

        install(ContentNegotiation) { json(modelsJsonConfig) }

        install(UserAgent) {
            agent = "Blean bsky client (pre pre pre pre alpha:3)"
        }

        defaultRequest {
            url(baseServerUrl)
        }
    }
)

@JvmInline
value class BleanKtorClient(val value: HttpClient) {
    suspend fun close() {
        value.close()
        value.coroutineContext.job.join()
    }
}