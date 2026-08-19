package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.domain.actions.model.ActionId
import com.carbroz.partner.domain.actions.model.ActionType
import com.carbroz.partner.domain.actions.spec.ActionSpec
import com.carbroz.partner.domain.actions.value.ActionParameters
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.storage.SessionCredentialStorage
import com.carbroz.partner.domain.session.store.SessionStore
import com.carbroz.partner.domain.storage.core.StorageResult
import com.carbroz.partner.domain.storage.secure.SecureStorageGateway
import com.carbroz.partner.engine.execution.result.ExecutionResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogoutActionExecutorTest {

    private class FakeSecureStorage : SecureStorageGateway {
        val map = mutableMapOf<String, String>()
        override suspend fun getSecret(key: String): StorageResult<String> {
            val v = map[key] ?: return StorageResult.NotFound
            return StorageResult.Success(v)
        }
        override suspend fun putSecret(key: String, value: String): StorageResult<Unit> {
            map[key] = value
            return StorageResult.Success(Unit)
        }
        override suspend fun removeSecret(key: String): StorageResult<Unit> {
            map.remove(key)
            return StorageResult.Success(Unit)
        }
    }

    @Test
    fun testLogoutExecutionClearsSessionAndMarksUnauthenticated() = runTest {
        val storage = FakeSecureStorage()
        val credStorage = SessionCredentialStorage(storage)
        val store = SessionStore()
        val clear = ClearSession(credStorage, store)

        credStorage.saveCredentials(SessionCredentials("tok_123"))
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
        assertTrue(credStorage.loadCredentials() is CredentialLoadResult.NotFound)
    }
}
