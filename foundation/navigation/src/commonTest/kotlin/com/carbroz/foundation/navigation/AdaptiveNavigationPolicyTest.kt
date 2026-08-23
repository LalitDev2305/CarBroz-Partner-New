package com.carbroz.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveNavigationPolicyTest {
    @Test
    fun compactCapacityAlwaysUsesSinglePane() {
        NavigationPresentationPreference.entries.forEach { preference ->
            assertEquals(
                NavigationPresentationMode.SinglePane,
                AdaptiveNavigationPolicy.resolve(
                    NavigationPresentationCapacity.SinglePane,
                    preference,
                ),
            )
        }
    }

    @Test
    fun listDetailCapacityUsesAtMostTwoPanePresentation() {
        assertEquals(
            NavigationPresentationMode.SinglePane,
            AdaptiveNavigationPolicy.resolve(
                NavigationPresentationCapacity.ListDetail,
                NavigationPresentationPreference.Single,
            ),
        )
        assertEquals(
            NavigationPresentationMode.ListDetail,
            AdaptiveNavigationPolicy.resolve(
                NavigationPresentationCapacity.ListDetail,
                NavigationPresentationPreference.ListDetail,
            ),
        )
        assertEquals(
            NavigationPresentationMode.ListDetail,
            AdaptiveNavigationPolicy.resolve(
                NavigationPresentationCapacity.ListDetail,
                NavigationPresentationPreference.Supporting,
            ),
        )
    }

    @Test
    fun supportingCapacityHonorsDestinationPreference() {
        assertEquals(
            NavigationPresentationMode.SinglePane,
            AdaptiveNavigationPolicy.resolve(
                NavigationPresentationCapacity.SupportingPane,
                NavigationPresentationPreference.Single,
            ),
        )
        assertEquals(
            NavigationPresentationMode.ListDetail,
            AdaptiveNavigationPolicy.resolve(
                NavigationPresentationCapacity.SupportingPane,
                NavigationPresentationPreference.ListDetail,
            ),
        )
        assertEquals(
            NavigationPresentationMode.SupportingPane,
            AdaptiveNavigationPolicy.resolve(
                NavigationPresentationCapacity.SupportingPane,
                NavigationPresentationPreference.Supporting,
            ),
        )
    }
}
