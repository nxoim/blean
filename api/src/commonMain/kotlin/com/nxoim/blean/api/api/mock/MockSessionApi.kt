package com.nxoim.blean.api.api.mock

import com.nxoim.blean.api.api.SessionApi
import com.nxoim.blean.api.models.session.CreatedSession
import com.nxoim.blean.api.models.session.Session
import com.nxoim.blean.api.utils.RequestResult

open class _MockSessionApi : SessionApi {
    override suspend fun createNewSessionWithLoginPassword(
        identifier: String,
        password: String,
        authFactorToken: String?
    ): RequestResult<CreatedSession> {
        TODO("Not yet implemented")
    }

    override suspend fun getSession(accessJwt: String): RequestResult<Session> {
        TODO("Not yet implemented")
    }
}