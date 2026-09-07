package com.carbroz.partner.composition

import com.carbroz.feature.dynamic.DynamicDestination
import com.carbroz.feature.dynamic.DynamicRestorePolicy
import com.carbroz.feature.dynamic.DynamicScreenInstruction
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.feature.dynamic.DynamicScreenRequest
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.navigation.RestoredDestination
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ApplicationNavigationPersistenceTest {
    @Test
    fun safeDynamicDestinationRoundTripsThroughProcessRestorationContract() {
        val persistence = ApplicationNavigationPersistence()
        val original = DynamicDestination(instruction())
        val persisted = persistence.persist(original) ?: error("destination must be persistable")
        val restored = assertIs<DynamicDestination>(persistence.restore(persisted))

        assertEquals(original, restored)
        assertEquals(original.navigationId, restored.navigationId)
    }

    @Test
    fun cacheOnlyDestinationIsNeverPersistedBecauseRestorationCouldReplayMutation() {
        val persistence = ApplicationNavigationPersistence()
        val destination = DynamicDestination(
            instruction().copy(
                request = DynamicScreenRequest(RequestMethod.POST, "/api/v1/action"),
                restorePolicy = DynamicRestorePolicy.CACHE_ONLY,
            ),
        )
        assertNull(persistence.persist(destination))
    }

    @Test
    fun restorationFailsClosedWhenIdentityDoesNotMatchPayload() {
        val persistence = ApplicationNavigationPersistence()
        val payload = DynamicScreenInstructionCodec().encode(instruction())
        assertNull(
            persistence.restore(
                RestoredDestination(
                    navigationId = "dynamic:tampered:screen-1:template-7",
                    payload = payload,
                ),
            ),
        )
    }

    @Test
    fun processStateCodecRoundTripsSemanticRestorationData() {
        val codec = NavigationProcessStateCodec()
        val expected = listOf(
            RestoredDestination(
                navigationId = "dynamic:instance-9:screen-1:template-7",
                payload = DynamicScreenInstructionCodec().encode(instruction()),
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
            val saved = encodedState(DynamicDestination(instruction()))
            NavigationProcessStateBridge.restore(saved)

            assertEquals(SplashDestination, store.state.value.current)
        }
    }

    @Test
    fun compatibleSavedRootIsRestoredOnlyAfterFreshBootstrap() {
        withNavigationStore { store ->
            val fresh = DynamicDestination(instruction())
            NavigationProcessStateBridge.restore(encodedState(fresh))

            NavigationProcessStateBridge.applyAfterBootstrap(fresh)

            assertEquals(fresh, store.state.value.current)
        }
    }

    @Test
    fun incompatibleSavedRootIsDiscardedInFavorOfFreshBootstrapRoot() {
        withNavigationStore { store ->
            val saved = DynamicDestination(instruction())
            val fresh = DynamicDestination(
                instruction().copy(
                    destination = ScreenDestination("screen-fresh", "template-fresh", NodeType("FORM_TEMPLATE")),
                    backStackKey = "fresh",
                ),
            )
            NavigationProcessStateBridge.restore(encodedState(saved))

            NavigationProcessStateBridge.applyAfterBootstrap(fresh)

            assertEquals(fresh, store.state.value.current)
            assertEquals(1, store.state.value.backStack.size)
        }
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

    private fun instruction() = DynamicScreenInstruction(
        destination = ScreenDestination("screen-1", "template-7", NodeType("FORM_TEMPLATE")),
        request = DynamicScreenRequest(RequestMethod.GET, "/api/v1/screen/next", JsonObject(emptyMap())),
        transition = ScreenTransition.PUSH,
        backStackKey = "instance-9",
        restorePolicy = DynamicRestorePolicy.CACHE_FIRST,
    )
}
