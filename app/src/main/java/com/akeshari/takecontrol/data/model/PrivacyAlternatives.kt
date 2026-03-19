package com.akeshari.takecontrol.data.model

data class PrivacyAlternative(
    val category: String,
    val mainstream: String,
    val mainstreamPackage: String,
    val mainstreamIssue: String,
    val alternative: String,
    val alternativePackage: String,
    val alternativeIconUrl: String = "",
    val whyBetter: String,
    val keyFeatures: List<String>
)

object PrivacyAlternativesData {
    /**
     * Returns alternatives for a specific installed package by matching against mainstreamPackage.
     */
    fun getAlternativesForPackage(packageName: String): List<PrivacyAlternative> {
        return ALL.filter { it.mainstreamPackage == packageName }
    }

    val ALL = listOf(
        PrivacyAlternative(
            category = "Messaging",
            mainstream = "WhatsApp", mainstreamPackage = "com.whatsapp",
            mainstreamIssue = "Owned by Meta. Collects metadata (who you talk to, when, how often) and shares it with Facebook for ad targeting.",
            alternative = "Signal", alternativePackage = "org.thoughtcrime.securesms", alternativeIconUrl = "https://play-lh.googleusercontent.com/FtGKSwVtpmMxKoJrJuI837DkYGRsqlMdiVPAd8bomLQZ3_Hc55XokY_dYdXKgGagiYs=w240",
            whyBetter = "End-to-end encrypted by default. Open source. Collects virtually no metadata. Run by a non-profit foundation.",
            keyFeatures = listOf("No metadata collection", "Open source & audited", "Non-profit, no ads ever", "Disappearing messages built-in")
        ),
        PrivacyAlternative(
            category = "Email",
            mainstream = "Gmail", mainstreamPackage = "com.google.android.gm",
            mainstreamIssue = "Google scans email content for ad targeting. Stores emails indefinitely. Tracks your purchases, travel, and subscriptions from email content.",
            alternative = "ProtonMail", alternativePackage = "ch.protonmail.android", alternativeIconUrl = "https://play-lh.googleusercontent.com/00bieZXhOjqyfYPslJSZFuMXEAGIgCopkxJcDC-soMF_xg6coHFDfGBImPy3qKSN4SJ27cDOcIcXhMeB2yam-i0=w240",
            whyBetter = "End-to-end encrypted email. Based in Switzerland (strong privacy laws). Cannot read your emails even if compelled by a court.",
            keyFeatures = listOf("End-to-end encryption", "Swiss privacy jurisdiction", "Zero-access architecture", "No tracking or profiling")
        ),
        PrivacyAlternative(
            category = "Browser",
            mainstream = "Chrome", mainstreamPackage = "com.android.chrome",
            mainstreamIssue = "Built by Google, the world's largest ad company. Tracks every site you visit. Syncs browsing history to your Google account.",
            alternative = "Brave", alternativePackage = "com.brave.browser", alternativeIconUrl = "https://play-lh.googleusercontent.com/UBrZnuXGb-QACU0uUdF5r_n5nx5oBbdfPyEbtsr0xXv00RN7rpaTVu3GjLeE90aHFmNlbaCmm8vNcmY0eE98U9E=w240",
            whyBetter = "Built-in ad and tracker blocker. Based on Chromium so all extensions work. Doesn't send browsing data to any company.",
            keyFeatures = listOf("Blocks ads & trackers by default", "No browsing history sent anywhere", "Built-in Tor for private tabs", "Chromium-based, all extensions work")
        ),
        PrivacyAlternative(
            category = "Search",
            mainstream = "Google Search", mainstreamPackage = "com.google.android.googlequicksearchbox",
            mainstreamIssue = "Records every search query tied to your identity. Builds a detailed interest profile. Uses search data for ad targeting across all Google products.",
            alternative = "DuckDuckGo", alternativePackage = "com.duckduckgo.mobile.android", alternativeIconUrl = "https://play-lh.googleusercontent.com/NW2ASwJ4qtxfThhVIpm4641sR4o-yGv80yqaJnOnpC4lEmdxEcNTFcF6-TlZYtmdaA=w240",
            whyBetter = "Doesn't track searches. Doesn't store search history. Doesn't build a profile on you. Same results, no surveillance.",
            keyFeatures = listOf("Zero search history stored", "No user profiling", "No personalized ad targeting", "Anonymous search results")
        ),
        PrivacyAlternative(
            category = "Maps",
            mainstream = "Google Maps", mainstreamPackage = "com.google.android.apps.maps",
            mainstreamIssue = "Records your complete location history. Tracks every place you visit, how long you stay, and how you travel. Data used for ads.",
            alternative = "OsmAnd", alternativePackage = "net.osmand", alternativeIconUrl = "https://play-lh.googleusercontent.com/FSmpbc9ugImmneiJac8ffLvHrkgZ19xZJCjJVgrlTMpFRUn1XB0dkpP90iVePSvC9vc=w240",
            whyBetter = "Fully offline maps. Open source. No tracking, no location history. Works without internet connection.",
            keyFeatures = listOf("Offline maps, no internet needed", "No location tracking", "Open source", "Turn-by-turn navigation offline")
        ),
        PrivacyAlternative(
            category = "Cloud Storage",
            mainstream = "Google Drive", mainstreamPackage = "com.google.android.apps.docs",
            mainstreamIssue = "Google can scan your files. Data stored on Google servers with Google's encryption keys. Subject to US data requests.",
            alternative = "Nextcloud", alternativePackage = "com.nextcloud.client", alternativeIconUrl = "https://play-lh.googleusercontent.com/ooYcbfnlgkfA71bbRylUMfNnqpcxVhVdhfjnBNpCHgTBo5XE75Vp7bFuiCsWnzOk5gM=w240",
            whyBetter = "Self-hosted or choose your provider. You control the encryption keys. Your files, your server, your rules.",
            keyFeatures = listOf("Self-hosted option", "End-to-end encryption", "Full data ownership", "No vendor lock-in")
        ),
        PrivacyAlternative(
            category = "Social Media",
            mainstream = "Instagram", mainstreamPackage = "com.instagram.android",
            mainstreamIssue = "Owned by Meta. Tracks activity across apps via SDK. Algorithmic feed designed for engagement, not your wellbeing. Extensive data collection.",
            alternative = "Mastodon (via Tusky)", alternativePackage = "com.keylesspalace.tusky", alternativeIconUrl = "https://play-lh.googleusercontent.com/KB74s1pYUJJoaqWpLpCCn_QgJrHqxBuJgb1cXArZaApMz9HPkCjowfZhdRdOQ7eSrug=w240",
            whyBetter = "Decentralized — no single company controls it. Chronological feed. No ads, no tracking, no algorithm. You choose your community.",
            keyFeatures = listOf("Decentralized, no owner", "Chronological feed, no algorithm", "No ads or tracking", "Choose your own server/community")
        ),
        PrivacyAlternative(
            category = "Password Manager",
            mainstream = "Chrome Passwords", mainstreamPackage = "com.android.chrome",
            mainstreamIssue = "Passwords stored in your Google account. If your Google account is compromised, all passwords are exposed. Not independently audited.",
            alternative = "Bitwarden", alternativePackage = "com.x8bit.bitwarden", alternativeIconUrl = "https://play-lh.googleusercontent.com/-jz18EgBYlmeHlnsq_iltq6uLnYFtXAVR_gi_d0qEj0pANQ1MtrJIstJoCQtImlWKwc=w240",
            whyBetter = "Open source, independently audited. End-to-end encrypted. Works across all devices and browsers. Free tier available.",
            keyFeatures = listOf("Open source & audited", "End-to-end encrypted vault", "Cross-platform", "Free tier with full features")
        ),
        PrivacyAlternative(
            category = "Keyboard",
            mainstream = "Gboard", mainstreamPackage = "com.google.android.inputmethod.latin",
            mainstreamIssue = "Made by Google. Can send everything you type to Google servers for 'prediction improvement'. Has full internet access.",
            alternative = "Simple Keyboard", alternativePackage = "com.simplemobiletools.keyboard", alternativeIconUrl = "https://play-lh.googleusercontent.com/siErq80gKOfcPXenEKM5oR04B7dkUawpLE4N2HoHsin7WU-aEBnkh0pw8rADMjMbk7KP=w240",
            whyBetter = "Lightweight keyboard with no internet permission. Nothing you type can ever leave your device. No tracking, no data collection.",
            keyFeatures = listOf("Zero internet access", "Nothing leaves your device", "Open source", "Lightweight and fast")
        ),
        PrivacyAlternative(
            category = "Browser (Alt)",
            mainstream = "Samsung Internet", mainstreamPackage = "com.sec.android.app.sbrowser",
            mainstreamIssue = "Samsung's default browser tracks browsing activity. Sends data to Samsung and third-party analytics. Pre-installed and hard to remove.",
            alternative = "Firefox", alternativePackage = "org.mozilla.firefox", alternativeIconUrl = "https://play-lh.googleusercontent.com/zqsuwFUBwKRcGOSBinKQCL3JgfvOW49vJphq0ZF32aDgfqmuDyl-fEpx4Lxm4pRr7A=w240",
            whyBetter = "Open source browser with Enhanced Tracking Protection built-in. Blocks third-party cookies and trackers by default. Run by Mozilla, a non-profit.",
            keyFeatures = listOf("Enhanced Tracking Protection", "Open source, non-profit", "Cross-platform sync with encryption", "Extensive add-on support")
        ),
        PrivacyAlternative(
            category = "Notes",
            mainstream = "Google Keep", mainstreamPackage = "com.google.android.keep",
            mainstreamIssue = "Stored on Google servers. Google can access your notes. Synced to your Google account with no end-to-end encryption.",
            alternative = "Standard Notes", alternativePackage = "com.standardnotes", alternativeIconUrl = "https://play-lh.googleusercontent.com/yE8spVlVS8DxeK0eDkgoMw2TgTckRojptYQLW8UUuXrg1X4wpld_G0SmNgJSDzBdlxvv=w240",
            whyBetter = "End-to-end encrypted notes. Open source. Your notes are unreadable to anyone but you, including the developers.",
            keyFeatures = listOf("End-to-end encrypted", "Open source", "Zero-knowledge architecture", "Cross-platform sync")
        ),
        PrivacyAlternative(
            category = "2FA / Authenticator",
            mainstream = "Google Authenticator", mainstreamPackage = "com.google.android.apps.authenticator2",
            mainstreamIssue = "Recently added cloud sync which sends your 2FA seeds to Google servers. Seeds stored without end-to-end encryption.",
            alternative = "Aegis Authenticator", alternativePackage = "com.beemdevelopment.aegis", alternativeIconUrl = "https://play-lh.googleusercontent.com/qDzsOmoRT9o6QTElCaRJElAtfYW-nnOadwIInb6bXSEKexB211SsEtSeZrF5xm_lKUY=w240",
            whyBetter = "Open source, offline-only. Your 2FA codes never leave your device. Encrypted local vault with biometric unlock.",
            keyFeatures = listOf("Completely offline", "Encrypted local vault", "Open source", "Biometric unlock")
        )
    )
}
