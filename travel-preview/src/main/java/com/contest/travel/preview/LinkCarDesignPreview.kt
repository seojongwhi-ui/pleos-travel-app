package com.contest.travel.preview

/** Only called by the debug-guarded entry point in LinkCarDemoScreen. */
internal fun WireSession.applyDesignPreview(node: String?) {
    if (applyDrivingDesignPreview(node)) return
    val target = when (node) {
        "443:19285" -> WireScreen.HOME
        "443:19337", "443:20144" -> WireScreen.CREATE
        "443:19430" -> WireScreen.INVITE
        "443:19528" -> WireScreen.WAITING
        "443:19640" -> WireScreen.MISSION
        "443:19762" -> WireScreen.MUSIC_PICK
        "443:14563" -> WireScreen.WAITING_DECLINED
        "443:14624" -> WireScreen.INVITE_CONFIRM
        "443:14680" -> WireScreen.SHARE_SCOPE
        "443:14887" -> WireScreen.GUEST_WAITING
        else -> return
    }
    if (node == "443:20144") {
        createPurposeSelection = "친구 여행"
        createRecentSelection = "가평 용추계곡"
    }
    if (target == WireScreen.WAITING_DECLINED) declinedInvitees = setOf("다연")
    isGuest = target in setOf(WireScreen.INVITE_CONFIRM, WireScreen.SHARE_SCOPE, WireScreen.GUEST_WAITING)
    sharingConfirmed = target == WireScreen.GUEST_WAITING
    if (target != WireScreen.HOME) stack.add(target)
}
