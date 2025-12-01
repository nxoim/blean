import com.nxoim.blean.api.models.feed.PostView

fun blockedManuallyPost() = Pair(
    PostView.Blocked,
    """{
  "${'$'}type": "app.bsky.feed.defs#blockedPost",
  "uri": "at://did:plc:vtpyqvwce4x6gpa5dcizqecy/app.bsky.feed.post/3ljb5abmug32w",
  "blocked": true,
  "author": {
    "did": "did:plc:vtpyqvwce4x6gpa5dcizqecy",
    "viewer": {
      "blockedBy": false,
      "blocking": "at://did:plc:4fovfpeqomd67hgjhfdnsyrn/app.bsky.graph.block/3lk24i43hi22t"
    }
  }
}"""
)

fun blockedByModListPost() = Pair(
    PostView.Blocked,
    """{
  "${'$'}type": "app.bsky.feed.defs#blockedPost",
  "uri": "at://did:plc:vtpyqvwce4x6gpa5dcizqecy/app.bsky.feed.post/3ljb5abmug32w",
  "blocked": true,
  "author": {
    "did": "did:plc:vtpyqvwce4x6gpa5dcizqecy",
    "viewer": {
      "blockedBy": false,
      "blocking": "at://did:plc:4fovfpeqomd67hgjhfdnsyrn/app.bsky.graph.list/3lk25pi2m6t2x"
    }
  }
}"""
)

fun mutedByModListPost() = Pair(
    PostView.Visible,
    """{
  "uri": "at://did:plc:vtpyqvwce4x6gpa5dcizqecy/app.bsky.feed.post/3ljb5abmug32w",
  "cid": "bafyreicgbc4oh5xorwm6rzxmuxcsvwsxlqrfxpqh5qaobcj6xuctucbs24",
  "author": {
    "did": "did:plc:vtpyqvwce4x6gpa5dcizqecy",
    "handle": "techcrunch.com",
    "displayName": "TechCrunch",
    "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:vtpyqvwce4x6gpa5dcizqecy/bafkreibox2utvy4ybplgp4bq7wgmzetiyvmgxvnshnk7q7qjhxt452l2um@jpeg",
    "viewer": {
      "muted": true,
      "mutedByList": {
        "uri": "at://did:plc:4fovfpeqomd67hgjhfdnsyrn/app.bsky.graph.list/3lk25pi2m6t2x",
        "cid": "bafyreidyisvyboyqszbc4k2flhnzjobhrc3ul6ephgsekzvx6kpqb5rp5u",
        "name": "test for dev purposes ignore pls",
        "purpose": "app.bsky.graph.defs#modlist",
        "listItemCount": 1,
        "indexedAt": "2025-03-10T18:12:39.356Z",
        "labels": [],
        "viewer": {
          "muted": true
        }
      },
      "blockedBy": false
    },
    "labels": [],
    "createdAt": "2023-04-26T17:53:51.327Z"
  },
  "record": {
    "${'$'}type": "app.bsky.feed.post",
    "createdAt": "2025-02-28T19:27:35Z",
    "embed": {
      "${'$'}type": "app.bsky.embed.external",
      "external": {
        "description": "Instagram alternative Flashes publicly launched its Bluesky-based photo-sharing app on the App Store this week, gaining nearly 30,000 downloads in its first 24 hours. The app offers a classic Instagram-like experience, allowing users to upload up to four…",
        "thumb": {
          "${'$'}type": "blob",
          "ref": {
            "${'$'}link": "bafkreie5owopq527ia4duxh2fsndeyz3ydx4ggs4fwwnyq45s5r4vxayru"
          },
          "mimeType": "image/jpeg",
          "size": 46875
        },
        "title": "Bluesky-based Instagram alternative Flashes launches publicly",
        "uri": "https://tcrn.ch/4blReFL"
      }
    },
    "text": "Bluesky-based Instagram alternative Flashes launches publicly"
  },
  "embed": {
    "${'$'}type": "app.bsky.embed.external#view",
    "external": {
      "uri": "https://tcrn.ch/4blReFL",
      "title": "Bluesky-based Instagram alternative Flashes launches publicly",
      "description": "Instagram alternative Flashes publicly launched its Bluesky-based photo-sharing app on the App Store this week, gaining nearly 30,000 downloads in its first 24 hours. The app offers a classic Instagram-like experience, allowing users to upload up to four…",
      "thumb": "https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:vtpyqvwce4x6gpa5dcizqecy/bafkreie5owopq527ia4duxh2fsndeyz3ydx4ggs4fwwnyq45s5r4vxayru@jpeg"
    }
  },
  "replyCount": 23,
  "repostCount": 180,
  "likeCount": 609,
  "quoteCount": 114,
  "indexedAt": "2025-02-28T19:27:35.649Z",
  "viewer": {
    "threadMuted": false,
    "embeddingDisabled": false
  },
  "labels": []
}"""
)

//         feedApi.getPostThread(
//            AuthenticationContext.AccessJwt(bskyapikey),
//            AtUri("at://did:plc:vtpyqvwce4x6gpa5dcizqecy/app.bsky.feed.post/3ljb5abmug32w")
//        )
//            .onSuccess {
//                println(it)
//                checkSerializationValidity(it)
//            }
//            .onFailure { error("Could not get thread. Response: $it") }