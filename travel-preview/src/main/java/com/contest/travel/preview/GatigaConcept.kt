package com.contest.travel.preview

internal data class TripTab(val key: String, val label: String, val symbol: String)

internal val tripTabs = listOf(
    TripTab("home", "여행 홈", "home"),
    TripTab("schedule", "일정", "calendar"),
    TripTab("crew", "일행", "people"),
    TripTab("talk", "대화", "chat"),
    TripTab("album", "추억", "photo")
)
