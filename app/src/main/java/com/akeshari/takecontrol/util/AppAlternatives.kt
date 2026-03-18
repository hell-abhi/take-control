package com.akeshari.takecontrol.util

object AppAlternatives {

    data class Alternative(
        val packageName: String,
        val name: String,
        val reason: String
    )

    private val ALTERNATIVES: Map<String, List<Alternative>> = mapOf(
        // Messaging
        "com.whatsapp" to listOf(
            Alternative("org.thoughtcrime.securesms", "Signal", "End-to-end encrypted, open source, minimal data collection")
        ),
        "com.facebook.orca" to listOf(
            Alternative("org.thoughtcrime.securesms", "Signal", "No tracking, no ads, fully encrypted")
        ),

        // Email
        "com.google.android.gm" to listOf(
            Alternative("com.protonmail.android", "ProtonMail", "End-to-end encrypted email, based in Switzerland"),
            Alternative("net.tutanota.tutanota", "Tuta", "Encrypted email with zero-knowledge architecture")
        ),

        // Browser
        "com.android.chrome" to listOf(
            Alternative("com.duckduckgo.mobile.android", "DuckDuckGo", "Blocks trackers, no search history stored"),
            Alternative("org.mozilla.firefox", "Firefox", "Open source, strong privacy protections"),
            Alternative("com.brave.browser", "Brave", "Built-in ad and tracker blocking")
        ),
        "com.UCMobile.intl" to listOf(
            Alternative("com.brave.browser", "Brave", "Privacy-first browser, blocks ads and trackers")
        ),

        // Maps
        "com.google.android.apps.maps" to listOf(
            Alternative("net.osmand", "OsmAnd", "Offline maps, open source, no tracking")
        ),

        // Keyboard
        "com.google.android.inputmethod.latin" to listOf(
            Alternative("org.futo.voiceinput", "FUTO Keyboard", "Privacy-respecting keyboard, no cloud sync"),
            Alternative("rkr.simplekeyboard.inputmethod", "Simple Keyboard", "Minimal keyboard, no internet access")
        ),

        // Cloud storage
        "com.google.android.apps.docs" to listOf(
            Alternative("com.nextcloud.client", "Nextcloud", "Self-hosted cloud storage, full data ownership")
        ),

        // Social
        "com.instagram.android" to listOf(
            Alternative("com.keylesspalace.tusky", "Tusky (Mastodon)", "Decentralized social network, no algorithm, no ads")
        ),
        "com.twitter.android" to listOf(
            Alternative("com.keylesspalace.tusky", "Tusky (Mastodon)", "Decentralized, chronological feed, no tracking")
        ),

        // Video
        "com.google.android.youtube" to listOf(
            Alternative("app.revanced.android.youtube", "NewPipe", "No ads, no tracking, background play")
        )
    )

    fun getAlternatives(packageName: String): List<Alternative> {
        return ALTERNATIVES[packageName] ?: emptyList()
    }
}
