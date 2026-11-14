package com.contest.travel.preview

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/** Figma 443:19285, rendered using the same frame, tokens and controls as the journey. */
@Composable
internal fun ColumnScope.FigmaHome(s: WireSession, enabled: Boolean) =
    FigmaJourneyExact(s, WireScreen.HOME, enabled)
