package com.akeshari.takecontrol.util

object TrackerCountries {

    data class CountryInfo(
        val code: String,
        val name: String,
        val flag: String,
        val region: String
    )

    private val COMPANY_COUNTRY: Map<String, CountryInfo> = mapOf(
        // USA
        "Google" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Meta" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Branch" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Braze" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Mixpanel" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Amplitude" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Heap" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "OneSignal" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Airship" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Kochava" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Singular" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Leanplum" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Chartboost" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Tapjoy" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "AppLovin" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),

        // Israel
        "AppsFlyer" to CountryInfo("IL", "Israel", "\uD83C\uDDEE\uD83C\uDDF1", "Middle East"),
        "StartApp" to CountryInfo("IL", "Israel", "\uD83C\uDDEE\uD83C\uDDF1", "Middle East"),

        // Germany
        "Adjust" to CountryInfo("DE", "Germany", "\uD83C\uDDE9\uD83C\uDDEA", "Europe"),

        // USA (ad networks)
        "Unity" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Digital Turbine" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Liftoff" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),

        // India
        "CleverTap" to CountryInfo("IN", "India", "\uD83C\uDDEE\uD83C\uDDF3", "Asia"),
        "MoEngage" to CountryInfo("IN", "India", "\uD83C\uDDEE\uD83C\uDDF3", "Asia"),
        "WebEngage" to CountryInfo("IN", "India", "\uD83C\uDDEE\uD83C\uDDF3", "Asia"),
        "InMobi" to CountryInfo("IN", "India", "\uD83C\uDDEE\uD83C\uDDF3", "Asia"),

        // Ireland (EU HQ for US companies)
        "Yahoo" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Twilio" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),

        // Estonia
        "Countly" to CountryInfo("GB", "United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7", "Europe"),

        // USA (crash/monitoring)
        "SmartBear" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Sentry" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),
        "Instabug" to CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas"),

        // China
        "Huawei" to CountryInfo("CN", "China", "\uD83C\uDDE8\uD83C\uDDF3", "Asia"),

        // South Korea
        "Samsung" to CountryInfo("KR", "South Korea", "\uD83C\uDDF0\uD83C\uDDF7", "Asia")
    )

    fun getCountry(companyName: String): CountryInfo {
        return COMPANY_COUNTRY[companyName] ?: CountryInfo("US", "United States", "\uD83C\uDDFA\uD83C\uDDF8", "Americas")
    }
}
