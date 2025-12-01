package com.nxoim.blean.models.account

data class MutedWord(
    val word: String,
    val id: String,
    val accountTarget: MuteAccountTarget,
    val contentTarget: MuteContentTarget,
    val expiresOnISO8601: String?
)

enum class MuteAccountTarget {
    All,
    ExcludeFollowedAccounts
}

enum class MuteContentTarget {
    Tag,
    Content,
    All
}