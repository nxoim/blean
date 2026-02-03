package com.nxoim.blean

import com.nxoim.blean.shared.InstanceCreationReason


/**
 * Receives deeplinks about authentication (oauth callbacks for instance). Is single instance.
 */
class MainActivity : BleanActivity(
    creationReason = InstanceCreationReason.RegularLaunchOrResume,
    observeNewDeeplinks = true
)