package com.akeshari.takecontrol.util

import com.akeshari.takecontrol.data.model.TrackerCategory

object TrackerSignatures {

    data class Signature(
        val name: String,
        val category: TrackerCategory,
        val classPrefix: String
    )

    val ALL: List<Signature> = listOf(
        // Analytics
        Signature("Google Firebase Analytics", TrackerCategory.ANALYTICS, "com.google.firebase.analytics"),
        Signature("Google Analytics", TrackerCategory.ANALYTICS, "com.google.android.gms.analytics"),
        Signature("Mixpanel", TrackerCategory.ANALYTICS, "com.mixpanel"),
        Signature("Amplitude", TrackerCategory.ANALYTICS, "com.amplitude"),
        Signature("Flurry", TrackerCategory.ANALYTICS, "com.flurry"),
        Signature("Segment", TrackerCategory.ANALYTICS, "com.segment"),
        Signature("CleverTap", TrackerCategory.ANALYTICS, "com.clevertap"),
        Signature("Countly", TrackerCategory.ANALYTICS, "ly.count"),
        Signature("Heap", TrackerCategory.ANALYTICS, "com.heapanalytics"),

        // Advertising
        Signature("Google AdMob", TrackerCategory.ADVERTISING, "com.google.android.gms.ads"),
        Signature("Facebook Ads", TrackerCategory.ADVERTISING, "com.facebook.ads"),
        Signature("Unity Ads", TrackerCategory.ADVERTISING, "com.unity3d.ads"),
        Signature("AppLovin", TrackerCategory.ADVERTISING, "com.applovin"),
        Signature("IronSource", TrackerCategory.ADVERTISING, "com.ironsource"),
        Signature("InMobi", TrackerCategory.ADVERTISING, "com.inmobi"),
        Signature("MoPub", TrackerCategory.ADVERTISING, "com.mopub"),
        Signature("AdColony", TrackerCategory.ADVERTISING, "com.adcolony"),
        Signature("Vungle", TrackerCategory.ADVERTISING, "com.vungle"),
        Signature("Chartboost", TrackerCategory.ADVERTISING, "com.chartboost"),
        Signature("Tapjoy", TrackerCategory.ADVERTISING, "com.tapjoy"),
        Signature("StartApp", TrackerCategory.ADVERTISING, "com.startapp"),

        // Social / Attribution
        Signature("Facebook SDK", TrackerCategory.SOCIAL, "com.facebook"),
        Signature("Facebook Login", TrackerCategory.SOCIAL, "com.facebook.login"),
        Signature("Facebook Share", TrackerCategory.SOCIAL, "com.facebook.share"),

        // Profiling / Attribution
        Signature("AppsFlyer", TrackerCategory.PROFILING, "com.appsflyer"),
        Signature("Adjust", TrackerCategory.PROFILING, "com.adjust.sdk"),
        Signature("Branch", TrackerCategory.PROFILING, "io.branch"),
        Signature("Kochava", TrackerCategory.PROFILING, "com.kochava"),
        Signature("Singular", TrackerCategory.PROFILING, "com.singular"),
        Signature("Leanplum", TrackerCategory.PROFILING, "com.leanplum"),
        Signature("Braze", TrackerCategory.PROFILING, "com.braze"),
        Signature("OneSignal", TrackerCategory.PROFILING, "com.onesignal"),
        Signature("Airship", TrackerCategory.PROFILING, "com.urbanairship"),
        Signature("MoEngage", TrackerCategory.PROFILING, "com.moengage"),
        Signature("WebEngage", TrackerCategory.PROFILING, "com.webengage"),

        // Crash Reporting
        Signature("Google Crashlytics", TrackerCategory.CRASH_REPORTING, "com.google.firebase.crashlytics"),
        Signature("Bugsnag", TrackerCategory.CRASH_REPORTING, "com.bugsnag"),
        Signature("Sentry", TrackerCategory.CRASH_REPORTING, "io.sentry"),
        Signature("Instabug", TrackerCategory.CRASH_REPORTING, "com.instabug"),

        // Identification
        Signature("Google Play Services", TrackerCategory.IDENTIFICATION, "com.google.android.gms.common"),
        Signature("Huawei Mobile Services", TrackerCategory.IDENTIFICATION, "com.huawei.hms")
    )
}
