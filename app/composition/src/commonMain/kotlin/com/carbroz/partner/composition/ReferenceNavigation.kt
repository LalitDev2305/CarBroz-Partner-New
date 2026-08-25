package com.carbroz.partner.composition

import com.carbroz.foundation.navigation.NavigationDestination

/** Neutral application-owned destination used only by the Phase 15 SDUI reference slice. */
data object ReferenceDestination : NavigationDestination {
    override val navigationId: String = "reference"
}
