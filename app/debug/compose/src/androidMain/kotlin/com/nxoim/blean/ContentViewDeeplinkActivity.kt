package com.nxoim.blean

import com.nxoim.blean.shared.InstanceCreationReason

/**
 * Content deeplink activity obv. Is single instance.
 */
class ContentViewDeeplinkActivity : BleanActivity(
    creationReason = InstanceCreationReason.Deeplink,
    observeNewDeeplinks = false,
)