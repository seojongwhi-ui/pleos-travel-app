package com.contest.travel.preview

/** Read-only design snapshots, reached only through the existing debuggable-app guard. */
internal fun WireSession.applyDrivingDesignPreview(node: String?): Boolean {
    val target = when (node) {
        "443:21058", "443:21359", "443:21947", "443:22235", "443:23005",
        "443:24648", "443:24791", "443:25023" -> WireScreen.DRIVE
        "443:22556", "443:22700" -> WireScreen.MAP
        "443:22856" -> WireScreen.DJ
        "443:23212" -> WireScreen.KNOCK_SENT
        "443:23343", "443:24027" -> WireScreen.CALL
        "443:23484" -> WireScreen.VOICE_LEAVE
        "443:23612" -> WireScreen.VOICE_RECEIVED
        "443:23801" -> WireScreen.KNOCK_RECEIVED
        "443:24166" -> WireScreen.VOICE_APPROACH
        "443:24491" -> WireScreen.VOICE_DELIVER
        else -> return false
    }
    designPreviewNode = node
    drivingIdle = node != "443:21058"
    showVoiceMenu = node == "443:22235"
    speechFeedbackVisible = node == "443:21359"
    proposalVisible = node in setOf("443:23005", "443:24791", "443:25023")
    showHistory = node == "443:24648"
    showSettings = node == "443:24791"
    showEnd = node == "443:25023"
    waypoint = node == "443:22700"
    seconds = if (target == WireScreen.CALL) 24 else 10
    knockTarget = if (target in setOf(WireScreen.KNOCK_SENT, WireScreen.VOICE_LEAVE)) "문준 차" else "다연 차"
    incomingCaller = "다연 차"
    settingsToggles = listOf(true, true, true, true, false)
    stack.add(WireScreen.DRIVE)
    if (target != WireScreen.DRIVE) stack.add(target)
    return true
}
