package com.carbroz.partner.composition

import com.carbroz.feature.dynamic.DynamicDestination
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.navigation.RestoredDestination
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiRequestMethod
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ApplicationNavigationPersistenceTest {
    @Test
    fun dynamicDestinationRoundTripsThroughProcessRestorationContract() {
        val persistence = ApplicationNavigationPersistence()
        val original = destination()
        val persisted = persistence.persist(original) ?: error("destination must be persistable")
        val restored = assertIs<DynamicDestination>(persistence.restore(persisted))

        assertEquals(original, restored)
        assertEquals(original.navigationId, restored.navigationId)
    }

    @Test
    fun fullDestinationFieldsSurviveRoundTrip() {
        val codec = DynamicDestinationCodec()
        val original = destination(
            screenId = "partner_dashboard",
            templateId = "partner_dashboard_template",
            templateType = "default_template",
            endpoint = "/api/v1/partner/sdui/registry/partner_dashboard",
            authentication = SduiAuthentication.SESSION,
        )

        assertEquals(original, codec.decode(codec.encode(original)))
    }

    @Test
    fun restorationFailsClosedWhenIdentityDoesNotMatchPayload() {
        val persistence = ApplicationNavigationPersistence()
        val original = destination()
        val payload = DynamicDestinationCodec().encode(original)

        assertNull(
            persistence.restore(
                RestoredDestination(
                    navigationId = "dynamic:tampered:template-7:/api/v1/screen/next",
                    payload = payload,
                ),
            ),
        )
    }

    @Test
    fun processStateCodecRoundTripsSemanticRestorationData() {
        val codec = NavigationProcessStateCodec()
        val original = destination()
        val expected = listOf(
            RestoredDestination(
                navigationId = original.navigationId,
                payload = DynamicDestinationCodec().encode(original),
            ),
        )

        assertEquals(expected, codec.decode(codec.encode(expected)))
    }

    @Test
    fun malformedProcessStateFailsClosedBeforeDestinationRestoration() {
        assertNull(NavigationProcessStateCodec().decode("not-json"))
    }

    @Test
    fun capturedDynamicStateIsNotAppliedBeforeFreshBootstrap() {
        withNavigationStore { store ->
            NavigationProcessStateBridge.restore(encodedState(destination()))

            assertEquals(SplashDestination, store.state.value.current)
        }
    }

    @Test
    fun compatibleSavedRootIsRestoredOnlyAfterFreshBootstrap() {
        withNavigationStore { store ->
            val fresh = destination()
            NavigationProcessStateBridge.restore(encodedState(fresh))

            NavigationProcessStateBridge.applyAfterBootstrap(fresh)

            assertEquals(fresh, store.state.value.current)
        }
    }

    @Test
    fun compatibleSavedStackPreservesFullDynamicDestinations() {
        withNavigationStore { store ->
            val root = destination()
            val dashboard = destination(
                screenId = "partner_dashboard",
                templateId = "partner_dashboard_template",
                templateType = "default_template",
                endpoint = "/api/v1/partner/sdui/registry/partner_dashboard",
                authentication = SduiAuthentication.SESSION,
            )
            val persistence = ApplicationNavigationPersistence()
            val encoded = NavigationProcessStateCodec().encode(
                listOf(
                    persistence.persist(root) ?: error("root must persist"),
                    persistence.persist(dashboard) ?: error("dashboard must persist"),
                ),
            )
            NavigationProcessStateBridge.restore(encoded)

            NavigationProcessStateBridge.applyAfterBootstrap(root)

            assertEquals(listOf(root, dashboard), store.state.value.backStack)
        }
    }

    @Test
    fun incompatibleSavedRootIsDiscardedInFavorOfFreshBootstrapRoot() {
        withNavigationStore { store ->
            val saved = destination()
            val fresh = destination(
                screenId = "screen-fresh",
                templateId = "template-fresh",
                templateType = "form_template",
                endpoint = "/api/v1/screen/fresh",
            )
            NavigationProcessStateBridge.restore(encodedState(saved))

            NavigationProcessStateBridge.applyAfterBootstrap(fresh)

            assertEquals(fresh, store.state.value.current)
            assertEquals(1, store.state.value.backStack.size)
        }
    }

    @Test
    fun malformedDynamicPayloadFailsClosed() {
        assertNull(
            ApplicationNavigationPersistence().restore(
                RestoredDestination(
                    navigationId = destination().navigationId,
                    payload = "not-json",
                ),
            ),
        )
    }

    private fun encodedState(destination: DynamicDestination): String {
        val persisted = ApplicationNavigationPersistence().persist(destination) ?: error("must persist")
        return NavigationProcessStateCodec().encode(listOf(persisted))
    }

    private fun withNavigationStore(block: (NavigationStore) -> Unit) {
        runCatching { stopKoin() }
        val store = NavigationStore(NavigationState(listOf(SplashDestination)))
        startKoin { modules(module { single { store } }) }
        try {
            block(store)
        } finally {
            NavigationProcessStateBridge.restore(null)
            stopKoin()
        }
    }

    private fun destination(
        screenId: String = "screen-1",
        templateId: String = "template-7",
        templateType: String = "form_template",
        endpoint: String = "/api/v1/screen/next",
        authentication: SduiAuthentication = SduiAuthentication.NONE,
    ) = DynamicDestination(
        screenId = screenId,
        templateId = templateId,
        templateType = templateType,
        endpoint = endpoint,
        method = SduiRequestMethod.GET,
        authentication = authentication,
    )
}
