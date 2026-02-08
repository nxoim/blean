package com.nxoim.blean.api.api.mock


import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.models.account.DidDocument
import com.nxoim.blean.api.models.account.Preferences
import com.nxoim.blean.api.models.account.Profile
import com.nxoim.blean.api.models.account.Profiles
import com.nxoim.blean.api.models.account.Suggestions
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.LimitUpToHundred
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did

open class _MockAccountApi : AccountApi {
    override suspend fun getProfile(
        authenticationContext: AuthenticationContext,
        targetHandleOrDid: AccountIdentificator
    ): RequestResult<Profile> {
        TODO("Not yet implemented")
    }

    override suspend fun getProfileUnauthorized(targetHandleOrDid: AccountIdentificator): RequestResult<Profile> {
        TODO("Not yet implemented")
    }

    override suspend fun getProfiles(
        authenticationContext: AuthenticationContext,
        targetHandlesOrDids: List<AccountIdentificator>
    ): RequestResult<Profiles> {
        TODO("Not yet implemented")
    }

    override suspend fun getSuggestions(
        authenticationContext: AuthenticationContext,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<Suggestions> {
        TODO("Not yet implemented")
    }

    override suspend fun getPreferences(authenticationContext: AuthenticationContext): RequestResult<Preferences> {
        TODO("Not yet implemented")
    }

    override suspend fun getDidDocumentFromPlcDirectory(did: Did): RequestResult<DidDocument> {
        TODO("Not yet implemented")
    }
}