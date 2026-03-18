package com.akeshari.takecontrol.util

object TrackerCompanies {

    val PARENT_COMPANY: Map<String, String> = mapOf(
        // Google / Alphabet
        "Google Firebase Analytics" to "Google",
        "Google Analytics" to "Google",
        "Google AdMob" to "Google",
        "Google Crashlytics" to "Google",
        "Google Play Services" to "Google",

        // Meta
        "Facebook SDK" to "Meta",
        "Facebook Ads" to "Meta",
        "Facebook Login" to "Meta",
        "Facebook Share" to "Meta",

        // Unity
        "Unity Ads" to "Unity",

        // AppLovin
        "AppLovin" to "AppLovin",

        // IronSource (now Unity)
        "IronSource" to "Unity",

        // InMobi
        "InMobi" to "InMobi",

        // Others — each is its own company
        "Mixpanel" to "Mixpanel",
        "Amplitude" to "Amplitude",
        "Flurry" to "Yahoo",
        "Segment" to "Twilio",
        "CleverTap" to "CleverTap",
        "Countly" to "Countly",
        "Heap" to "Heap",
        "MoPub" to "AppLovin",
        "AdColony" to "Digital Turbine",
        "Vungle" to "Liftoff",
        "Chartboost" to "Chartboost",
        "Tapjoy" to "Tapjoy",
        "StartApp" to "StartApp",
        "AppsFlyer" to "AppsFlyer",
        "Adjust" to "Adjust",
        "Branch" to "Branch",
        "Kochava" to "Kochava",
        "Singular" to "Singular",
        "Leanplum" to "Leanplum",
        "Braze" to "Braze",
        "OneSignal" to "OneSignal",
        "Airship" to "Airship",
        "MoEngage" to "MoEngage",
        "WebEngage" to "WebEngage",
        "Bugsnag" to "SmartBear",
        "Sentry" to "Sentry",
        "Instabug" to "Instabug",
        "Huawei Mobile Services" to "Huawei"
    )

    fun getCompany(trackerName: String): String {
        return PARENT_COMPANY[trackerName] ?: trackerName
    }
}
