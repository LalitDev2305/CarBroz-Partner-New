package com.carbroz.partner.engine.execution.dispatcher

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
import com.carbroz.partner.engine.execution.action.ActionId
import com.carbroz.partner.engine.execution.action.ActionParameters
import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.action.ActionValue
import com.carbroz.partner.engine.execution.binding.BindingExpression
import com.carbroz.partner.engine.execution.binding.BindingScope
import com.carbroz.partner.engine.execution.binding.DefaultBindingResolver
import com.carbroz.partner.engine.execution.executor.ActionExecutor
import com.carbroz.partner.engine.execution.executor.ActionRegistry
import com.carbroz.partner.engine.execution.result.ExecutionFailure
import com.carbroz.partner.engine.execution.result.ExecutionResult
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionDispatcherTest {

    private class TestLogger : StructuredLogger, BoundLogger {
        val loggedMessages = mutableListOf<String>()

        override fun withSource(sourceClass: String, defaultTraceContext: TraceContext?): BoundLogger = this
        override fun isLevelEnabled(level: LogLevel): Boolean = true

        override fun log(
            level: LogLevel,
            category: LogCategory,
            sourceFunction: String,
            event: String,
            message: String,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?,
            durationMs: Long?,
            throwable: Throwable?
        ) {
            loggedMessages.add(message)
        }

        override fun info(
            sourceFunction: String,
            category: LogCategory,
            event: String,
            message: String,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?
        ) {
            loggedMessages.add(message)
        }

        override fun debug(
            sourceFunction: String,
            category: LogCategory,
            event: String,
            message: String,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?
        ) {
            loggedMessages.add(message)
        }

        override fun error(
            sourceFunction: String,
            category: LogCategory,
            event: String,
            message: String,
            throwable: Throwable?,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?
        ) {
            loggedMessages.add(message)
        }
    }

    private class RecordingExecutor(
        override val supportedType: ActionType,
        val resultToReturn: ExecutionResult = ExecutionResult.Success()
    ) : ActionExecutor {
        var callCount = 0
        var lastReceivedAction: ActionSpec? = null

        override suspend fun execute(action: ActionSpec): ExecutionResult {
            callCount++
            lastReceivedAction = action
            return resultToReturn
        }
    }

    @Test
    fun verifySuccessfulDispatchWithoutBindings() = runBlocking {
        val type = ActionType("nav.push")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val spec = ActionSpec.create(ActionId("act_1"), type)
        val result = dispatcher.dispatch(spec)

        assertTrue(result is ExecutionResult.Success)
        assertEquals(1, executor.callCount)
        assertEquals(spec, executor.lastReceivedAction)
    }

    @Test
    fun verifyUnregisteredActionTypeFails() = runBlocking {
        val registry = ActionRegistry.EMPTY
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val spec = ActionSpec.create(ActionId("act_1"), ActionType("unknown"))
        val result = dispatcher.dispatch(spec)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals(ExecutionFailure.FailureCode.UNREGISTERED_ACTION_TYPE, result.failure.code)
        assertEquals(0, (registry.getExecutor(ActionType("unknown"))?.let { 1 } ?: 0))
    }

    @Test
    fun verifyTopLevelBindingResolutionSuccess() = runBlocking {
        val type = ActionType("user.update")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val params = ActionParameters.create(
            mapOf("name" to ActionValue.Binding(BindingExpression("\${user.name}")))
        )
        val spec = ActionSpec.create(ActionId("act_2"), type, params)
        val scope = BindingScope { path -> if (path == "user.name") ActionValue.Text("Alice") else null }

        val result = dispatcher.dispatch(spec, scope)

        assertTrue(result is ExecutionResult.Success)
        assertEquals(1, executor.callCount)
        val receivedParam = executor.lastReceivedAction?.parameters?.get("name")
        assertEquals(ActionValue.Text("Alice"), receivedParam)
    }

    @Test
    fun verifyNestedObjectAndListBindingResolutionSuccess() = runBlocking {
        val type = ActionType("nested.action")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val nestedObj = ActionValue.Object.create(
            mapOf("phone" to ActionValue.Binding(BindingExpression("\${user.phone}")))
        )
        val nestedList = ActionValue.List.create(
            listOf(ActionValue.Binding(BindingExpression("\${item.id}")))
        )
        val params = ActionParameters.create(
            mapOf("user" to nestedObj, "items" to nestedList)
        )
        val spec = ActionSpec.create(ActionId("act_3"), type, params)
        val scope = BindingScope { path ->
            when (path) {
                "user.phone" -> ActionValue.Text("+12345")
                "item.id" -> ActionValue.Integer(101L)
                else -> null
            }
        }

        val result = dispatcher.dispatch(spec, scope)

        assertTrue(result is ExecutionResult.Success)
        assertEquals(1, executor.callCount)

        val resUserObj = executor.lastReceivedAction?.parameters?.get("user") as ActionValue.Object
        assertEquals(ActionValue.Text("+12345"), resUserObj.properties["phone"])

        val resItemsList = executor.lastReceivedAction?.parameters?.get("items") as ActionValue.List
        assertEquals(ActionValue.Integer(101L), resItemsList.items[0])
    }

    @Test
    fun verifyDeeplyNestedObjectListObjectBindingSuccess() = runBlocking {
        val type = ActionType("deep.nested")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val innerObject = ActionValue.Object.create(
            mapOf("id" to ActionValue.Binding(BindingExpression("\${service.id}")))
        )
        val listValue = ActionValue.List.create(listOf(innerObject))
        val rootObject = ActionValue.Object.create(mapOf("services" to listValue))
        val params = ActionParameters.create(mapOf("booking" to rootObject))
        val spec = ActionSpec.create(ActionId("act_deep"), type, params)

        val scope = BindingScope { path -> if (path == "service.id") ActionValue.Text("SRV_77") else null }

        val result = dispatcher.dispatch(spec, scope)

        assertTrue(result is ExecutionResult.Success)
        assertEquals(1, executor.callCount)

        val resBooking = executor.lastReceivedAction?.parameters?.get("booking") as ActionValue.Object
        val resServices = resBooking.properties["services"] as ActionValue.List
        val resInner = resServices.items[0] as ActionValue.Object
        assertEquals(ActionValue.Text("SRV_77"), resInner.properties["id"])
    }

    @Test
    fun verifyDeeplyNestedBindingFailureAbortsDispatch() = runBlocking {
        val type = ActionType("deep.failing")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val innerObject = ActionValue.Object.create(
            mapOf("id" to ActionValue.Binding(BindingExpression("\${missing.service.id}")))
        )
        val listValue = ActionValue.List.create(listOf(innerObject))
        val rootObject = ActionValue.Object.create(mapOf("services" to listValue))
        val params = ActionParameters.create(mapOf("booking" to rootObject))
        val spec = ActionSpec.create(ActionId("act_deep_fail"), type, params)

        val scope = BindingScope { null }

        val result = dispatcher.dispatch(spec, scope)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals(ExecutionFailure.FailureCode.BINDING_RESOLUTION_FAILED, result.failure.code)
        assertEquals(0, executor.callCount)
    }

    @Test
    fun verifyMultipleBindingsAtomicityPartialFailureAborts() = runBlocking {
        val type = ActionType("multi.binding")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val params = ActionParameters.create(
            mapOf(
                "validKey" to ActionValue.Binding(BindingExpression("\${valid.key}")),
                "invalidKey" to ActionValue.Binding(BindingExpression("\${invalid.key}"))
            )
        )
        val spec = ActionSpec.create(ActionId("act_multi"), type, params)
        val scope = BindingScope { path -> if (path == "valid.key") ActionValue.Text("OK") else null }

        val result = dispatcher.dispatch(spec, scope)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals(ExecutionFailure.FailureCode.BINDING_RESOLUTION_FAILED, result.failure.code)
        assertEquals(0, executor.callCount)
    }

    @Test
    fun verifyMissingBindingAbortsExecution() = runBlocking {
        val type = ActionType("network.post")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val params = ActionParameters.create(
            mapOf("token" to ActionValue.Binding(BindingExpression("\${auth.token}")))
        )
        val spec = ActionSpec.create(ActionId("act_4"), type, params)
        val scope = BindingScope { null }

        val result = dispatcher.dispatch(spec, scope)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals(ExecutionFailure.FailureCode.BINDING_RESOLUTION_FAILED, result.failure.code)
        assertEquals(0, executor.callCount)
    }

    @Test
    fun verifyBindingExistsWithNullScopeAbortsExecution() = runBlocking {
        val type = ActionType("network.post")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val params = ActionParameters.create(
            mapOf("token" to ActionValue.Binding(BindingExpression("\${auth.token}")))
        )
        val spec = ActionSpec.create(ActionId("act_5"), type, params)

        val result = dispatcher.dispatch(spec, null)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals(ExecutionFailure.FailureCode.BINDING_RESOLUTION_FAILED, result.failure.code)
        assertEquals(0, executor.callCount)
    }

    @Test
    fun verifyOriginalActionSpecRemainsUnchanged() = runBlocking {
        val type = ActionType("user.update")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val bindingVal = ActionValue.Binding(BindingExpression("\${user.name}"))
        val params = ActionParameters.create(mapOf("name" to bindingVal))
        val spec = ActionSpec.create(ActionId("act_6"), type, params)
        val scope = BindingScope { ActionValue.Text("Bob") }

        dispatcher.dispatch(spec, scope)

        assertEquals(bindingVal, spec.parameters["name"])
    }

    @Test
    fun verifyExecutorSuccessAndFailurePreserved() = runBlocking {
        val type1 = ActionType("exec.success")
        val type2 = ActionType("exec.fail")
        val successExec = RecordingExecutor(type1, ExecutionResult.Success(ActionValue.Text("out")))
        val failExec = RecordingExecutor(type2, ExecutionResult.Failure(ExecutionFailure(ExecutionFailure.FailureCode.UNKNOWN, "Domain err")))

        val registry = ActionRegistry.create(listOf(successExec, failExec))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val res1 = dispatcher.dispatch(ActionSpec.create(ActionId("1"), type1))
        assertTrue(res1 is ExecutionResult.Success)
        assertEquals(ActionValue.Text("out"), res1.output)

        val res2 = dispatcher.dispatch(ActionSpec.create(ActionId("2"), type2))
        assertTrue(res2 is ExecutionResult.Failure)
        assertEquals(ExecutionFailure.FailureCode.UNKNOWN, res2.failure.code)
        assertEquals("Domain err", res2.failure.message)
    }

    @Test
    fun verifyUnexpectedExecutorExceptionReturnsExecutorFailedWithoutLeakingSecret() = runBlocking {
        val type = ActionType("throwing.type")
        val sensitiveSecretMsg = "SECRET_DB_PASSWORD_123"
        val throwingExecutor = object : ActionExecutor {
            override val supportedType = type
            override suspend fun execute(action: ActionSpec): ExecutionResult {
                throw IllegalStateException("Internal executor crash with secret: $sensitiveSecretMsg")
            }
        }
        val registry = ActionRegistry.create(listOf(throwingExecutor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val spec = ActionSpec.create(ActionId("act_7"), type)
        val result = dispatcher.dispatch(spec)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals(ExecutionFailure.FailureCode.EXECUTOR_FAILED, result.failure.code)
        assertEquals("Action executor encountered an unexpected error", result.failure.message)
        assertFalse(result.failure.message.contains(sensitiveSecretMsg))
    }

    @Test
    fun verifyCancellationExceptionPropagates() {
        val type = ActionType("cancelling.type")
        val cancellingExecutor = object : ActionExecutor {
            override val supportedType = type
            override suspend fun execute(action: ActionSpec): ExecutionResult {
                throw CancellationException("Scope cancelled")
            }
        }
        val registry = ActionRegistry.create(listOf(cancellingExecutor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val spec = ActionSpec.create(ActionId("act_8"), type)

        assertFailsWith<CancellationException> {
            runBlocking {
                dispatcher.dispatch(spec)
            }
        }
    }

    @Test
    fun verifyRawParameterValuesNeverLogged() = runBlocking {
        val type = ActionType("sensitive.action")
        val executor = RecordingExecutor(type)
        val registry = ActionRegistry.create(listOf(executor))
        val logger = TestLogger()
        val dispatcher = DefaultActionDispatcher(registry, DefaultBindingResolver(), logger)

        val fakeOtp = "123456"
        val fakeToken = "fake-secret-token"
        val fakePassword = "fake-password"

        val params = ActionParameters.create(
            mapOf(
                "otp" to ActionValue.Text(fakeOtp),
                "token" to ActionValue.Text(fakeToken),
                "password" to ActionValue.Text(fakePassword)
            )
        )
        val fakeActionId = "act_sensitive_999"
        val spec = ActionSpec.create(ActionId(fakeActionId), type, params)

        dispatcher.dispatch(spec)

        for (msg in logger.loggedMessages) {
            assertFalse(msg.contains(fakeActionId))
            assertFalse(msg.contains(fakeOtp))
            assertFalse(msg.contains(fakeToken))
            assertFalse(msg.contains(fakePassword))
            assertFalse(msg.contains("otp"))
            assertFalse(msg.contains("token"))
            assertFalse(msg.contains("password"))
        }
    }
}
