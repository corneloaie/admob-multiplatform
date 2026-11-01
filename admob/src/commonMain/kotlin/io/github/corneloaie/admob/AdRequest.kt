package io.github.corneloaie.admob

/**
 * Targeting for one ad request. The default asks for an ad with no targeting, as both apps do.
 *
 * @property keywords words that describe the app's current content.
 * @property contentUrl the URL of web content matching the app's current content.
 * @property neighboringContentUrls URLs of other content near the ad.
 */
public data class AdRequest(
    val keywords: Set<String> = emptySet(),
    val contentUrl: String? = null,
    val neighboringContentUrls: List<String> = emptyList(),
)
