package com.nxoim.blean.client

import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.client.stuff.BleanAccount
import com.nxoim.blean.client.stuff.BleanDrafts
import com.nxoim.blean.client.stuff.BleanFeed
import com.nxoim.blean.client.stuff.BleanPostInteractions
import com.nxoim.blean.client.stuff.BleanPosting
import kotlin.jvm.JvmInline

// todo to rename
// this was created when creating BleanClient interface so could
// value class Initialized(private val client: OAuthBleanClient): OAuthBleanClient by client,
// because for this OAuthBleanClient can only be an interface (Delegation is supported only for interfaces)

/**
 * Keep the client from recreating because this aggregates
 * dependencies into data, including state flows via .stateIn() (for user settings, for instance), and
 * we do not want any of that to be recreated not to cause any weird
 * issues in ui.
 */
interface OAuthBleanClient {
    val usersDid: AccountIdentificator.Did

    val account: BleanAccount
    val drafts: BleanDrafts
    val feed: BleanFeed

    val postInteractionsOutbox: BleanPostInteractions
    val posting: BleanPosting
}

/**
 * Client is intended to always be initialized when possible with the expectation
 * that there will be background and foreground users.
 */
sealed interface BleanClient {
    val usersDid: AccountIdentificator.Did

    @JvmInline
    value class LoggedIn(private val client: OAuthBleanClient): OAuthBleanClient by client, BleanClient {
        override val usersDid get() = client.usersDid
    }

    @JvmInline
    value class Loading(override val usersDid: AccountIdentificator.Did) : BleanClient

    /**
     * Soft logged out. Basic user details are present, but credentials are
     * either missing or invalid.
     */
    @JvmInline
    value class SoftLoggedOut(override val usersDid: AccountIdentificator.Did) : BleanClient
}
