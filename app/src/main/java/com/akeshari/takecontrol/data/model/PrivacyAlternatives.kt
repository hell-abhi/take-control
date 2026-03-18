package com.akeshari.takecontrol.data.model

data class PrivacyAlternative(
    val category: String,
    val mainstream: String,
    val mainstreamIssue: String,
    val alternative: String,
    val whyBetter: String,
    val keyFeatures: List<String>
)

object PrivacyAlternativesData {
    val ALL = listOf(
        PrivacyAlternative(
            category = "Messaging",
            mainstream = "WhatsApp",
            mainstreamIssue = "Owned by Meta. Collects metadata (who you talk to, when, how often) and shares it with Facebook for ad targeting.",
            alternative = "Signal",
            whyBetter = "End-to-end encrypted by default. Open source. Collects virtually no metadata. Run by a non-profit foundation.",
            keyFeatures = listOf("No metadata collection", "Open source & audited", "Non-profit, no ads ever", "Disappearing messages built-in")
        ),
        PrivacyAlternative(
            category = "Email",
            mainstream = "Gmail",
            mainstreamIssue = "Google scans email content for ad targeting. Stores emails indefinitely. Tracks your purchases, travel, and subscriptions from email content.",
            alternative = "ProtonMail",
            whyBetter = "End-to-end encrypted email. Based in Switzerland (strong privacy laws). Cannot read your emails even if compelled by a court.",
            keyFeatures = listOf("End-to-end encryption", "Swiss privacy jurisdiction", "Zero-access architecture", "No tracking or profiling")
        ),
        PrivacyAlternative(
            category = "Browser",
            mainstream = "Chrome",
            mainstreamIssue = "Built by Google, the world's largest ad company. Tracks every site you visit. Syncs browsing history to your Google account.",
            alternative = "Brave",
            whyBetter = "Built-in ad and tracker blocker. Based on Chromium so all extensions work. Doesn't send browsing data to any company.",
            keyFeatures = listOf("Blocks ads & trackers by default", "No browsing history sent anywhere", "Built-in Tor for private tabs", "Chromium-based, all extensions work")
        ),
        PrivacyAlternative(
            category = "Search",
            mainstream = "Google Search",
            mainstreamIssue = "Records every search query tied to your identity. Builds a detailed interest profile. Uses search data for ad targeting across all Google products.",
            alternative = "DuckDuckGo",
            whyBetter = "Doesn't track searches. Doesn't store search history. Doesn't build a profile on you. Same results, no surveillance.",
            keyFeatures = listOf("Zero search history stored", "No user profiling", "No personalized ad targeting", "Anonymous search results")
        ),
        PrivacyAlternative(
            category = "Maps",
            mainstream = "Google Maps",
            mainstreamIssue = "Records your complete location history. Tracks every place you visit, how long you stay, and how you travel. Data used for ads.",
            alternative = "OsmAnd",
            whyBetter = "Fully offline maps. Open source. No tracking, no location history. Works without internet connection.",
            keyFeatures = listOf("Offline maps, no internet needed", "No location tracking", "Open source", "Turn-by-turn navigation offline")
        ),
        PrivacyAlternative(
            category = "Cloud Storage",
            mainstream = "Google Drive",
            mainstreamIssue = "Google can scan your files. Data stored on Google servers with Google's encryption keys. Subject to US data requests.",
            alternative = "Nextcloud",
            whyBetter = "Self-hosted or choose your provider. You control the encryption keys. Your files, your server, your rules.",
            keyFeatures = listOf("Self-hosted option", "End-to-end encryption", "Full data ownership", "No vendor lock-in")
        ),
        PrivacyAlternative(
            category = "Social Media",
            mainstream = "Instagram",
            mainstreamIssue = "Owned by Meta. Tracks activity across apps via SDK. Algorithmic feed designed for engagement, not your wellbeing. Extensive data collection.",
            alternative = "Mastodon (via Tusky)",
            whyBetter = "Decentralized — no single company controls it. Chronological feed. No ads, no tracking, no algorithm. You choose your community.",
            keyFeatures = listOf("Decentralized, no owner", "Chronological feed, no algorithm", "No ads or tracking", "Choose your own server/community")
        ),
        PrivacyAlternative(
            category = "Password Manager",
            mainstream = "Chrome Passwords",
            mainstreamIssue = "Passwords stored in your Google account. If your Google account is compromised, all passwords are exposed. Not independently audited.",
            alternative = "Bitwarden",
            whyBetter = "Open source, independently audited. End-to-end encrypted. Works across all devices and browsers. Free tier available.",
            keyFeatures = listOf("Open source & audited", "End-to-end encrypted vault", "Cross-platform", "Free tier with full features")
        ),
        PrivacyAlternative(
            category = "Keyboard",
            mainstream = "Gboard",
            mainstreamIssue = "Made by Google. Can send everything you type to Google servers for 'prediction improvement'. Has full internet access.",
            alternative = "HeliBoard",
            whyBetter = "Open source keyboard with no internet permission at all. Nothing you type can ever leave your device. Supports gestures and autocorrect.",
            keyFeatures = listOf("Zero internet access", "Nothing leaves your device", "Open source", "Gesture typing supported")
        ),
        PrivacyAlternative(
            category = "Video",
            mainstream = "YouTube",
            mainstreamIssue = "Owned by Google. Tracks every video you watch to build a detailed interest profile. Uses data for ad targeting. Algorithmic recommendations.",
            alternative = "NewPipe",
            whyBetter = "Open source YouTube frontend. No ads, no tracking, no Google account needed. Background play and downloads included.",
            keyFeatures = listOf("No ads whatsoever", "No Google account needed", "No watch history tracking", "Background play & downloads")
        ),
        PrivacyAlternative(
            category = "Notes",
            mainstream = "Google Keep",
            mainstreamIssue = "Stored on Google servers. Google can access your notes. Synced to your Google account with no end-to-end encryption.",
            alternative = "Standard Notes",
            whyBetter = "End-to-end encrypted notes. Open source. Your notes are unreadable to anyone but you, including the developers.",
            keyFeatures = listOf("End-to-end encrypted", "Open source", "Zero-knowledge architecture", "Cross-platform sync")
        ),
        PrivacyAlternative(
            category = "2FA / Authenticator",
            mainstream = "Google Authenticator",
            mainstreamIssue = "Recently added cloud sync which sends your 2FA seeds to Google servers. Seeds stored without end-to-end encryption.",
            alternative = "Aegis Authenticator",
            whyBetter = "Open source, offline-only. Your 2FA codes never leave your device. Encrypted local vault with biometric unlock.",
            keyFeatures = listOf("Completely offline", "Encrypted local vault", "Open source", "Biometric unlock")
        )
    )
}
