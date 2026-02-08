package com.nxoim.blean.api

import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.api.RepoApi
import com.nxoim.blean.api.api.SessionApi

fun BleanApi(httpClient: BleanKtorClient): BleanApi = KtorBleanApi(httpClient)

interface BleanApi {
    val session: SessionApi
    val account: AccountApi
    val feed: FeedApi
    val repo: RepoApi
}

///////////////////////////////////////////////////////////////////////////////////////////

private class KtorBleanApi(
    private val client: BleanKtorClient
) : BleanApi {
    override val session = SessionApi(client.value)
    override val account = AccountApi(client.value)
    override val feed = FeedApi(client.value)
    override val repo = RepoApi(client.value)
}