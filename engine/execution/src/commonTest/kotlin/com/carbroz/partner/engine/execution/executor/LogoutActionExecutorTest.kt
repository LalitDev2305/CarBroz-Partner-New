package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.store.SessionStore
import com.carbroz.partner.engine.execution.action.ActionId
import com.carbroz.partner.engine.execution.action.ActionParameters
import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.result.ExecutionResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogoutActionExecutorTest {

    private class FakeCredentialPersistence : SessionCredentialPersistence {
        var stored: SessionCredentials? = null
        override suspend fun load(): CredentialLoadResult {
            val c = stored ?: return CredentialLoadResult.NotFound
            return CredentialLoadResult.Found(c)
        }
        override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult {
            stored = credentials
            return CredentialPersistenceResult.Success
        }
        override suspend fun clear(): CredentialPersistenceResult {
            stored = null
            return CredentialPersistenceResult.Success
        }
    }

    @Test
    fun testLogoutExecutionClearsSessionAndMarksUnauthenticated() = runTest {
        val persistence = FakeCredentialPersistence()
        val store = SessionStore()
        val clear = ClearSession(persistence, store)

        persistence.save(SessionCredentials("tok_123"))
        store.markAuthenticated()

        val executor = LogoutActionExecutor(clear)
        val actionSpec = ActionSpec.create(
            id = ActionId("act_logout"),
            type = ActionType.AUTH_LOGOUT,
            parameters = ActionParameters.EMPTY
        )

        val result = executor.execute(actionSpec)
        assertTrue(result is ExecutionResult.Success)
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertTrue(persistence.load() is CredentialLoadResult.NotFound)
    }
}
