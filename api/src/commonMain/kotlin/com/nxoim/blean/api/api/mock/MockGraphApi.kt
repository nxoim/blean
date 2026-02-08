package com.nxoim.blean.api.api.mock

import com.nxoim.blean.api.api.GraphApi
import com.nxoim.blean.api.models.graph.ListFromGraph
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.LimitUpToHundred
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.bskyPrimitives.AtUri

open class _MockGraphApi : GraphApi {
    override suspend fun getList(
        authenticationContext: AuthenticationContext,
        list: AtUri,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<ListFromGraph> {
        TODO("Not yet implemented")
    }
}