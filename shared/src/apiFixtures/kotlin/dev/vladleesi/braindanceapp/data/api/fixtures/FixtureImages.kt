// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.fixtures

/** Solid-color 3x4 PNGs (73 bytes each) that stand in for covers and giveaway images; the UI scales them to fit. */
internal object FixtureImages {
    // Any IGDB size of a sample cover, as the app derives it from the t_thumb URL, and any sample giveaway image.
    private val imagePath =
        Regex("""^(?:igdb/image/upload/t_[a-z0-9_]+/cover|images/giveaway)-(\d+)\.(?:jpg|png|webp)$""")

    private val pngs: List<ByteArray> by lazy {
        listOf(
            "63507af31f8218f0b200e20b189d589ddbb7",
            "63f8afd701410c7859009f7b147da56d9b27",
            "63f8f38e0b8218f0b200e04f177104a95224",
            "63a80ffa0f410c785900ad3715c17fbe6045",
            "63b0bdd302410c7859008013135dc3bf095d",
            "63d0b38a842006bc2c00b314090d4967deda",
        ).map { idat -> (PNG_HEADER_HEX + idat + PNG_FOOTER_HEX).decodeHex() }
    }

    /** The image for a sample image [path] without its leading slash, or null when [path] is not one. */
    fun pngOrNull(path: String): ByteArray? {
        val number =
            imagePath
                .matchEntire(path)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull() ?: return null
        return pngs[number % pngs.size]
    }

    private fun String.decodeHex(): ByteArray = chunked(2).map { byte -> byte.toInt(radix = 16).toByte() }.toByteArray()

    // Signature, IHDR (3x4, 8-bit RGB), and the start of the IDAT chunk; colors differ only in IDAT data and checksum.
    private const val PNG_HEADER_HEX =
        "89504e470d0a1a0a0000000d4948445200000003000000040802000000c44f1250000000104944415478da"
    private const val PNG_FOOTER_HEX = "0000000049454e44ae426082"
}
