package com.nxoim.blean.shared.appEnvironment

import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.filesDir
import okio.Path
import okio.Path.Companion.toPath

data class StoragePaths(
    val data: Path,
    val cache: Path,
)

/**
 * This only exists to make the code more readable
 */
object PathProvider {
    val globalAppDataPath = FileKit.filesDir.absolutePath().toPath()
    val globalCachePath = FileKit.cacheDir.absolutePath().toPath()

    fun userRelated(inside: Path, forUser: AccountIdentificator.Did): Path
        = inside.resolve(forUser.toString())
}