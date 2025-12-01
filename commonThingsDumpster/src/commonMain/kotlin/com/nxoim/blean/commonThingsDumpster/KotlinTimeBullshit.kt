@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.commonThingsDumpster

import com.nxoim.blean.bskyPrimitives.StringWrapperSerializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object InstantSerializer : StringWrapperSerializer<Instant>(
    "Instant",
    Instant::parse
)