package com.nxoim.blean.api

import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.api.RepoApi
import com.nxoim.blean.api.api.SessionApi

class BleanApi(
    private val client: BleanKtorClient
) {
    val session = SessionApi(client.value)
    val account = AccountApi(client.value)
    val feed = FeedApi(client.value)
    val repo = RepoApi(client.value)
}