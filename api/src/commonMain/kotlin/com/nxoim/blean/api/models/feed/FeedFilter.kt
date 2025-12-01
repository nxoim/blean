package com.nxoim.blean.api.models.feed

enum class FeedFilter(val requestParameterBody: String) {
    PostsWithReplies("posts_with_replies"),
    PostsNoReplies("posts_no_replies"),
    PostsWithMedia("posts_with_media"),
    PostsAndAuthorThreads("posts_and_author_threads")
}