package com.akeshari.takecontrol.data.model

data class TrackerInfo(
    val name: String,
    val category: TrackerCategory
)

enum class TrackerCategory(val label: String) {
    ANALYTICS("Analytics"),
    ADVERTISING("Advertising"),
    SOCIAL("Social"),
    PROFILING("Profiling"),
    CRASH_REPORTING("Crash Reporting"),
    IDENTIFICATION("Identification")
}
