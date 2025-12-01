package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable

@Serializable
data class Preferences(val preferences: List<UserPreference>)