package com.nxoim.blean.shared

sealed interface InstanceCreationReason {
    // when app launchess or resumes from being dead
    data object RegularLaunchOrResume : InstanceCreationReason

    // deeplink stateflow is provided independently, as regular app
    // launch shpuld also be able to observe deeplinks
    data object Deeplink : InstanceCreationReason
}